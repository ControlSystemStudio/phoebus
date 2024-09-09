/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model;

public enum SnapshotMode {

    READ_PVS("Read PVssss"),
    READ_FROM_ARCHIVER("Read from Archiver");

    SnapshotMode(String name){
        this.name = name;
    }

    private String name;
}
