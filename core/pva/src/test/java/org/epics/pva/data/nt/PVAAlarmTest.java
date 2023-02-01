/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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
 *
 */
package org.epics.pva.data.nt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.junit.jupiter.api.Test;

public class PVAAlarmTest {

    @Test
    void testConstructor() {
        PVAAlarm alarm = new PVAAlarm(PVAAlarm.AlarmSeverity.MAJOR, PVAAlarm.AlarmStatus.DRIVER, "alarmMessage");
        assertEquals(new PVAInt("severity", 2), alarm.get("severity"));
        assertEquals(new PVAInt("status", 2), alarm.get("status"));
        assertEquals(new PVAString("message", "alarmMessage"), alarm.get("message"));
    }

    @Test
    public void testSet() {
        PVAAlarm alarm = new PVAAlarm(PVAAlarm.AlarmSeverity.MAJOR, PVAAlarm.AlarmStatus.DRIVER, "test message");

        PVAStructure clone = alarm.cloneData();

        assertEquals(alarm, clone);

        alarm.set(PVAAlarm.AlarmSeverity.NO_ALARM, PVAAlarm.AlarmStatus.NO_STATUS, "test message 2");
        assertNotEquals(alarm, clone);
    }

    @Test
    public void testFromStructure() {
        PVAStructure structure = new PVAStructure("alarm", "structure",
                new PVAInt("severity", 0), new PVAInt("status", 0));

        PVAStructure alarm = new PVAAlarm(PVAAlarm.AlarmSeverity.NO_ALARM, PVAAlarm.AlarmStatus.NO_STATUS, "");

        assertEquals(alarm, PVAAlarm.fromStructure(alarm));
    }


}
