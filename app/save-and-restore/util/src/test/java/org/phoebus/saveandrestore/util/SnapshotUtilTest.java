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
import org.phoebus.applications.saveandrestore.model.Comparison;
import org.phoebus.applications.saveandrestore.model.ComparisonResult;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.ComparisonMode;
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

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 0.0, ComparisonMode.ABSOLUTE, false);

        assertTrue(compareResults.get(0).isEqual());
        assertTrue(compareResults.get(1).isEqual());

        snapshotItem1.setValue(VDouble.of(43.0, Alarm.none(), Time.now(), Display.none()));
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(), Time.now(), Display.none()));

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 0.0,  ComparisonMode.ABSOLUTE, false);

        assertFalse(compareResults.get(0).isEqual());
        assertNotNull(compareResults.get(0).getStoredValue());
        assertNotNull(compareResults.get(0).getLiveValue());
        assertTrue(compareResults.get(1).isEqual());
        assertNull(compareResults.get(1).getStoredValue());
        assertNull(compareResults.get(1).getLiveValue());

        snapshotItem1.setValue(VDouble.of(43.0, Alarm.none(), Time.now(), Display.none()));
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(), Time.now(), Display.none()));

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 10.0, ComparisonMode.ABSOLUTE, false);

        assertTrue(compareResults.get(0).isEqual());
        assertTrue(compareResults.get(1).isEqual());

        assertThrows(RuntimeException.class, () -> snapshotUtil.comparePvs(null, -77, ComparisonMode.ABSOLUTE, false));

    }

    @Test
    public void testComparePvsRelative(){
        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://xx(42.0)");
        ConfigPv configPv2 = new ConfigPv();
        configPv2.setPvName("loc://yy(771.0)");

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(42.0, Alarm.none(), Time.now(), Display.none()));
        SnapshotItem snapshotItem2 = new SnapshotItem();
        snapshotItem2.setConfigPv(configPv2);
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 0.0, ComparisonMode.RELATIVE, false);

        assertTrue(compareResults.get(0).isEqual());
        assertTrue(compareResults.get(1).isEqual());

        snapshotItem1.setValue(VDouble.of(43.0, Alarm.none(), Time.now(), Display.none()));
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(), Time.now(), Display.none()));

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 0.0,  ComparisonMode.RELATIVE, false);

        assertFalse(compareResults.get(0).isEqual());
        assertNotNull(compareResults.get(0).getStoredValue());
        assertNotNull(compareResults.get(0).getLiveValue());
        assertTrue(compareResults.get(1).isEqual());
        assertNull(compareResults.get(1).getStoredValue());
        assertNull(compareResults.get(1).getLiveValue());

        snapshotItem1.setValue(VDouble.of(43.0, Alarm.none(), Time.now(), Display.none()));
        snapshotItem2.setValue(VDouble.of(999.0, Alarm.none(), Time.now(), Display.none()));

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 0.1, ComparisonMode.RELATIVE, false);

        assertTrue(compareResults.get(0).isEqual());
        assertFalse(compareResults.get(1).isEqual());

        assertThrows(RuntimeException.class, () -> snapshotUtil.comparePvs(null, -77, ComparisonMode.RELATIVE, false));

    }

    @Test
    public void testCompareWithZeroStored(){
        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://b(0.0)");

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(1.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 10.0, ComparisonMode.RELATIVE, false);
        assertFalse(compareResults.get(0).isEqual());

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 2.0, ComparisonMode.ABSOLUTE, false);
        assertTrue(compareResults.get(0).isEqual());

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 10.0, ComparisonMode.RELATIVE, false);
        assertFalse(compareResults.get(0).isEqual());

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 0.5, ComparisonMode.ABSOLUTE, false);
        assertFalse(compareResults.get(0).isEqual());

        snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(0.0, Alarm.none(), Time.now(), Display.none()));
        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 10.0, ComparisonMode.RELATIVE, false);

        assertTrue(compareResults.get(0).isEqual());
    }

    @Test
    public void testComparePvsWithReadbacks() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setReadbackPvName("loc://aa(50.0)");

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(42.0, Alarm.none(), Time.now(), Display.none()));
        snapshotItem1.setReadbackValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 8.0, ComparisonMode.ABSOLUTE, false);
        assertTrue(compareResults.get(0).isEqual());

        compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 7.0, ComparisonMode.ABSOLUTE, false);
        assertFalse(compareResults.get(0).isEqual());

    }

    @Test
    public void testComparePvsWithIndividualToleranceValues1() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setComparison(new Comparison(ComparisonMode.RELATIVE, 0.3));

        ConfigPv configPv2 = new ConfigPv();
        configPv2.setPvName("loc://aa(42.0)");

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        SnapshotItem snapshotItem2 = new SnapshotItem();
        snapshotItem2.setConfigPv(configPv2);
        snapshotItem2.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2), 9.0, ComparisonMode.ABSOLUTE, false);
        assertTrue(compareResults.get(0).isEqual());
        assertTrue(compareResults.get(1).isEqual());

    }

    @Test
    public void testComparePvsWithIndividualToleranceValues2() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setComparison(new Comparison(ComparisonMode.RELATIVE, 0.15));

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 0.0, null, false);
        assertFalse(compareResults.get(0).isEqual());

    }

    @Test
    public void testComparePvsWithIndividualToleranceValues3() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setComparison(new Comparison(ComparisonMode.ABSOLUTE, 9.0));

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 0.5, ComparisonMode.RELATIVE, false);
        assertTrue(compareResults.get(0).isEqual());

    }

    @Test
    public void testComparePvsWithIndividualToleranceValues4() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");

        ConfigPv configPv11 = new ConfigPv();
        configPv11.setPvName("loc://a(42.0)");
        configPv11.setComparison(new Comparison(ComparisonMode.ABSOLUTE, 7.0));

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1),
                List.of(configPv11),
                0.5,
                ComparisonMode.RELATIVE,
                false);
        assertFalse(compareResults.get(0).isEqual());

    }

    @Test
    public void testComparePvsWithIndividualToleranceValues5() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setReadbackPvName("loc://aa(49.0)");

        ConfigPv configPv11 = new ConfigPv();
        configPv11.setPvName("loc://a(42.0)");
        configPv11.setComparison(new Comparison(ComparisonMode.RELATIVE, 0.1));

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1),
                List.of(configPv11),
                0.0,
                null,
                false);
        assertTrue(compareResults.get(0).isEqual());

    }

    @Test
    public void testComparePvsWithIndividualToleranceValues6() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setReadbackPvName("loc://aa(49.0)");
        configPv1.setComparison(new Comparison(ComparisonMode.RELATIVE, 0.1));

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1), 0.0, null, true);
        assertFalse(compareResults.get(0).isEqual());

    }

    @Test
    public void testComparePvsWithIndividualToleranceValues7() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setReadbackPvName("loc://aa(49.0)");

        ConfigPv configPv11 = new ConfigPv();
        configPv11.setPvName("loc://a(42.0)");
        configPv11.setComparison(new Comparison(ComparisonMode.ABSOLUTE, 1.1));

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1),
                List.of(configPv11),
                0.0,
                null,
                false);
        assertTrue(compareResults.get(0).isEqual());
    }

    @Test
    public void testComparePvsWithIndividualToleranceValues8() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setReadbackPvName("loc://aa(49.0)");

        ConfigPv configPv2 = new ConfigPv();
        configPv2.setPvName("loc://aa(42.0)");
        configPv2.setReadbackPvName("loc://aaa(49.0)");

        ConfigPv configPv11 = new ConfigPv();
        configPv11.setPvName("loc://a(42.0)");
        configPv11.setComparison(new Comparison(ComparisonMode.ABSOLUTE, 11.0));

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        SnapshotItem snapshotItem2 = new SnapshotItem();
        snapshotItem2.setConfigPv(configPv2);
        snapshotItem2.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1, snapshotItem2),
                List.of(configPv11),
                0.0,
                null,
                false);
        assertTrue(compareResults.get(0).isEqual());
        assertFalse(compareResults.get(1).isEqual());
    }



    @Test
    public void testComparePvsWithIndividualToleranceValues9() {

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://a(42.0)");
        configPv1.setReadbackPvName("loc://aa(49.0)");

        ConfigPv configPv11 = new ConfigPv();
        configPv11.setPvName("loc://a(42.0)");
        configPv11.setComparison(new Comparison(ComparisonMode.ABSOLUTE, 1.1));

        ConfigPv configPv22 = new ConfigPv();
        configPv22.setPvName("loc://aaa(42.0)");

        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(50.0, Alarm.none(), Time.now(), Display.none()));

        List<ComparisonResult> compareResults = snapshotUtil.comparePvs(List.of(snapshotItem1),
                List.of(configPv11, configPv22),
                0.0,
                null,
                false);
        assertTrue(compareResults.get(0).isEqual());
        assertFalse(compareResults.get(1).isEqual());
    }
}
