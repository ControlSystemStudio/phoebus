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
public class ComparisonResult {

    private String pvName;
    private boolean equal;
    private Comparison comparison;
    @JsonSerialize(using = VTypeSerializer.class)
    @JsonDeserialize(using = VTypeDeserializer.class)
    private VType storedValue;
    @JsonSerialize(using = VTypeSerializer.class)
    @JsonDeserialize(using = VTypeDeserializer.class)
    private VType liveValue;
    private String delta;

    /**
     * Needed by unit tests. Do not remove.
     */
    public ComparisonResult(){}

    public ComparisonResult(String pvName,
                            boolean equal,
                            Comparison comparison,
                            VType storedValue,
                            VType liveValue,
                            String delta){

        this.pvName = pvName;
        this.equal = equal;
        this.comparison = comparison;
        this.storedValue = storedValue;
        this.liveValue = liveValue;
        this.delta = delta;
    }

    public boolean isEqual() {
        return equal;
    }

    public Comparison getComparison() {
        return comparison;
    }

    public void setComparison(Comparison comparison) {
        this.comparison = comparison;
    }

    /**
     *
     * @return <code>null</code> indicates failure to connect to PV.
     */
    public VType getStoredValue() {
        return storedValue;
    }

    public String getDelta() {
        return delta;
    }

    /**
     *
     * @return <code>null</code> indicates failure to connect to PV.
     */
    public VType getLiveValue() {
        return liveValue;
    }

    public String getPvName() {
        return pvName;
    }
}
