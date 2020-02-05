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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.service.saveandrestore.persistence.dao.SnapshotPvDataType;

/**
 * Class encapsulating data associated with a PV. 
 * @author georgweiss
 * Created 14 Nov 2018
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotPv {

	private int snapshotId;
	private AlarmSeverity alarmSeverity;
	private AlarmStatus alarmStatus;
	@Builder.Default
	private String alarmName = AlarmStatus.NONE.toString();
	private long time;
	private int timens;
	private String value;
	private ConfigPv configPv;
	private SnapshotPvDataType dataType;
	private String sizes;
}
