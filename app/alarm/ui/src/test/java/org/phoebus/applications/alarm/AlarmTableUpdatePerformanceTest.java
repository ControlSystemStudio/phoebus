/*******************************************************************************
 * Copyright (c) 2026 Canadian Light Source Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.table.AlarmInfoRow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

/**
 * Regression test for the O(N^2 log N) performance bug in AlarmTableUI.update().
 *
 * <p>The ObservableList for alarm rows is created with the CHANGING_PROPERTIES
 * extractor.  A SortedList wraps it with a comparator.  Each call to
 * AlarmInfoRow.copy() fires 8 property-change events, and each event causes the
 * SortedList to perform a full O(N log N) sort.  For N rows this gives
 * O(8N * N log N) = O(N^2 log N) total comparator work.
 *
 * <p>The fix: temporarily set the SortedList comparator to null before the copy
 * loop so property-change events do not trigger re-sorts.  Restore the comparator
 * once after the loop, producing exactly one O(N log N) sort.
 *
 * <p>These tests use only javafx.base classes (ObservableList, SortedList,
 * SimpleStringProperty, ...) which work headlessly without starting the JavaFX
 * application toolkit.
 *
 * @see <a href="https://github.com/ControlSystemStudio/phoebus/issues/3504">Issue #3504</a>
 */
@SuppressWarnings("nls")
public class AlarmTableUpdatePerformanceTest
{
    private static final int N = 150;  // Large enough to expose O(N^2) but fast to run

    /** Build a list of N AlarmInfoRow items and a corresponding list of N "update" rows. */
    private static AlarmInfoRow makeRow(final int index, final SeverityLevel sev)
    {
        // AlarmClientLeaf(parent_path, name) works standalone – no tree needed.
        final AlarmClientLeaf leaf = new AlarmClientLeaf("/Test/Area", "pv" + String.format("%04d", index));
        leaf.setState(new ClientState(sev, "msg", String.valueOf(index),
                Instant.now(), sev, "msg"));
        return new AlarmInfoRow(leaf);
    }

    /**
     * Demonstrate the O(N^2 log N) problem: with the CHANGING_PROPERTIES extractor
     * active and a comparator present, each of the 8 * N property-change events fired
     * by the copy loop triggers a full re-sort of the SortedList.
     */
    @Test
    public void testOldBehaviorTriggersExcessiveSorts()
    {
        final ObservableList<AlarmInfoRow> rows =
                FXCollections.observableArrayList(AlarmInfoRow.CHANGING_PROPERTIES);

        final List<AlarmInfoRow> inputs = new ArrayList<>(N);
        for (int i = 0; i < N; i++)
        {
            rows.add(makeRow(i, SeverityLevel.MAJOR));          // "existing" row
            inputs.add(makeRow(N - 1 - i, SeverityLevel.MINOR)); // "update" row (reversed name)
        }

        final AtomicInteger comparisons = new AtomicInteger();
        final Comparator<AlarmInfoRow> countingComparator =
                (a, b) -> { comparisons.incrementAndGet(); return a.pv.get().compareTo(b.pv.get()); };

        final SortedList<AlarmInfoRow> sorted = new SortedList<>(rows, countingComparator);

        // Reset after the initial sort that SortedList performs on construction.
        comparisons.set(0);

        // OLD: copy with comparator active — each property change fires a re-sort.
        for (int i = 0; i < N; i++)
            rows.get(i).copy(inputs.get(i));

        final int oldComparisons = comparisons.get();

        // One sort of N elements costs at most N*log2(N) comparisons.
        final int singleSortUpperBound = N * 20; // generous upper bound for 1 sort
        System.out.println("Old comparisons: " + oldComparisons + "  (one-sort bound: " + singleSortUpperBound + ")");

        // The old code triggers far more comparisons than a single sort would.
        assertThat("Old code should trigger many more comparisons than a single sort",
                oldComparisons, greaterThan(singleSortUpperBound));
    }

    /**
     * Validate the fix: suppressing the comparator during the copy loop and
     * restoring it once reduces total comparator calls to a single O(N log N) sort.
     * The final row order must also be correct.
     */
    @Test
    public void testFixSuppressesIntermediateSortsAndPreservesOrder()
    {
        final ObservableList<AlarmInfoRow> rows =
                FXCollections.observableArrayList(AlarmInfoRow.CHANGING_PROPERTIES);

        final List<AlarmInfoRow> inputs = new ArrayList<>(N);
        for (int i = 0; i < N; i++)
        {
            rows.add(makeRow(i, SeverityLevel.MAJOR));
            inputs.add(makeRow(N - 1 - i, SeverityLevel.MINOR));
        }

        final AtomicInteger comparisons = new AtomicInteger();
        final Comparator<AlarmInfoRow> countingComparator =
                (a, b) -> { comparisons.incrementAndGet(); return a.pv.get().compareTo(b.pv.get()); };

        final SortedList<AlarmInfoRow> sorted = new SortedList<>(rows, countingComparator);
        comparisons.set(0); // reset after initial sort

        // FIX: suppress the comparator, copy all rows, restore comparator once.
        sorted.setComparator(null);
        for (int i = 0; i < N; i++)
            rows.get(i).copy(inputs.get(i));
        sorted.setComparator(countingComparator); // triggers exactly one O(N log N) sort

        final int fixedComparisons = comparisons.get();

        // Only one sort should have happened.
        final int singleSortUpperBound = N * 20;
        System.out.println("Fixed comparisons: " + fixedComparisons + "  (one-sort bound: " + singleSortUpperBound + ")");

        assertThat("Fix should produce at most one sort's worth of comparisons",
                fixedComparisons, lessThan(singleSortUpperBound));

        // Final order must be ascending by PV name.
        assertThat(sorted.size(), equalTo(N));
        for (int i = 0; i < N - 1; i++)
        {
            final String a = sorted.get(i).pv.get();
            final String b = sorted.get(i + 1).pv.get();
            assertThat("Sorted list must be in ascending PV name order at index " + i,
                    a.compareTo(b), lessThan(1));
        }
    }

    /**
     * End-to-end comparison: run both the old and fixed code paths on identical input
     * and verify that the fix uses at least 5x fewer comparator calls while producing
     * the same final sort order.
     */
    @Test
    public void testFixOutperformsOldCodeAndProducesSameOrder()
    {
        final List<AlarmInfoRow> inputs = new ArrayList<>(N);
        for (int i = 0; i < N; i++)
            inputs.add(makeRow(N - 1 - i, SeverityLevel.MINOR));

        // --- Old path ---------------------------------------------------
        final AtomicInteger oldCount = new AtomicInteger();
        final Comparator<AlarmInfoRow> oldCmp =
                (a, b) -> { oldCount.incrementAndGet(); return a.pv.get().compareTo(b.pv.get()); };
        final ObservableList<AlarmInfoRow> oldRows =
                FXCollections.observableArrayList(AlarmInfoRow.CHANGING_PROPERTIES);
        for (int i = 0; i < N; i++) oldRows.add(makeRow(i, SeverityLevel.MAJOR));
        final SortedList<AlarmInfoRow> oldSorted = new SortedList<>(oldRows, oldCmp);
        oldCount.set(0);
        for (int i = 0; i < N; i++) oldRows.get(i).copy(inputs.get(i));

        // --- Fixed path -------------------------------------------------
        final AtomicInteger newCount = new AtomicInteger();
        final Comparator<AlarmInfoRow> newCmp =
                (a, b) -> { newCount.incrementAndGet(); return a.pv.get().compareTo(b.pv.get()); };
        final ObservableList<AlarmInfoRow> newRows =
                FXCollections.observableArrayList(AlarmInfoRow.CHANGING_PROPERTIES);
        for (int i = 0; i < N; i++) newRows.add(makeRow(i, SeverityLevel.MAJOR));
        final SortedList<AlarmInfoRow> newSorted = new SortedList<>(newRows, newCmp);
        newCount.set(0);
        newSorted.setComparator(null);
        for (int i = 0; i < N; i++) newRows.get(i).copy(inputs.get(i));
        newSorted.setComparator(newCmp);

        System.out.println("Comparison counts — old: " + oldCount.get() + "  fixed: " + newCount.get()
                + "  ratio: " + (oldCount.get() / Math.max(1, newCount.get())) + "x");

        assertThat("Fix must use at least 5x fewer comparator calls",
                oldCount.get(), greaterThan(newCount.get() * 5));

        // Both paths must produce the same sorted order.
        assertThat(oldSorted.size(), equalTo(newSorted.size()));
        for (int i = 0; i < N; i++)
            assertThat("Row " + i + " must match between old and fixed paths",
                    oldSorted.get(i).pv.get(), equalTo(newSorted.get(i).pv.get()));
    }
}
