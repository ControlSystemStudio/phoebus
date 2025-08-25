/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.epics.pva.data.nt.PVAAlarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.ui.SingleListenerBooleanProperty;
import org.phoebus.saveandrestore.util.Threshold;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.saveandrestore.util.VNoData;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.core.vtypes.VDisconnectedData;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <code>TableEntry</code> represents a single line in the snapshot viewer table. It provides values for all columns in
 * the table, be it a single snapshot table or a multi snapshots table.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 * <p>
 * This code has been modified at the European Spallation Source (ESS), Lund, Sweden.
 * </p>
 */
public class TableEntry {

    /**
     * Holds result of a take snapshot or restore action for the PV.
     */
    private final ObjectProperty<ActionResult> actionResult = new SimpleObjectProperty<>(this, "actionResult", ActionResult.PENDING);
    private final IntegerProperty id = new SimpleIntegerProperty(this, "id");
    private final SingleListenerBooleanProperty selected = new SingleListenerBooleanProperty(this, "selected", true);
    private final StringProperty pvName = new SimpleStringProperty(this, "pvName");
    private final ObjectProperty<Instant> timestamp = new SimpleObjectProperty<>(this, "timestamp");
    private final StringProperty liveStatus = new SimpleStringProperty(this, "liveStatus", "---");
    private final StringProperty storedStatus = new SimpleStringProperty(this, "storedStatus", PVAAlarm.AlarmStatus.UNDEFINED.name());
    private final ObjectProperty<AlarmSeverity> liveSeverity = new SimpleObjectProperty<>(this, "liveSeverity", null);
    private final ObjectProperty<AlarmSeverity> storedSeverity = new SimpleObjectProperty<>(this, "storedSeverity", null);
    /**
     * Snapshot value set either when user takes snapshot, or when snapshot data is loaded from remote service. Note that this
     * can be modified if user chooses to use a multiplier before triggering a restore operation, or if the value is
     * edited directly in the table view cell.
     */
    private final ObjectProperty<VType> snapshotVal = new SimpleObjectProperty<>(this, "snapshotValue", VNoData.INSTANCE);

    /**
     * Snapshot value as loaded from remote service. Unlike snapshotVal it is not subject to changes.
     */
    private final ObjectProperty<VType> storedSnapshotValue = new SimpleObjectProperty<>(VNoData.INSTANCE);


    private final ObjectProperty<VTypePair> value = new SimpleObjectProperty<>(this, "value",
            new VTypePair(VDisconnectedData.INSTANCE, VDisconnectedData.INSTANCE, Optional.empty()));

    private final ObjectProperty<VType> liveValue = new SimpleObjectProperty<>(this, "liveValue", VDisconnectedData.INSTANCE);
    private final List<ObjectProperty<VTypePair>> compareValues = new ArrayList<>();

    private final ObjectProperty<VTypePair> liveReadback = new SimpleObjectProperty<>(this, "liveReadback",
            new VTypePair(VDisconnectedData.INSTANCE, VDisconnectedData.INSTANCE, Optional.empty()));

    private final StringProperty readbackName = new SimpleStringProperty(this, "readbackName");
    private final BooleanProperty liveStoredEqual = new SingleListenerBooleanProperty(this, "liveStoredEqual", true);

    private final ObjectProperty<VTypePair> storedReadback = new SimpleObjectProperty<>(this, "storedReadback",
            new VTypePair(VDisconnectedData.INSTANCE, VDisconnectedData.INSTANCE, Optional.empty()));

    private final List<ObjectProperty<VTypePair>> compareStoredReadbacks = new ArrayList<>();
    private Optional<Threshold<?>> threshold = Optional.empty();
    private final BooleanProperty readOnly = new SimpleBooleanProperty(this, "readOnly", false);

    /**
     * Holds result of a take snapshot for the read-back PV. Unlike the <code>actionResult</code> field this only
     * applies to tale snapshot action as read-back PVs are not subject to restore.
     */
    private final ObjectProperty<ActionResult> actionResultReadback = new SimpleObjectProperty<>(this, "actionResultReadback", ActionResult.PENDING);


    private ConfigPv configPv;

    /**
     * Construct a new table entry.
     */
    public TableEntry() {
        //when read only is set to true, unselect this PV
        readOnly.addListener((a, o, n) -> {
            if (n) {
                selected.set(false);
            }
        });
        //when selected, check if readonly is also selected and if yes unselect this pv
        selected.forceAddListener((a, o, n) -> {
            if (n && readOnly.get()) {
                selected.set(false);
            }
        });
    }

    public void setConfigPv(ConfigPv configPv) {
        this.configPv = configPv;
        pvName.setValue(configPv.getPvName());
        readbackName.setValue(configPv.getReadbackPvName());
    }

    public ConfigPv getConfigPv() {
        return configPv;
    }

    /**
     * Returns the property that describes whether the live and stored values are identical. This property can only have
     * one listener.
     *
     * @return the property describing if live and stored value are identical (in value terms only)
     */
    public ReadOnlyBooleanProperty liveStoredEqualProperty() {
        return liveStoredEqual;
    }

    /**
     * Returns the property that describes whether the property is selected or not. This property can only have one
     * listener.
     *
     * @return the property describing if the entry is selected or not
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * @return the property providing the pv name
     */
    public StringProperty pvNameProperty() {
        return pvName;
    }

    /**
     * @return the property providing the liveReadback pv name
     */
    public StringProperty readbackNameProperty() {
        return readbackName;
    }

    /**
     * @return the property providing the liveReadback value
     */
    public ObjectProperty<VTypePair> liveReadbackProperty() {
        return liveReadback;
    }

    /**
     * @return the property providing the alarm status of the PV value
     */
    @SuppressWarnings("unused")
    public StringProperty liveStatusProperty() {
        return liveStatus;
    }

    /**
     * @return the property providing the alarm severity of the PV value
     */
    @SuppressWarnings("unused")
    public ObjectProperty<AlarmSeverity> liveSeverityProperty() {
        return liveSeverity;
    }

    /**
     * @return the property providing the value of the primary snapshot value
     */
    public ObjectProperty<VType> snapshotValProperty() {
        return snapshotVal;
    }

    /**
     * @return the property providing the value of the primary snapshot value
     */
    public ObjectProperty<VTypePair> valueProperty() {
        return value;
    }

    /**
     * @return the property providing the unique (incremental id) used for sorting the entries
     */
    public IntegerProperty idProperty() {
        return id;
    }

    /**
     * @return the property providing the live PV value
     */
    public ObjectProperty<VType> liveValueProperty() {
        return liveValue;
    }

    /**
     * @return the property providing the stored liveReadback vs stored setpoint value
     */
    @SuppressWarnings("unused")
    public ObjectProperty<VTypePair> storedReadbackProperty() {
        return storedReadback;
    }

    @SuppressWarnings("unused")
    public StringProperty storedStatusProperty() {
        return storedStatus;
    }

    /**
     * @return the property indicating the PV is read only or read and write
     */
    public BooleanProperty readOnlyProperty() {
        return readOnly;
    }

    public Instant getTimestamp() {
        return timestamp.get();
    }

    public ObjectProperty<Instant> timestampProperty() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp.set(timestamp);
    }

    public ObjectProperty<VType> storedSnapshotValue() {
        return storedSnapshotValue;
    }

    @SuppressWarnings("unused")
    public ObjectProperty<AlarmSeverity> storedSeverityProperty() {
        return storedSeverity;
    }

    /**
     * @param index the index of the compared value (starts with 1)
     * @return the property providing the compared value for the given index
     */
    public ObjectProperty<VTypePair> compareValueProperty(int index) {
        if (index == 0) {
            throw new IndexOutOfBoundsException("Index has to be larger than 0.");
        } else {
            return compareValues.get(index - 1);
        }
    }

    /**
     * @param index the index of the compared value (starts with 1)
     * @return the property providing the compares stored liveReadback value for the given index
     */
    public ObjectProperty<VTypePair> compareStoredReadbackProperty(int index) {
        if (index == 0) {
            throw new IndexOutOfBoundsException("Index has to be larger than 0.");
        } else {
            return compareStoredReadbacks.get(index - 1);
        }
    }

    /**
     * Updates the snapshot value for the primary snapshot (index = 0) or for the snapshot compared to the primary
     * (index > 0).
     *
     * @param snapshotValue the value to set
     * @param index         the index of the snapshot to which the value belongs
     */
    public void setSnapshotValue(VType snapshotValue, int index) {
        final VType val = snapshotValue == null ? VDisconnectedData.INSTANCE : snapshotValue;
        if (index == 0) {
            if (val instanceof VNumber) {
                storedStatus.set(((VNumber) val).getAlarm().getStatus().name());
                storedSeverity.set(((VNumber) val).getAlarm().getSeverity());
                timestamp.set(((VNumber) val).getTime().getTimestamp());
            } else if (val instanceof VNumberArray) {
                storedStatus.set(((VNumberArray) val).getAlarm().getStatus().name());
                storedSeverity.set(((VNumberArray) val).getAlarm().getSeverity());
                timestamp.set(((VNumberArray) val).getTime().getTimestamp());
            } else if (val instanceof VEnum) {
                storedStatus.set(((VEnum) val).getAlarm().getStatus().name());
                storedSeverity.set(((VEnum) val).getAlarm().getSeverity());
                timestamp.set(((VEnum) val).getTime().getTimestamp());
            } else if (val instanceof VEnumArray) {
                storedStatus.set(((VEnumArray) val).getAlarm().getStatus().name());
                storedSeverity.set(((VEnumArray) val).getAlarm().getSeverity());
                timestamp.set(((VEnumArray) val).getTime().getTimestamp());
            } else if (val instanceof VNoData) {
                storedStatus.set("---");
                storedSeverity.set(null);
                timestamp.set(null);
            } else {
                storedStatus.set(AlarmSeverity.NONE.toString());
                storedSeverity.set(null);
                timestamp.set(null);
            }
            snapshotVal.set(val);
            storedSnapshotValue.set(val);
            value.set(new VTypePair(liveValue.get(), val, threshold));
            compareValues.forEach(o -> o.set(new VTypePair(val, o.get().value, threshold)));
            liveStoredEqual.set(Utilities.areValuesEqual(liveValue.get(), val, threshold));
            storedReadback.set(new VTypePair(val, storedReadback.get().value, threshold));
        } else {
            for (int i = compareValues.size(); i < index; i++) {
                compareValues.add(new SimpleObjectProperty<>(this, "CompareValue" + i,
                        new VTypePair(VDisconnectedData.INSTANCE, VDisconnectedData.INSTANCE, threshold)));
                compareStoredReadbacks.add(new SimpleObjectProperty<>(this, "CompareStoredReadback" + i,
                        new VTypePair(VDisconnectedData.INSTANCE, VDisconnectedData.INSTANCE, threshold)));
            }
            compareValues.get(index - 1).set(new VTypePair(valueProperty().get().value, val, threshold));
            compareStoredReadbacks.get(index - 1)
                    .set(new VTypePair(val, compareStoredReadbacks.get(index - 1).get().value, threshold));
        }
    }

    /**
     * Set the stored readback value for the primary snapshot of for the snapshots compared to the primary one.
     *
     * @param val the value to set
     */
    public void setStoredReadbackValue(VType val, int index) {
        if (val == null) {
            val = VNoData.INSTANCE;
        }
        if (index == 0) {
            storedReadback.set(new VTypePair(storedReadback.get().base, val, threshold));
        } else {
            for (int i = compareValues.size(); i < index; i++) {
                compareStoredReadbacks.add(new SimpleObjectProperty<>(this, "CompareStoredReadback" + i,
                        new VTypePair(VDisconnectedData.INSTANCE, VDisconnectedData.INSTANCE, threshold)));
            }
            compareStoredReadbacks.get(index - 1)
                    .set(new VTypePair(compareStoredReadbacks.get(index - 1).get().base, val, threshold));
        }
    }

    /**
     * Set the liveReadback value of this entry.
     *
     * @param val the value
     */
    public void setReadbackValue(VType val) {
        if (val == null) {
            val = VDisconnectedData.INSTANCE;
        }
        if (liveReadback.get().value != val) {
            VTypePair vTypePair = new VTypePair(liveValueProperty().get(), val, threshold);
            liveReadback.set(vTypePair);
        }
    }

    /**
     * Set the live value of this entry.
     *
     * @param val the new value
     */
    public void setLiveValue(VType val) {
        if (val == null) {
            val = VDisconnectedData.INSTANCE;
        }
        liveValue.set(val);
        liveReadback.set(new VTypePair(val, liveReadback.get().value, threshold));
        VType stored = value.get().value;
        value.set(new VTypePair(val, stored, threshold));
        liveStoredEqual.set(Utilities.areValuesEqual(val, stored, threshold));
        if (val instanceof VNumber) {
            liveStatus.set(((VNumber) val).getAlarm().getStatus().name());
            liveSeverity.set(((VNumber) val).getAlarm().getSeverity());
            timestamp.set(((VNumber) val).getTime().getTimestamp());
        } else if (val instanceof VNumberArray) {
            liveStatus.set(((VNumberArray) val).getAlarm().getStatus().name());
            liveSeverity.set(((VNumberArray) val).getAlarm().getSeverity());
            timestamp.set(((VNumberArray) val).getTime().getTimestamp());
        } else if (val instanceof VEnum) {
            liveStatus.set(((VEnum) val).getAlarm().getStatus().name());
            liveSeverity.set(((VEnum) val).getAlarm().getSeverity());
            timestamp.set(((VEnum) val).getTime().getTimestamp());
        } else if (val instanceof VEnumArray) {
            liveStatus.set(((VEnumArray) val).getAlarm().getStatus().name());
            liveSeverity.set(((VEnumArray) val).getAlarm().getSeverity());
            timestamp.set(((VEnumArray) val).getTime().getTimestamp());
        } else if (val instanceof VDisconnectedData) {
            liveSeverity.set(AlarmSeverity.UNDEFINED);
            liveStatus.set("---");
            timestamp.set(null);
        } else {
            liveSeverity.set(null);
            liveStatus.set("---");
            timestamp.set(null);
        }
    }

    /**
     * Set the threshold value for this entry. All value comparisons related to this entry are calculated using the
     * threshold (if it exists).
     *
     * @param threshold the threshold
     */
    public void setThreshold(Optional<Threshold<?>> threshold) {
        if (threshold.isPresent()) {
            this.threshold = threshold;
            VType val = this.value.get().value;
            this.value.set(new VTypePair(this.value.get().base, val, threshold));
            this.liveStoredEqual.set(Utilities.areValuesEqual(liveValue.get(), val, threshold));
            this.compareValues.forEach(e -> e.set(new VTypePair(val, e.get().value, threshold)));
            this.liveReadback.set(new VTypePair(this.liveReadback.get().base, this.liveReadback.get().value, threshold));
            this.storedReadback
                    .set(new VTypePair(this.storedReadback.get().base, this.storedReadback.get().value, threshold));
            this.compareStoredReadbacks.forEach(e -> e.set(new VTypePair(e.get().base, e.get().value, threshold)));
        }
    }

    public ObjectProperty<VType> getSnapshotVal() {
        return snapshotVal;
    }

    public ObjectProperty<ActionResult> actionResultProperty(){
        return actionResult;
    }

    public void setActionResult(ActionResult actionResult){
        this.actionResult.set(actionResult);
    }

    public ObjectProperty<ActionResult> actionResultReadbackProperty(){
        return actionResultReadback;
    }

    public void setActionResultReadback(ActionResult actionResult){
        this.actionResultReadback.set(actionResult);
    }


}
