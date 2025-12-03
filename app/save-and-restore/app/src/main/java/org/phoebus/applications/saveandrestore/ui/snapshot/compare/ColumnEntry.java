/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.phoebus.core.vtypes.VDisconnectedData;

/**
 * Data class for one column in the comparison table.
 * @param <T>
 */
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
        delta.set(new ColumnDelta());
    }

    public ObjectProperty<T> getLiveValue(){
        return liveVal;
    }

    public ObjectProperty<ColumnDelta> getDelta(){
        return delta;
    }

    /**
     * Class wrapping data needed to render or sort the delta column.
     */
    public class ColumnDelta{
        private final boolean equal;
        private String displayString;
        private final double absoluteDelta;

        public ColumnDelta(){
            if(liveVal.get() instanceof String){
                String stringValue = (String)liveVal.get();
                displayString = (String)snapshotVal.get();
                absoluteDelta = Math.abs((displayString).compareTo(stringValue));
            }
            else{
                double valueNumber = ((Number) liveVal.get()).doubleValue();
                double diff = ((Number) snapshotVal.get()).doubleValue() - valueNumber;
                displayString = Double.toString(diff);
                if(diff > 0){
                    displayString = "+" + diff;
                }
                absoluteDelta = Math.abs(diff);
            }
            equal = absoluteDelta == 0;
        }

        public double getAbsoluteDelta(){
            return absoluteDelta;
        }

        public boolean isEqual() {
            return equal;
        }

        @Override
        public String toString(){
            return displayString;
        }
    }
}
