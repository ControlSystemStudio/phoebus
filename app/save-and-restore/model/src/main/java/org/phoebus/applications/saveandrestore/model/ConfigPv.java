/** 
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.model;

import java.util.Objects;

/**
 * Class encapsulating data to describe a PV subject to a save operation. A read-back PV name
 * is optionally associated with the PV name. A PV record is uniquely identified by both the PV name
 * and the read-back PV name (if it has been specified).
 * <p>
 *     If a read-back PV name has been specified, a {@link Comparison} can be specified to
 *     indicate if and how the stored read-back value is compared to the live read-back value
 *     if a client requests it.
 * </p>
 * @author georgweiss
 * Created 1 Oct 2018
 */
public class ConfigPv implements Comparable<ConfigPv>{

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

	/**
	 * Optional comparison data for the read-back PV.
	 */
	private Comparison comparison;

	public String getPvName() {
		return pvName;
	}

	public void setPvName(String pvName) {
		this.pvName = pvName;
	}

	public String getReadbackPvName() {
		return readbackPvName;
	}

	public void setReadbackPvName(String readbackPvName) {
		this.readbackPvName = readbackPvName;
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
		if(other instanceof ConfigPv) {
			ConfigPv otherConfigPv = (ConfigPv)other;
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
		
		return new StringBuffer()
				.append("PV name=").append(pvName)
				.append(", readback PV name=").append(readbackPvName)
				.append(", readOnly=").append(readOnly)
				.toString();
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

	public static Builder builder(){
		return new Builder();
	}

	public static class Builder{

		private ConfigPv configPv;

		private Builder(){
			configPv = new ConfigPv();
		}

		public Builder pvName(String pvName){
			configPv.setPvName(pvName);
			return this;
		}

		public Builder readbackPvName(String readbackPvName){
			configPv.setReadbackPvName(readbackPvName);
			return this;
		}

		public Builder readOnly(boolean readOnly){
			configPv.setReadOnly(readOnly);
			return this;
		}

		public Builder comparison(Comparison comparison){
			configPv.setComparison(comparison);
			return this;
		}

		public ConfigPv build(){
			return configPv;
		}
	}
}
