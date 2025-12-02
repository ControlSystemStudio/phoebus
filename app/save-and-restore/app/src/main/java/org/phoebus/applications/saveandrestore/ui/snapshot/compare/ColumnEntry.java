/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.saveandrestore.util.Utilities;

public class ColumnEntry<T> {

    private final ObjectProperty<T> snapshotVal = new SimpleObjectProperty<>(this, "snapshotValue", null);
    private final ObjectProperty<ColumnDelta> delta = new SimpleObjectProperty<>(this, "delta", null);
    private final ObjectProperty<T> liveVal = new SimpleObjectProperty<>(this, "liveValue", (T) VDisconnectedData.INSTANCE);

    public ColumnEntry(T snapshotVal){
        this.snapshotVal.set(snapshotVal);
    }

    public ObjectProperty getSnapshotValue(){
        return snapshotVal;
    }

    public void setLiveVal(T value){
        liveVal.set(value);
        if(value instanceof String){
            String stringValue = (String)value;
            delta.set(new ColumnDelta(stringValue, stringValue.equals(snapshotVal.get())));
        }
        else{
            Double valueNumber = ((Number) value).doubleValue();
            Double diff = ((Number) snapshotVal.get()).doubleValue() - valueNumber;
            String diffString = Double.toString(diff);
            if(diff > 0){
                diffString = "+" + diff;
            }
            delta.set(new ColumnDelta(diffString, diff != 0));
        }
    }

    public ObjectProperty<T> getLiveValue(){
        return liveVal;
    }

    public ObjectProperty<ColumnDelta> getDelta(){
        return delta;
    }

    public static class ColumnDelta{
        private String deltaString;
        private boolean equal;

        public ColumnDelta(String deltaString, boolean equal){
            this.deltaString = deltaString;
            this.equal = equal;
        }

        public boolean isEqual() {
            return equal;
        }

        @Override
        public String toString(){
            return deltaString;
        }
    }
}
