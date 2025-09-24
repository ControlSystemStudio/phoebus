/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents an entry in the PV table for the Chart Browser application.
 * Holds the process variable name, archive/raw data flags, time range (start and end dates),
 * and buffer size for data retrieval.
 */
public class PVTableEntry {
    private final StringProperty pvName;
    private final BooleanProperty useArchive;
    private final BooleanProperty useRawData;
    private final ObjectProperty<Integer> bufferSize;
    private final ObjectProperty<String> meanValue;

    /**
     * Creates a new PVTableEntry with the given process variable name.
     * Initializes archive usage to false, raw data usage to true,
     * start date to one hour before the current time, end date to the current time,
     * and buffer size to 5000.
     *
     * @param pvName the name of the process variable
     */
    public PVTableEntry(String pvName) {
        this.pvName = new SimpleStringProperty(pvName);
        this.useArchive = new SimpleBooleanProperty(false);

        this.useRawData = new SimpleBooleanProperty(false);

        this.bufferSize = new SimpleObjectProperty<>(5_000);
        this.meanValue = new SimpleObjectProperty<>("");
    }

    /**
     * Gets the buffer size property representing the maximum number of data points to fetch.
     *
     * @return the buffer size property
     */
    public ObjectProperty<Integer> bufferSizeProperty() {
        return bufferSize;
    }

    /**
     * Gets the current buffer size.
     *
     * @return the buffer size value
     */
    public Integer getBufferSize() {
        return bufferSize.get();
    }

    /**
     * Sets the buffer size.
     *
     * @param size the new buffer size value
     */
    public void setBufferSize(Integer size) {
        bufferSize.set(size);
    }

    /**
     * Gets the PV name property.
     *
     * @return the PV name property
     */
    public StringProperty pvNameProperty() {
        return pvName;
    }

    /**
     * Gets the process variable name.
     *
     * @return the PV name value
     */
    public String getPvName() {
        return pvName.get();
    }

    /**
     * Sets the process variable name.
     *
     * @param name the new PV name
     */
    public void setPvName(String name) {
        pvName.set(name);
    }

    public void setUseRawData(boolean rawDataStatus)
    {
        useRawData.set(rawDataStatus);
    }

    /**
     * Gets the archive usage property.
     *
     * @return the archive usage property
     */
    public BooleanProperty useArchiveProperty() {
        return useArchive;
    }

    /**
     * Checks whether archive usage is enabled.
     *
     * @return true if archive usage is enabled, false otherwise
     */
    public boolean isUseArchive() {
        return useArchive.get();
    }

    /**
     * Sets whether to use archive.
     *
     * @param use true to enable archive usage, false to disable
     */
    public void setUseArchive(boolean use) {
        useArchive.set(use);
    }

    /**
     * Gets the raw data usage property.
     *
     * @return the raw data usage property
     */
    public BooleanProperty useRawDataProperty() {
        return useRawData;
    }

    /**
     * Checks whether raw data usage is enabled.
     *
     * @return true if raw data usage is enabled, false otherwise
     */
    public boolean isUseRawData() {
        return useRawData.get();
    }

    /**
     * Sets whether to compute mean on a duration.
     *
     * @param value true to enable archive by mean computation, false to disable
     */
    public void meanValue(String value) {
        meanValue.set(value);
    }

    public ObjectProperty<String> meanValueProperty() {
        return meanValue;
    }

    /**
     * Checks whether mean computation is enabled.
     *
     * @return true if mean computation is enabled, false otherwise
     */
    public String getMeanValue() {
        return meanValue.get();
    }
}
