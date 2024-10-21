/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;

import org.phoebus.applications.saveandrestore.Messages;

public enum RestoreMode {

    /**
     * Classic mode: read data from IOC
     */
    CLIENT_RESTORE(Messages.restoreFromClient),

    /**
     * Read PV data from archiver
     */
    SERVICE_RESTORE(Messages.restoreFromService);

    private final String name;

    RestoreMode(final String name) {
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }
}
