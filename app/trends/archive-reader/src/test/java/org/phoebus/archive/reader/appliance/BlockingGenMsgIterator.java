package org.phoebus.archive.reader.appliance;

import org.epics.archiverappliance.retrieval.client.EpicsMessage;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;

/**
 * Iterator whose hasNext() blocks until released, for concurrency tests.
 *
 * The caller awaits {@link #entered} to confirm the worker thread is inside
 * hasNext(), then counts down {@link #release} to unblock it.
 */
class BlockingGenMsgIterator implements Iterator<EpicsMessage> {

    final CountDownLatch entered = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);

    @Override
    public boolean hasNext() {
        entered.countDown();
        try {
            release.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public EpicsMessage next() {
        throw new NoSuchElementException();
    }
}
