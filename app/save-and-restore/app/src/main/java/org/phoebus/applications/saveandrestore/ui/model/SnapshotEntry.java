package org.phoebus.applications.saveandrestore.ui.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Utilities;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;

import java.io.Serializable;
import java.util.Objects;


/**
 * <code>SnapshotEntry</code> represents a single entry in the snapshot. It contains fields for all parameters that are
 * store in the snapshot file. All parameters except <code>value</code> and <code>selected</code> are fixed and cannot
 * be changed.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class SnapshotEntry implements Serializable {

        private static final long serialVersionUID = 5181175467248870613L;
        private final ConfigPv configPv;
        private transient ObjectProperty<VType> valueProperty = new SimpleObjectProperty<>();
        private final transient VType storedValue;
        private boolean selected;
        private final String readbackName;
        private final transient VType readbackValue;
        private final String delta;
        private final boolean readOnly;

        public SnapshotEntry(SnapshotItem snapshotItem, boolean selected){
            this(snapshotItem.getConfigPv(),
                    snapshotItem.getValue(),
                    selected,
                    snapshotItem.getConfigPv().getReadbackPvName(),
                    snapshotItem.getReadbackValue(),
                    null,
                    snapshotItem.getConfigPv().isReadOnly());
        }


        /**
         * Constructs a new entry from pieces.
         *
         */
        public SnapshotEntry(ConfigPv configPv, VType value, boolean selected, String readbackName, VType readbackValue,
                             String delta, boolean readOnly) {
            this.valueProperty.set(value == null ? VNoData.INSTANCE : value);
            this.storedValue = value;
            this.configPv = configPv;
            this.selected = selected;
            this.readbackName = readbackName == null ? "" : readbackName;
            this.readbackValue = readbackValue == null ? VNoData.INSTANCE : readbackValue;
            this.delta = delta == null ? "" : delta;
            this.readOnly = readOnly;
        }

        public ConfigPv getConfigPv(){
            return configPv;
        }

        /**
         * Returns PV value (value, timestamp, alarm stuff).
         *
         * @return the stored pv value
         */
        public VType getValue() {
            return valueProperty.get();
        }

        /**
         * Returns PV value property
         *
         * @return the stored PV value property
         */

        public ObjectProperty<VType> getValueProperty() {
            return valueProperty;
        }

        /**
         * Returns stored PV value.
         * Initialized once when object creation and remain unchanged.
         *
         * @return an immutable stored PV value
         */
        public VType getStoredValue() { return storedValue; }

        /**
         * Returns the name of the PV.
         *
         * @return the PV name
         */
        public String getPVName() {
            return configPv.getPvName();
        }

        /**
         * Returns the delta used for validating the value of this PV.
         *
         * @see Threshold
         * @return the delta
         */
        public String getDelta() {
            return delta;
        }

        /**
         * Returns the name of the readback PV associated with the setpoint represented by this entry.
         *
         * @return the readback name
         */
        public String getReadbackName() {
            return readbackName;
        }

        /**
         * Returns the readback PV value as it was at the time when the snapshot was taken.
         *
         * @return the readback pv value
         */
        public VType getReadbackValue() {
            return readbackValue;
        }

        /**
         * Returns true if this entry represents a read only PV or false if the PV can be read and written to. Read only PVs
         * cannot be restored.
         *
         * @return true if read only or false if not
         */
        public boolean isReadOnly() {
            return readOnly;
        }

        /**
         * Returns true if this entry was selected for restoring in the GUI or false otherwise.
         *
         * @return true if selected or false if not selected
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Sets the value and the selected state of this entry. If the value is null {@link VNoData#INSTANCE} is set.
         *
         * @param value the value to set
         * @param selected true if selected or false otherwise
         */
        public void set(VType value, boolean selected) {
            this.valueProperty.set(value == null ? VNoData.INSTANCE : value);
            this.selected = selected;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return Objects.hash(SnapshotEntry.class, configPv.getPvName(), selected, readOnly, readbackName, readbackValue, delta, valueProperty.get(),
                    readbackValue);
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SnapshotEntry other = (SnapshotEntry) obj;
            if (!(Objects.equals(configPv.getPvName(), other.configPv.getPvName()) && selected == other.selected && readOnly == other.readOnly
                    && Objects.equals(readbackName, other.readbackName) && Objects.equals(delta, other.delta))) {
                return false;
            }
            if (!Utilities.areVTypesIdentical(valueProperty.get(), other.valueProperty.get(), true)) {
                return false;
            }
            if (!Utilities.areVTypesIdentical(readbackValue, other.readbackValue, false)) {
                return false;
            }
            return true;
        }
}
