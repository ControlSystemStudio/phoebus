/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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
 */

package org.phoebus.applications.saveandrestore.script;

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VInt;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.SaveAndRestoreClient;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class SaveAndRestoreScriptUtilTest {

    private static SaveAndRestoreClient saveAndRestoreClient;
    private static final String UNIQUE_ID = UUID.randomUUID().toString();
    private static PV localPV1;
    private static PV localPV2;

    @BeforeClass
    public static void init() {
        saveAndRestoreClient = Mockito.mock(SaveAndRestoreClient.class);
        SaveAndRestoreScriptUtil.setSaveAndRestoreClient(saveAndRestoreClient);

        try {
            localPV1 = PVPool.getPV("loc://pv1(1)");
            localPV2 = PVPool.getPV("loc://pv2(11)");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Test(expected = Exception.class)
    public void testRestoreInvalidSnapshotId() throws Exception {
        Mockito.reset(saveAndRestoreClient);
        when(saveAndRestoreClient.getNode(UNIQUE_ID)).thenThrow(RuntimeException.class);
        SaveAndRestoreScriptUtil.restore(UNIQUE_ID, 1000, 100, false, false);
    }

    @Test(expected = Exception.class)
    public void testRestoreNoSnapshotItems() throws Exception {
        Mockito.reset(saveAndRestoreClient);
        when(saveAndRestoreClient.getNode(UNIQUE_ID)).thenReturn(new Node());
        when(saveAndRestoreClient.getSnapshotItems(UNIQUE_ID)).thenThrow(RuntimeException.class);
        SaveAndRestoreScriptUtil.restore(UNIQUE_ID, 1000, 1000, false, false);
    }

    @Test(expected = Exception.class)
    public void testRestoreNoRestorablePVs() throws Exception {
        Mockito.reset(saveAndRestoreClient);
        when(saveAndRestoreClient.getNode(UNIQUE_ID)).thenReturn(new Node());
        SnapshotItem snapshotItem = new SnapshotItem();
        ConfigPv configPv = new ConfigPv();
        configPv.setId(1);
        configPv.setPvName("loc://pv1");
        configPv.setReadOnly(true);
        snapshotItem.setConfigPv(configPv);
        snapshotItem.setValue(VInt.of(7, Alarm.none(), Time.now(), Display.none()));
        when(saveAndRestoreClient.getSnapshotItems(UNIQUE_ID)).thenReturn(Arrays.asList(snapshotItem));
        SaveAndRestoreScriptUtil.restore(UNIQUE_ID, 1000, 1000, false, false);
    }

    @Test(expected = Exception.class)
    public void testRestoreConnectionFailed() throws Exception {
        Mockito.reset(saveAndRestoreClient);
        when(saveAndRestoreClient.getNode(UNIQUE_ID)).thenReturn(new Node());
        SnapshotItem snapshotItem = new SnapshotItem();
        ConfigPv configPv = new ConfigPv();
        configPv.setId(1);
        configPv.setPvName("bad");
        configPv.setReadOnly(false);
        snapshotItem.setConfigPv(configPv);
        snapshotItem.setValue(VInt.of(7, Alarm.none(), Time.now(), Display.none()));
        when(saveAndRestoreClient.getSnapshotItems(UNIQUE_ID)).thenReturn(Arrays.asList(snapshotItem));
        SaveAndRestoreScriptUtil.restore(UNIQUE_ID, 1000, 1000, false, false);
    }

    @Test
    public void testRestore() throws Exception {
        Mockito.reset(saveAndRestoreClient);
        when(saveAndRestoreClient.getNode(UNIQUE_ID)).thenReturn(new Node());

        SnapshotItem snapshotItem1 = new SnapshotItem();
        ConfigPv configPv1 = new ConfigPv();
        configPv1.setId(1);
        configPv1.setPvName("loc://pv1");
        configPv1.setReadOnly(false);
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VInt.of(7, Alarm.none(), Time.now(), Display.none()));

        SnapshotItem snapshotItem2 = new SnapshotItem();
        ConfigPv configPv2 = new ConfigPv();
        configPv2.setId(2);
        configPv2.setPvName("loc://pv2");
        configPv2.setReadOnly(true);
        snapshotItem2.setConfigPv(configPv2);
        snapshotItem2.setValue(VInt.of(77, Alarm.none(), Time.now(), Display.none()));

        when(saveAndRestoreClient.getSnapshotItems(UNIQUE_ID)).thenReturn(Arrays.asList(snapshotItem1, snapshotItem2));
        RestoreReport restoreReport = SaveAndRestoreScriptUtil.restore(UNIQUE_ID, 1000, 1000, false, false);
        Future<?> result = localPV1.asyncRead();
        VDouble v = (VDouble) result.get();
        assertEquals(v.getValue(), 7.0, 0.0);
        result = localPV2.asyncRead();
        v = (VDouble) result.get();
        assertEquals(v.getValue(), 11.0, 0.0);

        assertTrue(restoreReport.getNonRestoredPVs().isEmpty());
        assertEquals(1, restoreReport.getRestoredPVs().size());
    }
}
