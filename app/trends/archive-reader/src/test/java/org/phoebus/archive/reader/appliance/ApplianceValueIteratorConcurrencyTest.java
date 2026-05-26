package org.phoebus.archive.reader.appliance;

import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.phoebus.archive.reader.ValueIterator;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplianceValueIteratorConcurrencyTest {

    private static GenMsgIterator emptyStream() {
        GenMsgIterator s = mock(GenMsgIterator.class);
        when(s.iterator()).thenReturn(Collections.emptyIterator());
        return s;
    }

    private static ValueIterator getRaw(FakeApplianceArchiveReader reader) throws Exception {
        Instant start = Instant.now().minusSeconds(60);
        return reader.getRawValues("TESTPV", start, Instant.now());
    }

    /**
     * Regression test for the hasNext()/close() deadlock.
     *
     * With the old synchronized hasNext(), a worker blocked inside mainIterator.hasNext()
     * held the 'this' monitor. close() could never acquire it, so the job hung forever
     * and the semaphore permit was never returned. @Timeout(5) catches the hang.
     */
    @Test
    @Timeout(5)
    void closeCompletesWhileHasNextIsBlocking() throws Exception {
        BlockingGenMsgIterator blocker = new BlockingGenMsgIterator();
        GenMsgIterator stream = mock(GenMsgIterator.class);
        when(stream.iterator()).thenReturn(blocker);

        FakeApplianceArchiveReader reader = new FakeApplianceArchiveReader(new FakeDataRetrieval(stream));
        ValueIterator iter = getRaw(reader);

        Thread worker = new Thread(iter::hasNext);
        worker.start();
        blocker.entered.await();    // worker is now blocked inside hasNext()

        iter.close();               // must not deadlock
        assertFalse(iter.hasNext());

        blocker.release.countDown();
        worker.join(1000);
        assertFalse(worker.isAlive());
    }

    @Test
    void hasNextReturnsFalseAfterClose() throws Exception {
        EpicsMessage msg = mock(EpicsMessage.class);
        GenMsgIterator stream = mock(GenMsgIterator.class);
        when(stream.iterator()).thenReturn(Collections.singletonList(msg).iterator());

        ValueIterator iter = getRaw(new FakeApplianceArchiveReader(new FakeDataRetrieval(stream)));

        assertTrue(iter.hasNext());
        iter.close();
        assertFalse(iter.hasNext());
    }

    @Test
    void nextReturnsNullAfterClose() throws Exception {
        EpicsMessage msg = mock(EpicsMessage.class);
        GenMsgIterator stream = mock(GenMsgIterator.class);
        when(stream.iterator()).thenReturn(Collections.singletonList(msg).iterator());

        ValueIterator iter = getRaw(new FakeApplianceArchiveReader(new FakeDataRetrieval(stream)));
        iter.close();
        assertNull(iter.next());
    }

    /**
     * Regression test for the static lock serialising fetches across all PV iterators.
     *
     * With a static lock, t1 holding it inside getDataForPVs blocks t2 even though
     * they belong to completely independent readers. @Timeout(5) catches the hang.
     */
    @Test
    @Timeout(5)
    void twoIteratorFetchesProceedConcurrently() throws Exception {
        CountDownLatch firstInFetch = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);

        FakeDataRetrieval dr1 = new FakeDataRetrieval(emptyStream()) {
            @Override
            public GenMsgIterator getDataForPVs(List<String> pvNames, Timestamp start,
                                                Timestamp end, boolean b, Map<String, String> m) {
                firstInFetch.countDown();
                try { releaseFetch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return super.getDataForPVs(pvNames, start, end, b, m);
            }
        };
        FakeDataRetrieval dr2 = new FakeDataRetrieval(emptyStream());

        FakeApplianceArchiveReader reader1 = new FakeApplianceArchiveReader(dr1);
        FakeApplianceArchiveReader reader2 = new FakeApplianceArchiveReader(dr2);
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now();

        Thread t1 = new Thread(() -> {
            try { reader1.getRawValues("PV1", start, end); }
            catch (Exception ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try { reader2.getRawValues("PV2", start, end); }
            catch (Exception ignored) {}
        });

        t1.start();
        firstInFetch.await();   // t1 is inside getDataForPVs, holding the lock

        t2.start();
        t2.join(2000);          // t2 must finish while t1 is still blocked
        assertFalse(t2.isAlive(), "t2 blocked — static lock contention between independent iterators");

        releaseFetch.countDown();
        t1.join(1000);
    }
}
