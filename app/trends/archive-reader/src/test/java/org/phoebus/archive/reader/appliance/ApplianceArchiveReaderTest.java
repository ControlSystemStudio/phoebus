package org.phoebus.archive.reader.appliance;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadInfo;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Display;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplianceArchiveReaderTest {

    private static final Instant START = Instant.now().minusSeconds(3600);
    private static final Instant END = Instant.now();

    private static GenMsgIterator emptyStream() {
        GenMsgIterator s = mock(GenMsgIterator.class);
        when(s.iterator()).thenReturn(Collections.emptyIterator());
        return s;
    }

    private static GenMsgIterator probeStream(PayloadType type) {
        EpicsMessage msg = mock(EpicsMessage.class);
        GenMsgIterator s = mock(GenMsgIterator.class);
        when(s.iterator()).thenReturn(Collections.singletonList(msg).iterator());
        when(s.getPayLoadInfo()).thenReturn(PayloadInfo.newBuilder().setType(type).buildPartial());
        return s;
    }

    private static GenMsgIterator countStream(int count) {
        EpicsMessage msg = mock(EpicsMessage.class);
        when(msg.getNumberValue()).thenReturn(count);
        GenMsgIterator s = mock(GenMsgIterator.class);
        when(s.iterator()).thenReturn(Collections.singletonList(msg).iterator());
        return s;
    }

    /**
     * When the PV type is SCALAR_ENUM the optimized path throws, getOptimizedValues
     * falls back, and since binning is unsupported it reaches NonNumericOptimizedValueIterator
     * (or RawValueIterator if points <= count).
     */
    @Test
    void scalarEnumRoutesToNonNumericIterator() throws Exception {
        // probe returns SCALAR_ENUM → OptimizedValueIterator throws ArchiverApplianceInvalidTypeException
        // ncount returns 200 points > requested 100 → NonNumeric path chosen
        FakeDataRetrieval dr = new FakeDataRetrieval(probeStream(PayloadType.SCALAR_ENUM));
        dr.whenPvContains("ncount", countStream(200));
        dr.whenPvContains("nth_", emptyStream());

        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr, false, true);
        Object iter = reader.getOptimizedValues("TEST:PV", START, END, 100);

        assertInstanceOf(ApplianceNonNumericOptimizedValueIterator.class, iter);
    }

    @Test
    void numericScalarUsesOptimizedIterator() throws Exception {
        // probe returns SCALAR_DOUBLE → OptimizedValueIterator succeeds
        FakeDataRetrieval dr = new FakeDataRetrieval(probeStream(PayloadType.SCALAR_DOUBLE));
        dr.whenPvContains("optimized_", emptyStream());

        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr, false, true);
        Object iter = reader.getOptimizedValues("TEST:PV", START, END, 100);

        assertInstanceOf(ApplianceOptimizedValueIterator.class, iter);
    }

    @Test
    void cancelClosesAllActiveIterators() throws Exception {
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);

        ApplianceValueIterator iter = (ApplianceValueIterator) reader.getRawValues("TEST:PV", START, END);
        assertFalse(iter.closed);

        reader.cancel();
        assertTrue(iter.closed);
    }

    @Test
    void weakMapDoesNotPreventGC() throws Exception {
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);

        WeakReference<ApplianceValueIterator> ref;
        {
            ApplianceValueIterator iter = (ApplianceValueIterator) reader.getRawValues("TEST:PV", START, END);
            ref = new WeakReference<>(iter);
            // iter goes out of scope here; no other strong reference
        }

        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(10);
        }

        assertNull(ref.get(), "Iterator was not garbage collected");
        assertTrue(reader.iterators.isEmpty(), "WeakHashMap should be empty after GC");
    }

    @Test
    void getNumberOfPointsUsesNcountOperator() throws Exception {
        // ncount returns 0 ≤ requested 100 → falls back to RawValueIterator
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr, false, false);
        reader.getOptimizedValues("TEST:PV", START, END, 100);

        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("ncount(")),
                "Expected an ncount( call, got: " + dr.pvsCalled);
    }

    @Test
    void getRawValuesRegistersIteratorInMap() throws Exception {
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);
        ApplianceValueIterator iter = (ApplianceValueIterator) reader.getRawValues("TEST:PV", START, END);
        assertTrue(reader.iterators.containsKey(iter), "iterator should be registered in the map after getRawValues");
    }

    @Test
    void closeCallsCancel() throws Exception {
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);
        ApplianceValueIterator iter = (ApplianceValueIterator) reader.getRawValues("TEST:PV", START, END);
        assertFalse(iter.closed);
        reader.close();
        assertTrue(iter.closed, "close() should close all active iterators via cancel()");
    }

    @Test
    void pointsAtOrBelowCountUsesRawIterator() throws Exception {
        // ncount returns 50, requested count is 100 → points <= count → ApplianceRawValueIterator
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        dr.whenPvContains("ncount(", countStream(50));
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr, false, false);
        Object iter = reader.getOptimizedValues("TEST:PV", START, END, 100);
        assertInstanceOf(ApplianceRawValueIterator.class, iter);
    }

    @Test
    void numericScalarWithUseStatisticsTrueUsesStatisticsIterator() throws Exception {
        // ncount returns 200 > 100 and PV type is SCALAR_DOUBLE → ApplianceStatisticsValueIterator
        FakeDataRetrieval dr = new FakeDataRetrieval(probeStream(PayloadType.SCALAR_DOUBLE));
        dr.whenPvContains("ncount(", countStream(200));
        dr.whenPvContains("mean_", emptyStream());
        dr.whenPvContains("std_", emptyStream());
        dr.whenPvContains("min_", emptyStream());
        dr.whenPvContains("max_", emptyStream());
        dr.whenPvContains("count_", emptyStream());
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr, true, false);
        Object iter = reader.getOptimizedValues("TEST:PV", START, END, 100);
        assertInstanceOf(ApplianceStatisticsValueIterator.class, iter);
    }

    @Test
    void numericScalarWithUseStatisticsFalseUsesMeanIterator() throws Exception {
        // ncount returns 200 > 100, useStatistics=false → ApplianceMeanValueIterator (not statistics subclass)
        FakeDataRetrieval dr = new FakeDataRetrieval(probeStream(PayloadType.SCALAR_DOUBLE));
        dr.whenPvContains("ncount(", countStream(200));
        dr.whenPvContains("mean_", emptyStream());
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr, false, false);
        Object iter = reader.getOptimizedValues("TEST:PV", START, END, 100);
        assertEquals(ApplianceMeanValueIterator.class, iter.getClass(),
                "expected exact ApplianceMeanValueIterator, not a statistics subclass");
    }

    @Test
    void oldApplianceFallbackOnFetchFailure() throws Exception {
        // Statistics fetch fails mid-construction (std_ stream null → ArchiverApplianceException)
        // → outer catch falls back to ApplianceRawValueIterator
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        dr.whenPvContains("ncount(", countStream(200));
        dr.whenPvContains("mean_", emptyStream());
        dr.whenPvContains("std_", null); // null triggers ArchiverApplianceException in statistics iterator
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr, true, false);
        Object iter = reader.getOptimizedValues("TEST:PV", START, END, 100);
        assertInstanceOf(ApplianceRawValueIterator.class, iter);
    }

    @Test
    void testUpdateDisplay1() {
        Display original = Display.of(Range.of(-1, 1),
                Range.of(-3, 3),
                Range.of(-2, 2),
                Range.of(-4, 4),
                "unit",
                new DecimalFormat(),
                "description");

        EpicsMessage epicsMessage = Mockito.mock(EpicsMessage.class);
        when(epicsMessage.getFieldValues()).thenReturn(Collections.emptyMap());

        Display updated = ApplianceValueIterator.updateDisplay(epicsMessage, original);
        assertEquals(original, updated);
        assertSame(original, updated);
    }

    @Test
    void testUpdateDisplay2() {
        Display original = Display.of(Range.of(-1, 1),
                Range.of(-3, 3),
                Range.of(-2, 2),
                Range.of(-4, 4),
                "unit",
                NumberFormats.precisionFormat(7));

        EpicsMessage epicsMessage = Mockito.mock(EpicsMessage.class);
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put(ApplianceArchiveReaderConstants.LOW, "-10");
        fieldValues.put(ApplianceArchiveReaderConstants.HIGH, "10");
        fieldValues.put(ApplianceArchiveReaderConstants.LOLO, "-30");
        fieldValues.put(ApplianceArchiveReaderConstants.HIHI, "30");
        fieldValues.put(ApplianceArchiveReaderConstants.LOPR, "-40");
        fieldValues.put(ApplianceArchiveReaderConstants.HOPR, "40");
        fieldValues.put(ApplianceArchiveReaderConstants.EGU, "otherunit");
        fieldValues.put(ApplianceArchiveReaderConstants.PREC, "10");
        when(epicsMessage.getFieldValues()).thenReturn(fieldValues);

        Display updated = ApplianceValueIterator.updateDisplay(epicsMessage, original);

        assertNotEquals(original, updated);
        assertNotSame(original, updated);
        assertNotEquals(original.getDisplayRange(), updated.getDisplayRange());
        assertNotEquals(original.getWarningRange(), updated.getWarningRange());
        assertNotEquals(original.getAlarmRange(), updated.getAlarmRange());
        assertEquals(original.getControlRange(), updated.getControlRange());
        assertNotEquals(original.getUnit(), updated.getUnit());
        assertNotEquals(original.getFormat(), updated.getFormat());

    }

    @Test
    void testUpdateDisplay3() {
        Display original = Display.of(Range.of(-1, 1),
                Range.of(-3, 3),
                Range.of(-2, 2),
                Range.of(-4, 4),
                "unit",
                NumberFormats.precisionFormat(7));

        EpicsMessage epicsMessage = Mockito.mock(EpicsMessage.class);
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put(ApplianceArchiveReaderConstants.HIGH, "10");
        fieldValues.put(ApplianceArchiveReaderConstants.LOLO, "-30");
        fieldValues.put(ApplianceArchiveReaderConstants.HIHI, "30");
        fieldValues.put(ApplianceArchiveReaderConstants.LOPR, "-40");
        fieldValues.put(ApplianceArchiveReaderConstants.HOPR, "40");
        fieldValues.put(ApplianceArchiveReaderConstants.EGU, "otherunit");
        fieldValues.put(ApplianceArchiveReaderConstants.PREC, "10");
        when(epicsMessage.getFieldValues()).thenReturn(fieldValues);

        Display updated = ApplianceValueIterator.updateDisplay(epicsMessage, original);

        assertNotEquals(original, updated);
        assertNotSame(original, updated);
        assertNotEquals(original.getDisplayRange(), updated.getDisplayRange());
        assertNotEquals(original.getWarningRange(), updated.getWarningRange());
        // ApplianceArchiveReaderConstants.LOW not present in field values, so it should be unchanged
        assertEquals(-2, original.getWarningRange().getMinimum());
        assertNotEquals(original.getAlarmRange(), updated.getAlarmRange());
        assertEquals(original.getControlRange(), updated.getControlRange());
        assertNotEquals(original.getUnit(), updated.getUnit());
        assertNotEquals(original.getFormat(), updated.getFormat());
    }
}

