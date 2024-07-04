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

package org.phoebus.service.saveandrestore.epics;

import org.junit.jupiter.api.Assertions;

import java.util.Arrays;

import org.epics.vtype.Time;
import org.epics.vtype.VFloat;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SnapshotRestorerTestConfig.class)
public class SnapshotRestorerTest {

    @Autowired
    private SnapshotRestorer snapshotRestorer;

    @Test
    public void testRestorePVValues() throws Exception {
        PV pv = PVPool.getPV("loc://x(42.0)");
        var configPv = new ConfigPv();
        configPv.setPvName("loc://x");

        var testSnapshotItem = new SnapshotItem();
        testSnapshotItem.setConfigPv(configPv);
        testSnapshotItem.setValue(VFloat.of(1.0, Alarm.noValue(), Time.now(), Display.none()));
        snapshotRestorer.restorePVValues(
                Arrays.asList(testSnapshotItem));
        var pvValue = pv.asyncRead().get();
        Assertions.assertEquals(VTypeHelper.toObject(pvValue), 1.0);
    }

    @Test
    public void testCannotConnectPV() throws Exception {
        var configPv = new ConfigPv();
        configPv.setPvName("pva://x");

        var testSnapshotItem = new SnapshotItem();
        testSnapshotItem.setConfigPv(configPv);
        testSnapshotItem.setValue(VFloat.of(1.0, Alarm.noValue(), Time.now(), Display.none()));
        var result = snapshotRestorer.restorePVValues(
                Arrays.asList(testSnapshotItem));
        Assertions.assertNotNull(result.get(0).getErrorMsg());
    }
}
