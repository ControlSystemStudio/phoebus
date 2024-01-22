package org.phoebus.service.saveandrestore.web.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.epics.vtype.Time;
import org.epics.vtype.VFloat;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.service.saveandrestore.epics.SnapshotRestorer;

public class SnapshotRestorerTest {

    @Test
    public void testRestorePVValues() throws Exception {
        var snapshotRestorer = new SnapshotRestorer();
        PV pv = PVPool.getPV("loc://x(42.0)");
        var configPv = new ConfigPv();
        configPv.setPvName("loc://x");

        var testSnapshotItem = new SnapshotItem();
        testSnapshotItem.setConfigPv(configPv);
        testSnapshotItem.setValue(VFloat.of(1.0, Alarm.noValue(), Time.now(), Display.none()));
        snapshotRestorer.restorePVValues(
                Arrays.asList(testSnapshotItem));
        var pvValue = pv.asyncRead().get();
        assertEquals(VTypeHelper.toObject(pvValue), 1.0);
    }

    @Test
    public void testCannotConnectPV() throws Exception {
        var snapshotRestorer = new SnapshotRestorer();
        var configPv = new ConfigPv();
        configPv.setPvName("pva://x");

        var testSnapshotItem = new SnapshotItem();
        testSnapshotItem.setConfigPv(configPv);
        testSnapshotItem.setValue(VFloat.of(1.0, Alarm.noValue(), Time.now(), Display.none()));
        var result = snapshotRestorer.restorePVValues(
                Arrays.asList(testSnapshotItem));
        assertNotNull(result.get(0).getErrorMsg());
    }
}
