/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.common.VNoData;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;

import java.io.Serializable;
import java.util.Objects;


/**
 * <code>SnapshotEntry</code> wraps an {@link SnapshotItem} (the data object stored in the
 * remote service through {@link org.phoebus.applications.saveandrestore.model.SnapshotData}) and
 * a few additional fields.
 */
public class SnapshotEntry {

    private static final long serialVersionUID = 5181175467248870613L;
    private boolean selected;
    private SnapshotItem snapshotItem;

    public SnapshotEntry(SnapshotItem snapshotItem, boolean selected){
        this.snapshotItem = snapshotItem;
        this.selected = selected;
    }

    public ConfigPv getConfigPv() {
        return snapshotItem.getConfigPv();
    }

    /**
     * Returns PV value (value, timestamp, alarm stuff).
     *
     * @return the stored pv value
     */
    public VType getValue() {
        return snapshotItem.getValue();
    }

    /**
     * Returns the name of the PV.
     *
     * @return the PV name
     */
    public String getPVName() {
        return snapshotItem.getConfigPv().getPvName();
    }


    /**
     * Returns the name of the readback PV associated with the setpoint represented by this entry.
     *
     * @return the readback name
     */
    public String getReadbackName() {
        return snapshotItem.getConfigPv().getReadbackPvName();
    }

    /**
     * Returns the readback PV value as it was at the time when the snapshot was taken.
     *
     * @return the readback pv value
     */
    public VType getReadbackValue() {
        return snapshotItem.getReadbackValue() == null ? VNoData.INSTANCE : snapshotItem.getReadbackValue();
    }

    /**
     * Returns true if this entry represents a read only PV or false if the PV can be read and written to. Read only PVs
     * cannot be restored.
     *
     * @return true if read only or false if not
     */
    public boolean isReadOnly() {
        return snapshotItem.getConfigPv().isReadOnly();
    }

    /**
     * Returns true if this entry was selected for restoring in the GUI or false otherwise.
     *
     * @return true if selected or false if not selected
     */
    public boolean isSelected() {
        System.out.println("****************** Querying isSelected **********************");
        return selected;
    }

    /**
     * Sets the value and the selected state of this entry. If the value is null {@link VNoData#INSTANCE} is set.
     *
     * @param value    the value to set
     * @param selected true if selected or false otherwise
     */
    public void set(VType value, boolean selected) {
        snapshotItem.setValue(value == null ? VNoData.INSTANCE : value);
        this.selected = selected;
    }

    public SnapshotItem getSnapshotItem(){
        return snapshotItem;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(SnapshotEntry.class,
                snapshotItem.getConfigPv().getPvName(),
                selected,
                snapshotItem.getConfigPv().isReadOnly(),
                snapshotItem.getConfigPv().getReadbackPvName(),
                snapshotItem.getReadbackValue(),
                snapshotItem.getValue());
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
        if (!(Objects.equals(snapshotItem.getConfigPv().getPvName(),
                other.snapshotItem.getConfigPv().getPvName()) &&
                selected == other.selected &&
                snapshotItem.getConfigPv().isReadOnly() == other.getConfigPv().isReadOnly() &&
                Objects.equals(snapshotItem.getConfigPv().getReadbackPvName(), other.getConfigPv().getReadbackPvName()))) {
            return false;
        }
        if (!Utilities.areVTypesIdentical(snapshotItem.getValue(), other.getSnapshotItem().getValue(), true)) {
            return false;
        }
        if (!Utilities.areVTypesIdentical(snapshotItem.getReadbackValue(), other.getSnapshotItem().getReadbackValue(), false)) {
            return false;
        }
        return true;
    }
}
