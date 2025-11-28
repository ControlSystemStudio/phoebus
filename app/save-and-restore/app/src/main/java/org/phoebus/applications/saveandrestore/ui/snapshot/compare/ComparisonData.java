/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;

import java.util.ArrayList;
import java.util.List;

public class ComparisonData {

    private final IntegerProperty index = new SimpleIntegerProperty(this, "index");
    private List<ColumnEntry> columnEntries;

    public ComparisonData(int index, List<ColumnEntry> columnEntries){
        this.index.set(index);
        this.columnEntries = columnEntries;
    }

    @SuppressWarnings("unused")
    public IntegerProperty indexProperty() {
        return index;
    }

    public List<ColumnEntry> getColumnEntries(){
        return columnEntries;
    }
}
