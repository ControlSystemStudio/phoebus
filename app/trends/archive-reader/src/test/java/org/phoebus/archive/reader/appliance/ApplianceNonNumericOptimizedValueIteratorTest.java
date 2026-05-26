package org.phoebus.archive.reader.appliance;

import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplianceNonNumericOptimizedValueIteratorTest {

    private static final Instant START = Instant.now().minusSeconds(3600);
    private static final Instant END = Instant.now();

    private static GenMsgIterator emptyStream() {
        GenMsgIterator s = mock(GenMsgIterator.class);
        when(s.iterator()).thenReturn(Collections.emptyIterator());
        return s;
    }

    private ApplianceNonNumericOptimizedValueIterator makeIterator(
            FakeDataRetrieval dr, int requestedPoints, int totalPoints)
            throws ArchiverApplianceException, java.io.IOException {
        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(dr);
        return new ApplianceNonNumericOptimizedValueIterator(reader, "TEST:PV", START, END,
                requestedPoints, totalPoints);
    }

    @Test
    void nEqualsOneCallsRawFetch() throws Exception {
        // totalPoints == requestedPoints → n = 1 → fetches bare PV name
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        makeIterator(dr, 100, 100);
        assertTrue(dr.pvsCalled.contains("TEST:PV"),
                "Expected bare PV name, got: " + dr.pvsCalled);
    }

    @Test
    void nGreaterThanOneUsesNthOperator() throws Exception {
        // totalPoints = 1000, requestedPoints = 100 → n = 10 → nth_10(TEST:PV)
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        makeIterator(dr, 100, 1000);
        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("nth_")),
                "Expected nth_ prefix, got: " + dr.pvsCalled);
    }

    @Test
    void boundaryAt1point5xGivesN2() throws Exception {
        // 1.5 * 100 < 160 < 2 * 100 → n forced to 2, regardless of integer division (160/100=1)
        FakeDataRetrieval dr = new FakeDataRetrieval(emptyStream());
        makeIterator(dr, 100, 160);
        assertTrue(dr.pvsCalled.stream().anyMatch(pv -> pv.startsWith("nth_2(")),
                "Expected nth_2( prefix at boundary, got: " + dr.pvsCalled);
    }
}
