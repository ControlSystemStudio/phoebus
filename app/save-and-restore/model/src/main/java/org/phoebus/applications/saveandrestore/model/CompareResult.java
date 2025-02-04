/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.model.json.VTypeDeserializer;
import org.phoebus.applications.saveandrestore.model.json.VTypeSerializer;

/**
 * Pojo class holding data to describe the outcome of a PV comparison operation, i.e.
 * comparison of a save-and-restore stored PV value and a live value. The idea is to
 * (roughly) provide the same data as the snapshot UI.
 *
 * For the live {@link VType} value, <code>null</code> indicates failure to connect to the PV.
 */
@SuppressWarnings("unused")
public class CompareResult{

    private String pvName;
    private boolean equal;
    private PvCompareMode pvCompareMode;
    private double tolerance;
    @JsonSerialize(using = VTypeSerializer.class)
    @JsonDeserialize(using = VTypeDeserializer.class)
    private VType storedValue;
    @JsonSerialize(using = VTypeSerializer.class)
    @JsonDeserialize(using = VTypeDeserializer.class)
    private VType liveValue;
    private String delta;

    public CompareResult(){

    }

    public CompareResult(String pvName,
        boolean equal,
        PvCompareMode pvCompareMode,
        double tolerance,
        VType storedValue,
        VType liveValue,
        String delta){

        this.pvName = pvName;
        this.equal = equal;
        this.pvCompareMode = pvCompareMode;
        this.tolerance = tolerance;
        this.storedValue = storedValue;
        this.liveValue = liveValue;
        this.delta = delta;
    }

    public boolean isEqual() {
        return equal;
    }

    public void setEqual(boolean equal) {
        this.equal = equal;
    }

    public PvCompareMode getPvCompareMode() {
        return pvCompareMode;
    }

    public void setPvCompareMode(PvCompareMode pvCompareMode) {
        this.pvCompareMode = pvCompareMode;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public VType getStoredValue() {
        return storedValue;
    }

    public void setStoredValue(VType storedValue) {
        this.storedValue = storedValue;
    }

    /**
     *
     * @return <code>null</code> indicates failure to connect to PV.
     */
    public VType getLiveValue() {
        return liveValue;
    }

    public void setLiveValue(VType liveValue) {
        this.liveValue = liveValue;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    public String getPvName() {
        return pvName;
    }

    public void setPvName(String pvName) {
        this.pvName = pvName;
    }
}
