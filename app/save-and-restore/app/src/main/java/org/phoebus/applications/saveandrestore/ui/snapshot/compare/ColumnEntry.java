/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.epics.vtype.VNumber;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.SafeMultiply;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.saveandrestore.util.Threshold;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.saveandrestore.util.VNoData;

import java.util.Optional;

/**
 * Data class for one column in the comparison table.
 */
public class ColumnEntry {

    /**
     * The {@link VType} value as stored in a {@link org.phoebus.applications.saveandrestore.model.Snapshot}
     */
    private final ObjectProperty<VType> storedValue = new SimpleObjectProperty<>(this, "storedValue", null);
    /**
     * A {@link VTypePair} property holding data for the purpose of calculating and showing a delta.
     */
    private final ObjectProperty<VTypePair> delta = new SimpleObjectProperty<>(this, "delta", null);
    /**
     * The live {@link VType} value as read from a connected PV.
     */
    private final ObjectProperty<VType> liveValue = new SimpleObjectProperty<>(this, "liveValue", VNoData.INSTANCE);

    private Optional<Threshold<?>> threshold = Optional.empty();

    public ColumnEntry(VType storedValue) {
        this.storedValue.set(storedValue);
    }

    public ObjectProperty<VType> storedValueProperty() {
        return storedValue;
    }

    public void setLiveVal(VType liveValue) {
        this.liveValue.set(liveValue);
        VTypePair vTypePair = new VTypePair(liveValue, storedValue.get(), threshold);
        delta.set(vTypePair);
    }

    public ObjectProperty<VType> liveValueProperty() {
        return liveValue;
    }

    public ObjectProperty<VTypePair> getDelta() {
        return delta;
    }

    /**
     * Set the threshold value for this entry. All value comparisons related to this entry are calculated using the
     * threshold (if it exists).
     *
     * @param ratio the threshold
     */
    public void setThreshold(double ratio) {
        if (storedValue.get() instanceof VNumber) {
            VNumber vNumber = SafeMultiply.multiply((VNumber) storedValue.get(), ratio);
            boolean isNegative = vNumber.getValue().doubleValue() < 0;
            Threshold t = new Threshold<>(isNegative ? SafeMultiply.multiply(vNumber.getValue(), -1.0) : vNumber.getValue());
            this.delta.set(new VTypePair(liveValue.get(), storedValue.get(), Optional.of(t)));
        }
    }
}
