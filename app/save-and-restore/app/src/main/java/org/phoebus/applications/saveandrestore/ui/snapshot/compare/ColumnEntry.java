/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.saveandrestore.util.Threshold;
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
     * The libe {@link VType} value as read from a connected PV.
     */
    private final ObjectProperty<VType> liveValue = new SimpleObjectProperty<>(this, "liveValue", VNoData.INSTANCE);

    private Optional<Threshold<?>> threshold = Optional.empty();

    public ColumnEntry(VType storedValue) {
        this.storedValue.set(storedValue);
    }

    public ObjectProperty<VType> storedValueProperty() {
        return storedValue;
    }

    public void setLiveVal(VType value) {
        liveValue.set(value);
        VTypePair vTypePair = new VTypePair(storedValue.get(), value, threshold);
        delta.set(vTypePair);
    }

    public ObjectProperty<VType> liveValueProperty() {
        return liveValue;
    }

    public ObjectProperty<VTypePair> getDelta() {
        return delta;
    }
}
