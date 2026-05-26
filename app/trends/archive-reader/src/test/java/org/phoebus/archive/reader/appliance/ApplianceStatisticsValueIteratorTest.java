package org.phoebus.archive.reader.appliance;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadInfo;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.junit.jupiter.api.Test;

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
}
