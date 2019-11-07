/*
 * *
 *  * Copyright (C) 2019 European Spallation Source ERIC.
 *  * <p>
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  * <p>
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * <p>
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.applications.saveandrestore.datamigration.git;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * <code>SaveSetEntry</code> represents a single entry in the save set. It provides all properties of a single PV that
 * are stored in the save set file: the name of the PV, the name of the readback pv, delta used for comparison of
 * values, and the flag indicating if the PV is readonly or readable and writable.
 *
 * @see org.phoebus.applications.saveandrestore.ui.model.Threshold
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class SaveSetEntry implements Serializable {

    private static final long serialVersionUID = -4899009360011799916L;

    private final String pvName;
    private final String readback;
    private final String delta;
    private final boolean readOnly;

    /**
     * Constructs a new entry from the pv name and default values (empty readback and delta, writable PV).
     *
     * @param pvName the name of the pv
     */
    public SaveSetEntry(String pvName) {
        this(pvName, null, null, false);
    }

    /**
     * Constructs a new entry.
     *
     * @param pvName the pv name
     * @param readback the readback pv name
     * @param delta the delta string
     * @param readOnly true if read only and false if read write
     */
    public SaveSetEntry(String pvName, String readback, String delta, boolean readOnly) {
        if (pvName == null) {
            throw new IllegalArgumentException("The PV name of a save set entry cannot be null.");
        }
        this.pvName = pvName;
        this.readback = readback == null ? "" : readback;
        this.delta = delta == null ? "" : delta;
        this.readOnly = readOnly;
    }

    /**
     * Returns the name of the PV. This value can never be null
     *
     * @return the name of the PV
     */
    public String getPVName() {
        return pvName;
    }

    /**
     * Returns the name of the readback PV. If the PV does not have a readback, the method should return an empty
     * string.
     *
     * @return the name of the readback PV
     */
    public String getReadback() {
        return readback;
    }

    /**
     * Returns the deltas value, which define how to treat the difference between values. The deltas are later
     * transformed to {@link org.phoebus.applications.saveandrestore.ui.model.Threshold}s, which evaluate the difference between the values. In general if two values
     * differ less than delta, they are considered equal or at least non-critically different. The delta can be a
     * number, or a function. If there is no known delta for a PV the entry should be an empty string.
     *
     * @return the delta string
     */
    public String getDelta() {
        return delta;
    }

    /**
     * Returns the read only flag. The flag define whether the PV is a read only PV or a read and write PV. Read only
     * PVs have the read only flag set to true, read and write PVs have the flag set to false. Read only PVs cannot be
     * restored.
     *
     * @return true if read only or false if read and write
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(SaveSetEntry.class, pvName, delta, readback, readOnly);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SaveSetEntry other = (SaveSetEntry) obj;
        return Objects.equals(delta, other.delta) && Objects.equals(pvName, other.pvName)
            && Objects.equals(readback, other.readback) && readOnly == other.readOnly;
    }

    /**
     * Returns the string with which this save set is represented in the file.
     *
     * @return the string representing this save set
     */
    public String getSaveString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append(pvName).append(',').append(readback).append(',');
        if (delta.indexOf(',') > -1) {
            sb.append('"').append(delta).append('"');
        } else {
            sb.append(delta);
        }
        sb.append(',').append(readOnly);
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getSaveString();
    }

}
