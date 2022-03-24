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

package org.phoebus.service.saveandrestore.model.internal;

import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.service.saveandrestore.persistence.dao.SnapshotPvDataType;

/**
 * Class encapsulating data associated with a PV. 
 * @author georgweiss
 * Created 14 Nov 2018
 */
public class SnapshotPv {

	private int snapshotId;
	private AlarmSeverity alarmSeverity;
	private AlarmStatus alarmStatus;
	private String alarmName = AlarmStatus.NONE.toString();
	private long time;
	private int timens;
	private String value;
	private ConfigPv configPv;
	private SnapshotPvDataType dataType;
	private String sizes;

	public int getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(int snapshotId) {
		this.snapshotId = snapshotId;
	}

	public AlarmSeverity getAlarmSeverity() {
		return alarmSeverity;
	}

	public void setAlarmSeverity(AlarmSeverity alarmSeverity) {
		this.alarmSeverity = alarmSeverity;
	}

	public AlarmStatus getAlarmStatus() {
		return alarmStatus;
	}

	public void setAlarmStatus(AlarmStatus alarmStatus) {
		this.alarmStatus = alarmStatus;
	}

	public String getAlarmName() {
		return alarmName;
	}

	public void setAlarmName(String alarmName) {
		this.alarmName = alarmName;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public int getTimens() {
		return timens;
	}

	public void setTimens(int timens) {
		this.timens = timens;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ConfigPv getConfigPv() {
		return configPv;
	}

	public void setConfigPv(ConfigPv configPv) {
		this.configPv = configPv;
	}

	public SnapshotPvDataType getDataType() {
		return dataType;
	}

	public void setDataType(SnapshotPvDataType dataType) {
		this.dataType = dataType;
	}

	public String getSizes() {
		return sizes;
	}

	public void setSizes(String sizes) {
		this.sizes = sizes;
	}

	public static Builder builder(){
		return new Builder();
	}

	public static class Builder{

		private SnapshotPv snapshotPv;

		private Builder(){
			snapshotPv = new SnapshotPv();
			snapshotPv.setAlarmName(AlarmStatus.NONE.toString());
		}

		public Builder snapshotId(int snapshotId){
			snapshotPv.setSnapshotId(snapshotId);
			return this;
		}

		public Builder alarmSeverity(AlarmSeverity alarmSeverity){
			snapshotPv.setAlarmSeverity(alarmSeverity);
			return this;
		}

		public Builder alarmStatus(AlarmStatus alarmStatus){
			snapshotPv.setAlarmStatus(alarmStatus);
			return this;
		}

		public Builder alarmName(String alarmName){
			snapshotPv.setAlarmName(alarmName);
			return this;
		}

		public Builder time(long time){
			snapshotPv.setTime(time);
			return this;
		}

		public Builder timens(int timens){
			snapshotPv.setTimens(timens);
			return this;
		}

		public Builder value(String value){
			snapshotPv.setValue(value);
			return this;
		}

		public Builder configPv(ConfigPv configPv){
			snapshotPv.setConfigPv(configPv);
			return this;
		}

		public Builder dataType(SnapshotPvDataType dataType){
			snapshotPv.setDataType(dataType);
			return this;
		}

		public Builder sizes(String sizes){
			snapshotPv.setSizes(sizes);
			return this;
		}

		public SnapshotPv build(){
			return snapshotPv;
		}
	}
}
