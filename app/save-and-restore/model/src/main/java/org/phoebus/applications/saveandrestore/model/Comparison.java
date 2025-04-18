/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model;

public class Comparison{

    private ComparisonMode comparisonMode;
    private Double tolerance;

    public Comparison(){

    }

    public Comparison(ComparisonMode comparisonMode, Double tolerance){
        this.comparisonMode = comparisonMode;
        this.tolerance = tolerance;
    }

    public ComparisonMode getComparisonMode() {
        return comparisonMode;
    }

    public void setComparisonMode(ComparisonMode comparisonMode) {
        this.comparisonMode = comparisonMode;
    }

    public Double getTolerance() {
        return tolerance;
    }

    public void setTolerance(Double tolerance) {
        this.tolerance = tolerance;
    }
}
