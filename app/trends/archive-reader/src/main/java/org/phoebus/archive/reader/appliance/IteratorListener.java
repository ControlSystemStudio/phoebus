package org.phoebus.archive.reader.appliance;

/** Listener to iterator */
public interface IteratorListener {
    /** @param source Value iterator */
    public void finished(ApplianceValueIterator source);
}
