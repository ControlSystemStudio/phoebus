/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.List;

/**
 * Data class for the {@link javafx.scene.control.TableView} of the comparison dialog.
 */
public class ComparisonData {

    /**
     * Index (=row number) for this instance.
     */
    private final IntegerProperty index = new SimpleIntegerProperty(this, "index");
    /**
     * {@link List} of {@link ColumnEntry}s, one for each column in the data. For array data this will
     * hold only one element.
     */
    private final List<ColumnEntry> columnEntries;

    public ComparisonData(int index, List<ColumnEntry> columnEntries) {
        this.index.set(index);
        this.columnEntries = columnEntries;
    }

    @SuppressWarnings("unused")
    public IntegerProperty indexProperty() {
        return index;
    }

    public List<ColumnEntry> getColumnEntries() {
        return columnEntries;
    }
}
