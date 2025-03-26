/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.saveandrestore.util;

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VFloat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.CompareResult;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SnapshotUtilTest {

    private static SnapshotUtil snapshotUtil;

    @BeforeAll
    public static void setup(){
        snapshotUtil = new SnapshotUtil();
    }

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
        Assertions.assertEquals(1.0, VTypeHelper.toObject(pvValue));
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
        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://x(42.0)");
        configPv1.setReadbackPvName("loc://y(777.0)");
        ConfigPv configPv2 = new ConfigPv();
        configPv2.setPvName("loc://xx(44.0)");
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(configPv1, configPv2));

        List<SnapshotItem> snapshotItems = snapshotUtil.takeSnapshot(configurationData);
        assertEquals(2, snapshotItems.size());

        SnapshotItem snapshotItem0 = snapshotItems.get(0);
        if(snapshotItem0.getConfigPv().getPvName().equals("loc://x(42.0)")){
            assertEquals(42.0, VTypeHelper.toDouble(snapshotItem0.getValue()));
            assertEquals(777.0, VTypeHelper.toDouble(snapshotItem0.getReadbackValue()));
        }
        else{
            assertEquals(44.0, VTypeHelper.toDouble(snapshotItem0.getValue()));
            assertNull(snapshotItem0.getReadbackValue());
        }
    }

    @Test
    public void testComparePvs(){
        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://x(42.0)");
        ConfigPv configPv2 = new ConfigPv();
        configPv2.setPvName("loc://y(771.0)");

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(42.0, Alarm.none(), Time.now(), Display.none()));
        SnapshotItem snapshotItem2 = new SnapshotItem();
        snapshotItem2.setConfigPv(configPv2);
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(), Time.now(), Display.none()));

        List<CompareResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 0.0);

        assertTrue(compareResults.get(0).isEqual());
        assertTrue(compareResults.get(1).isEqual());

        snapshotItem1.setValue(VDouble.of(43.0, Alarm.none(), Time.now(), Display.none()));
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(), Time.now(), Display.none()));

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 0.0);

        assertFalse(compareResults.get(0).isEqual());
        assertNotNull(compareResults.get(0).getStoredValue());
        assertNotNull(compareResults.get(0).getLiveValue());
        assertTrue(compareResults.get(1).isEqual());
        assertNull(compareResults.get(1).getStoredValue());
        assertNull(compareResults.get(1).getLiveValue());

        snapshotItem1.setValue(VDouble.of(43.0, Alarm.none(), Time.now(), Display.none()));
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(), Time.now(), Display.none()));

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 10.0);

        assertTrue(compareResults.get(0).isEqual());
        assertTrue(compareResults.get(1).isEqual());

        assertThrows(RuntimeException.class, () -> snapshotUtil.comparePvs(null, -77));

    }
}
