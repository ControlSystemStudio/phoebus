/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.datamigration.git;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.phoebus.applications.saveandrestore.ui.model.SnapshotEntry;

/**
 * <code>SnapshotContent</code> provides the raw data as they were read from the snapshot file. This is only a container
 * for the data and does not provide any other functionality.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public final class SnapshotContent {

    private final Instant date;
    private final List<SnapshotEntry> entries;

    /**
     * Constructs a new snapshot content.
     *
     * @param date the time when snapshot was taken
     * @param entries the entries of this snapshot
     */
    SnapshotContent(Instant date, List<SnapshotEntry> entries) {
        this.entries = Collections.unmodifiableList(entries);
        this.date = date;
    }

    /**
     * Returns the list of all entries in this snapshot.
     *
     * @return the list of entries
     */
    public List<SnapshotEntry> getEntries() {
        return entries;
    }

    /**
     * Returns the full date, when this snapshot was taken.
     *
     * @return the date
     */
    public Instant getDate() {
        return date;
    }
}
