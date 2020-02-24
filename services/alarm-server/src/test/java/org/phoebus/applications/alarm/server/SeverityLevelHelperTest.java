/*
 * Copyright (C) 2019 European Spallation Source ERIC.
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

package org.phoebus.applications.alarm.server;

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VInt;
import org.junit.Test;
import org.phoebus.applications.alarm.model.SeverityLevel;

import static org.junit.Assert.*;

public class SeverityLevelHelperTest {

    @Test
    public void decodeSeverity() {

        assertEquals(SeverityLevel.INVALID, SeverityLevelHelper.decodeSeverity(null));

        VInt intValue = VInt.of(7, Alarm.disconnected(), Time.now(), Display.none());
        SeverityLevel severityLevel = SeverityLevelHelper.decodeSeverity(intValue);
        assertEquals(SeverityLevel.INVALID, severityLevel);

        intValue = VInt.of(7, Alarm.none(), Time.now(), Display.none());
        severityLevel = SeverityLevelHelper.decodeSeverity(intValue);
        assertEquals(SeverityLevel.OK, severityLevel);

        intValue = VInt.of(7, Alarm.lolo(), Time.now(), Display.none());
        severityLevel = SeverityLevelHelper.decodeSeverity(intValue);
        assertEquals(SeverityLevel.MAJOR, severityLevel);

        intValue = VInt.of(7, Alarm.hihi(), Time.now(), Display.none());
        severityLevel = SeverityLevelHelper.decodeSeverity(intValue);
        assertEquals(SeverityLevel.MAJOR, severityLevel);

        intValue = VInt.of(7, Alarm.low(), Time.now(), Display.none());
        severityLevel = SeverityLevelHelper.decodeSeverity(intValue);
        assertEquals(SeverityLevel.MINOR, severityLevel);

        intValue = VInt.of(7, Alarm.high(), Time.now(), Display.none());
        severityLevel = SeverityLevelHelper.decodeSeverity(intValue);
        assertEquals(SeverityLevel.MINOR, severityLevel);

    }

    @Test
    public void getStatusMessage() {
        VInt intValue = VInt.of(7, Alarm.disconnected(), Time.now(), Display.none());
        String statusMessage = SeverityLevelHelper.getStatusMessage(intValue);
        assertEquals("Disconnected", statusMessage);

        intValue = VInt.of(7, Alarm.none(), Time.now(), Display.none());
        statusMessage = SeverityLevelHelper.getStatusMessage(intValue);
        assertEquals("None", statusMessage);

        intValue = VInt.of(7, Alarm.lolo(), Time.now(), Display.none());
        statusMessage = SeverityLevelHelper.getStatusMessage(intValue);
        assertEquals("LOLO", statusMessage);

        intValue = VInt.of(7, Alarm.hihi(), Time.now(), Display.none());
        statusMessage = SeverityLevelHelper.getStatusMessage(intValue);
        assertEquals("HIHI", statusMessage);

        intValue = VInt.of(7, Alarm.low(), Time.now(), Display.none());
        statusMessage = SeverityLevelHelper.getStatusMessage(intValue);
        assertEquals("LOW", statusMessage);

        intValue = VInt.of(7, Alarm.high(), Time.now(), Display.none());
        statusMessage = SeverityLevelHelper.getStatusMessage(intValue);
        assertEquals("HIGH", statusMessage);
    }
}