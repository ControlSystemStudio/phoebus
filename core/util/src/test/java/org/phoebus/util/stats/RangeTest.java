/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.stats;

import org.junit.Test;
import static org.junit.Assert.*;
import org.phoebus.util.stats.Range;
import org.phoebus.util.stats.Ranges;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author carcassi
 */
public class RangeTest {

    @Test
    public void range1() throws Exception {
        Range range = Range.create(0.0, 10.0);
        assertThat(range.getMinimum(), equalTo(0.0));
        assertThat(range.getMaximum(), equalTo(10.0));
        assertThat(range.isReversed(), equalTo(false));
        assertThat(range.toString(), equalTo("[0.0 - 10.0]"));
    }

    @Test
    public void range2() throws Exception {
        Range range = Range.create(0.0, 0.0);
        assertThat(range.getMinimum(), equalTo(0.0));
        assertThat(range.getMaximum(), equalTo(0.0));
        assertThat(range.isReversed(), equalTo(false));
        assertThat(range.toString(), equalTo("[0.0 - 0.0]"));
    }

    @Test
    public void range3() throws Exception {
        Range range = Range.create(10.0, 0.0);
        assertThat(range.getMinimum(), equalTo(0.0));
        assertThat(range.getMaximum(), equalTo(10.0));
        assertThat(range.isReversed(), equalTo(true));
        assertThat(range.toString(), equalTo("[10.0 - 0.0]"));
    }

    @Test
    public void range4() throws Exception {
        Range range = Range.create(0.0, Double.NaN);
        assertThat(range, sameInstance(Range.undefined()));
    }

    @Test
    public void equal1() throws Exception {
        assertThat(Range.create(0.0, 10.0), equalTo(Range.create(0.0, 10.0)));
        assertThat(Range.create(10.0, 0.0), not(equalTo(Range.create(0.0, 10.0))));
        assertThat(Range.create(10.0, 0.0), not(equalTo(Range.create(1.0, 10.0))));
        assertThat(Range.create(10.0, 0.0), not(equalTo(null)));
        assertThat(Range.undefined(), equalTo(Range.undefined()));
    }

    @Test
    public void isFinite1() {
        Range range1 = Ranges.range(0.0, 8.0);
        assertThat(range1.isFinite(), equalTo(true));
    }

    @Test
    public void isFinite2() {
        Range range1 = Ranges.range(5.0, 5.0);
        assertThat(range1.isFinite(), equalTo(false));
    }

    @Test
    public void isFinite3() {
        Range range1 = Ranges.range(Double.NaN, 8.0);
        assertThat(range1.isFinite(), equalTo(false));
    }

    @Test
    public void isFinite4() {
        Range range1 = Ranges.range(Double.NEGATIVE_INFINITY, 8.0);
        assertThat(range1.isFinite(), equalTo(false));
    }

    @Test
    public void isFinite5() {
        Range range1 = Ranges.range(0.0, Double.NaN);
        assertThat(range1.isFinite(), equalTo(false));
    }

    @Test
    public void isFinite6() {
        Range range1 = Ranges.range(0.0, Double.POSITIVE_INFINITY);
        assertThat(range1.isFinite(), equalTo(false));
    }

    @Test
    public void normalize1() {
        Range range = Ranges.range(-10.0, 10.0);
        assertThat(range.normalize(0.0), equalTo(0.5));
    }

    @Test
    public void normalize2() {
        Range range = Ranges.range(-10.0, 10.0);
        assertThat(range.normalize(10.0), equalTo(1.0));
    }

    @Test
    public void normalize3() {
        Range range = Ranges.range(-10.0, 10.0);
        assertThat(range.normalize(-10.0), equalTo(0.0));
    }

    @Test
    public void contains1() {
        Range range = Ranges.range(-10.0, 10.0);
        assertThat(range.contains(5.0), equalTo(true));
    }

    @Test
    public void contains2() {
        Range range = Ranges.range(-10.0, 10.0);
        assertThat(range.contains(7.5), equalTo(true));
    }

    @Test
    public void contains3() {
        Range range = Ranges.range(-10.0, 10.0);
        assertThat(range.contains(25.0), equalTo(false));
    }

    @Test
    public void contains4() {
        Range range = Ranges.range(-10.0, 10.0);
        assertThat(range.contains(-25.0), equalTo(false));
    }

    @Test
    public void containsRange1() {
        assertThat(Range.create(0.0, 1.0).contains(Range.create(0.5, 0.75)), equalTo(true));
        assertThat(Range.create(0.0, 1.0).contains(Range.create(0.5, 1.0)), equalTo(true));
        assertThat(Range.create(0.0, 1.0).contains(Range.create(0.0, 0.75)), equalTo(true));
        assertThat(Range.create(0.0, 1.0).contains(Range.create(-1.0, 0.75)), equalTo(false));
        assertThat(Range.create(0.0, 1.0).contains(Range.create(0.0, 1.75)), equalTo(false));
    }

    @Test
    public void combine1() {
        Range range1 = Range.create(0.0, 5.0);
        Range range2 = Range.create(1.0, 2.0);
        assertThat(range1.combine(range2), sameInstance(range1));
        assertThat(range2.combine(range1), sameInstance(range1));
    }

    @Test
    public void combine2() {
        Range range1 = Ranges.range(0.0, 5.0);
        Range range2 = Ranges.range(1.0, 6.0);
        Range range = range1.combine(range2);
        assertThat(range.getMinimum(), equalTo((Number) 0.0));
        assertThat(range.getMaximum(), equalTo((Number) 6.0));
        range = range2.combine(range1);
        assertThat(range.getMinimum(), equalTo((Number) 0.0));
        assertThat(range.getMaximum(), equalTo((Number) 6.0));
    }

    @Test
    public void combine3() {
        Range range1 = Ranges.range(0.0, 3.0);
        Range range2 = Ranges.range(4.0, 6.0);
        Range range = range1.combine(range2);
        assertThat(range.getMinimum(), equalTo((Number) 0.0));
        assertThat(range.getMaximum(), equalTo((Number) 6.0));
        range = range2.combine(range1);
        assertThat(range.getMinimum(), equalTo((Number) 0.0));
        assertThat(range.getMaximum(), equalTo((Number) 6.0));
    }

    @Test
    public void combine4() {
        Range range1 = Ranges.range(0.0, 3.0);
        Range range2 = Ranges.range(0.0, 6.0);
        Range range = range1.combine(range2);
        assertThat(range.getMinimum(), equalTo((Number) 0.0));
        assertThat(range.getMaximum(), equalTo((Number) 6.0));
        range = range2.combine(range1);
        assertThat(range.getMinimum(), equalTo((Number) 0.0));
        assertThat(range.getMaximum(), equalTo((Number) 6.0));
    }

    @Test
    public void combine5() {
        Range range1 = Ranges.range(0.0, 6.0);
        Range range2 = Ranges.range(3.0, 6.0);
        Range range = range1.combine(range2);
        assertThat(range.getMinimum(), equalTo((Number) 0.0));
        assertThat(range.getMaximum(), equalTo((Number) 6.0));
        range = range2.combine(range1);
        assertThat(range.getMinimum(), equalTo((Number) 0.0));
        assertThat(range.getMaximum(), equalTo((Number) 6.0));
    }
    @Test
    public void combine6() {
        Range range1 = Range.undefined();
        Range range2 = Range.create(1.0, 2.0);
        assertThat(range1.combine(range2), sameInstance(range2));
        assertThat(range2.combine(range1), sameInstance(range2));
    }

}
