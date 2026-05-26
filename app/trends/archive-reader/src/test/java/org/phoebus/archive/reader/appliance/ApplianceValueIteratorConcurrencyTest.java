package org.phoebus.archive.reader.appliance;

import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.phoebus.archive.reader.ValueIterator;

import java.time.Instant;
import java.util.Collections;

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
}
