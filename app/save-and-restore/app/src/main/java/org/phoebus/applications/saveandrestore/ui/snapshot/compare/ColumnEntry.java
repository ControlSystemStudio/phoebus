/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VDisconnectedData;

public class ColumnEntry<T> {

    private final ObjectProperty<T> snapshotVal = new SimpleObjectProperty<>(this, "snapshotValue", null);
    private final ObjectProperty<T> delta = new SimpleObjectProperty<>(this, "delta", null);
    private final ObjectProperty<T> liveVal = new SimpleObjectProperty<>(this, "liveValue", (T) VDisconnectedData.INSTANCE);

    public ColumnEntry(T snapshotVal){
        this.snapshotVal.set(snapshotVal);
    }

    public ObjectProperty getSnapshotValue(){
        return snapshotVal;
    }

    public void setLiveVal(T value){
        liveVal.set(value);
    }

    public ObjectProperty<T> getLiveValue(){
        return liveVal;
    }
}
