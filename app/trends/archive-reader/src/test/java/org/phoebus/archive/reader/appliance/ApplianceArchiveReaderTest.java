package org.phoebus.archive.reader.appliance;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadInfo;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Collections;

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
}
