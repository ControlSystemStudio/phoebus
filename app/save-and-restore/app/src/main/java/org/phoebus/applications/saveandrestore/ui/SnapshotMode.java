/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;

import org.phoebus.applications.saveandrestore.Messages;

public enum SnapshotMode {

    /**
     * Classic mode: read data from IOC
     */
    READ_PVS(Messages.snapshotFromPvs),

    /**
     * Read PV data from archiver
     */
    FROM_ARCHIVER(Messages.snapshotFromArchiver);

    private final String name;

    SnapshotMode(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
