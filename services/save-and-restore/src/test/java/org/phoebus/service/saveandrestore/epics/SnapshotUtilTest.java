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

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VFloat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SnapshotUtilTestConfig.class)
public class SnapshotUtilTest {

    @Autowired
    private SnapshotUtil snapshotUtil;

    @Test
    public void testRestorePVValues() throws Exception {
        PV pv = PVPool.getPV("loc://x(42.0)");
        var configPv = new ConfigPv();
        configPv.setPvName("loc://x");

        var testSnapshotItem = new SnapshotItem();
        testSnapshotItem.setConfigPv(configPv);
        testSnapshotItem.setValue(VFloat.of(1.0, Alarm.noValue(), Time.now(), Display.none()));
        snapshotUtil.restore(List.of(testSnapshotItem));
        var pvValue = pv.asyncRead().get();
        Assertions.assertEquals(VTypeHelper.toObject(pvValue), 1.0);
    }

    @Test
    public void testCannotConnectPV() {
        var configPv = new ConfigPv();
        configPv.setPvName("pva://x");

        var testSnapshotItem = new SnapshotItem();
        testSnapshotItem.setConfigPv(configPv);
        testSnapshotItem.setValue(VFloat.of(1.0, Alarm.noValue(), Time.now(), Display.none()));
        var result = snapshotUtil.restore(
                List.of(testSnapshotItem));
        Assertions.assertNotNull(result.get(0).getErrorMsg());
    }

    @Test
    public void testTakeSnapshot(){
         ConfigurationData configurationData = new ConfigurationData();
        ConfigPv configPv = new ConfigPv();
        configPv.setPvName("loc://x(42.0)");
        configPv.setReadbackPvName("loc://y(777.0)");
        ConfigPv configPv2 = new ConfigPv();
        configPv2.setPvName("loc://xx(44.0)");
        configurationData.setPvList(List.of(configPv, configPv2));

        List<SnapshotItem> snapshotItems = snapshotUtil.takeSnapshot(configurationData);
        Assertions.assertEquals(42.0, VTypeHelper.toDouble(snapshotItems.get(0).getValue()));
        Assertions.assertEquals(777.0, VTypeHelper.toDouble(snapshotItems.get(0).getReadbackValue()));
        Assertions.assertEquals(44.0, VTypeHelper.toDouble(snapshotItems.get(1).getValue()));
        Assertions.assertNull(snapshotItems.get(1).getReadbackValue());


    }
}
