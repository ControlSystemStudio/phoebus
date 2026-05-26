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

class ApplianceMeanValueIteratorTest {

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

    @Test
    void fetchUrlContainsMeanIntervalOperator() throws Exception {
        FakeDataRetrieval dr = new FakeDataRetrieval(probeStream(PayloadType.SCALAR_DOUBLE));
        dr.whenPvContains("mean_", emptyStream());

        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);
        new ApplianceMeanValueIterator(reader, "TEST:PV", START, END, POINTS);

        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("mean_") && pv.endsWith("(TEST:PV)")),
                "Expected mean_<interval>(TEST:PV) in calls, got: " + dr.pvsCalled);
    }

    @Test
    void determineDisplayRejectsEnum() {
        FakeDataRetrieval dr = new FakeDataRetrieval(probeStream(PayloadType.SCALAR_ENUM));
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);

        assertThrows(ArchiverApplianceInvalidTypeException.class,
                () -> new ApplianceMeanValueIterator(reader, "TEST:PV", START, END, POINTS));
    }

    @Test
    void determineDisplayAcceptsDouble() throws Exception {
        FakeDataRetrieval dr = new FakeDataRetrieval(probeStream(PayloadType.SCALAR_DOUBLE));
        dr.whenPvContains("mean_", emptyStream());

        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);
        ApplianceMeanValueIterator iter = new ApplianceMeanValueIterator(reader, "TEST:PV", START, END, POINTS);

        assertNotNull(iter.display);
    }
}
