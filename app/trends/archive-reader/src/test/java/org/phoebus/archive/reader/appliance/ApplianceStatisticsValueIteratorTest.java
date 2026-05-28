package org.phoebus.archive.reader.appliance;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadInfo;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplianceStatisticsValueIteratorTest {

    private static final Instant START = Instant.now().minusSeconds(3600);
    private static final Instant END = Instant.now();
    private static final int POINTS = 60;

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

    private ApplianceStatisticsValueIterator makeIterator(FakeDataRetrieval dr)
            throws ArchiverApplianceException, java.io.IOException {
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);
        return new ApplianceStatisticsValueIterator(reader, "TEST:PV", START, END, POINTS);
    }

    private FakeDataRetrieval setupDr(GenMsgIterator meanStream, GenMsgIterator stdStream,
                                      GenMsgIterator minStream, GenMsgIterator maxStream,
                                      GenMsgIterator countStream) {
        FakeDataRetrieval dr = new FakeDataRetrieval(probeStream(PayloadType.SCALAR_DOUBLE));
        dr.whenPvContains("mean_", meanStream);
        dr.whenPvContains("std_", stdStream);
        dr.whenPvContains("min_", minStream);
        dr.whenPvContains("max_", maxStream);
        dr.whenPvContains("count_", countStream);
        return dr;
    }

    @Test
    void fetchIssuesFiveOperatorCalls() throws Exception {
        FakeDataRetrieval dr = setupDr(emptyStream(), emptyStream(), emptyStream(), emptyStream(), emptyStream());
        makeIterator(dr);

        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("mean_")), "missing mean_ call");
        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("std_")),  "missing std_ call");
        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("min_")),  "missing min_ call");
        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("max_")),  "missing max_ call");
        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("count_")), "missing count_ call");
    }

    @Test
    void closeClosesAllFiveStreams() throws Exception {
        GenMsgIterator meanStream  = emptyStream();
        GenMsgIterator stdStream   = emptyStream();
        GenMsgIterator minStream   = emptyStream();
        GenMsgIterator maxStream   = emptyStream();
        GenMsgIterator countStream = emptyStream();

        FakeDataRetrieval dr = setupDr(meanStream, stdStream, minStream, maxStream, countStream);
        ApplianceStatisticsValueIterator iter = makeIterator(dr);

        iter.close();

        verify(meanStream).close();
        verify(stdStream).close();
        verify(minStream).close();
        verify(maxStream).close();
        verify(countStream).close();
    }

    // ---- next() assembly and lifecycle ----

    /** One-message stream returning the given numeric value; mean stream gets SCALAR_DOUBLE PayloadInfo. */
    private static GenMsgIterator valueStream(double value, boolean withPayloadInfo) {
        EpicsMessage msg = mock(EpicsMessage.class);
        when(msg.getNumberValue()).thenReturn(value);
        when(msg.getSeverity()).thenReturn(0);
        when(msg.getStatus()).thenReturn(0);
        when(msg.getTimestamp()).thenReturn(new Timestamp(System.currentTimeMillis()));
        GenMsgIterator s = mock(GenMsgIterator.class);
        when(s.iterator()).thenReturn(Collections.singletonList(msg).iterator());
        if (withPayloadInfo)
            when(s.getPayLoadInfo()).thenReturn(
                    PayloadInfo.newBuilder().setType(PayloadType.SCALAR_DOUBLE).buildPartial());
        return s;
    }

    @Test
    void nextAssemblesVStatisticsFromFiveIterators() throws Exception {
        GenMsgIterator meanStream  = valueStream(2.0, true);
        GenMsgIterator stdStream   = valueStream(0.5, false);
        GenMsgIterator minStream   = valueStream(1.0, false);
        GenMsgIterator maxStream   = valueStream(3.0, false);
        GenMsgIterator countStream = valueStream(10.0, false);

        FakeDataRetrieval dr = setupDr(meanStream, stdStream, minStream, maxStream, countStream);
        ApplianceStatisticsValueIterator iter = makeIterator(dr);

        assertTrue(iter.hasNext());
        VType result = iter.next();

        assertInstanceOf(VStatistics.class, result);
        VStatistics vs = (VStatistics) result;
        assertEquals(2.0, vs.getAverage(),  0.001);
        assertEquals(0.5, vs.getStdDev(),   0.001);
        assertEquals(1.0, vs.getMin(),      0.001);
        assertEquals(3.0, vs.getMax(),      0.001);
        assertEquals(10,  (int) vs.getNSamples());
    }

    @Test
    void nextReturnsNullWhenClosed() throws Exception {
        GenMsgIterator meanStream  = valueStream(1.0, true);
        FakeDataRetrieval dr = setupDr(meanStream, emptyStream(), emptyStream(), emptyStream(), emptyStream());
        ApplianceStatisticsValueIterator iter = makeIterator(dr);

        iter.close();

        assertNull(iter.next(), "next() after close() should return null without throwing");
    }
}
