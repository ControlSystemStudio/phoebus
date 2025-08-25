/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Class encapsulating data to describe a PV subject to a save operation. A read-back PV name
 * is optionally associated with the PV name. A PV record is uniquely identified by both the PV name
 * and the read-back PV name (if it has been specified).
 * <p>
 *     A {@link ComparisonMode} can optionally be specified to
 *     indicate how the stored value (as defined by the {@link #pvName} field) is compared to a live value
 *     if a client requests it. A non-null {@link ComparisonMode} must be paired non-null and a
 *     {@link #tolerance} value &ge;0.
 * </p>
 * @author georgweiss
 * Created 1 Oct 2018
 */
public class ConfigPv implements Comparable<ConfigPv> {

    /**
     * Set-point PV name.
     */
    private String pvName;

    /**
     * Optional read-back PV name
     */
    private String readbackPvName;

    /**
     * Flag indicating if set-point PV value should be restored or not.
     */
    private boolean readOnly = false;

    private Comparison comparison;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double tolerance = null;

    public String getPvName() {
        return pvName;
    }

    public void setPvName(String pvName) {
        this.pvName = pvName;
    }

    public void setReadbackPvName(String readbackPvName) {
        this.readbackPvName = readbackPvName;
    }

    public String getReadbackPvName(){
        return readbackPvName;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Comparison getComparison() {
        return comparison;
    }

    public void setComparison(Comparison comparison) {
        this.comparison = comparison;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ConfigPv otherConfigPv) {
            return Objects.equals(pvName, otherConfigPv.getPvName()) && Objects.equals(readbackPvName, otherConfigPv.getReadbackPvName()) && Objects.equals(readOnly, otherConfigPv.isReadOnly());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pvName, readbackPvName, readOnly);
    }

    @Override
    public String toString() {

        StringBuilder stringBuffer = new StringBuilder()
                .append("PV name=").append(pvName)
                .append(", readback PV name=").append(readbackPvName)
                .append(", readOnly=").append(readOnly)
                .append(", tolerance=").append(tolerance);
        if(comparison != null){
            stringBuffer.append(", comparison mode=").append(comparison.getComparisonMode());
            stringBuffer.append(", tolerance=").append(comparison.getTolerance());
        }
        return stringBuffer.toString();
    }

    /**
     * Comparison is simply a comparison of the {@code pvName} field.
     * @param other The object to compare to.
     * @return The comparison result, typically used to sort list of {@link ConfigPv}s by name
     */
    @Override
    public int compareTo(ConfigPv other) {
        return pvName.compareTo(other.getPvName());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ConfigPv configPv;

        private Builder() {
            configPv = new ConfigPv();
        }

        public Builder pvName(String pvName) {
            configPv.setPvName(pvName);
            return this;
        }

        public Builder readbackPvName(String readbackPvName) {
            configPv.setReadbackPvName(readbackPvName);
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            configPv.setReadOnly(readOnly);
            return this;
        }

        public Builder comparison(Comparison comparison) {
            configPv.setComparison(comparison);
            return this;
        }

        public ConfigPv build() {
            return configPv;
        }
    }
}
