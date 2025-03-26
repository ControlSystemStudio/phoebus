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
 *
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;

import org.epics.vtype.VType;
import org.phoebus.saveandrestore.util.VNoData;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class binding the snapshot table views to a {@link PV} for each row such
 * that the view can show the live values. A hardcoded throttling time of 500ms keeps
 * update rate of PV values under control.
 */
public class SaveAndRestorePV {

    private final String pvName;

    private final String readbackPvName;
    private CountDownLatch countDownLatch;
    private PV pv;
    private PV readbackPv;
    private VType pvValue = VDisconnectedData.INSTANCE;
    private VType readbackValue = VDisconnectedData.INSTANCE;
    private TableEntry snapshotTableEntry;

    /**
     * The time between updates of dynamic data in the table, in ms.
     */
    private static final long TABLE_UPDATE_INTERVAL = 500;

    protected SaveAndRestorePV(TableEntry snapshotTableEntry) {
        this.snapshotTableEntry = snapshotTableEntry;
        this.pvName = patchPvName(snapshotTableEntry.pvNameProperty().get());
        this.readbackPvName = patchPvName(snapshotTableEntry.readbackNameProperty().get());

        try {
            pv = PVPool.getPV(pvName);
            pv.onValueEvent().throttleLatest(TABLE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS).subscribe(value -> {
                pvValue = org.phoebus.pv.PV.isDisconnected(value) ? VDisconnectedData.INSTANCE : value;
                this.snapshotTableEntry.setLiveValue(pvValue);
            });

            if (readbackPvName != null && !readbackPvName.isEmpty()) {
                readbackPv = PVPool.getPV(readbackPvName);
                readbackPv.onValueEvent()
                        .throttleLatest(TABLE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
                        .subscribe(value -> {
                            this.readbackValue = org.phoebus.pv.PV.isDisconnected(value) ? VDisconnectedData.INSTANCE : value;
                            this.snapshotTableEntry.setReadbackValue(this.readbackValue);
                        });
            } else {
                // If configuration does not define read-back PV, then UI should show "no data" rather than "disconnected"
                this.snapshotTableEntry.setReadbackValue(VNoData.INSTANCE);
            }
        } catch (Exception e) {
            Logger.getLogger(SaveAndRestorePV.class.getName()).log(Level.INFO, "Error connecting to PV", e);
        }
    }

    private String patchPvName(String pvName) {
        if (pvName == null || pvName.isEmpty()) {
            return null;
        } else if (pvName.startsWith("ca://") || pvName.startsWith("pva://")) {
            return pvName.substring(pvName.lastIndexOf('/') + 1);
        } else {
            return pvName;
        }
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    public void countDown() {
        this.countDownLatch.countDown();
    }

    public void setSnapshotTableEntry(TableEntry snapshotTableEntry) {
        this.snapshotTableEntry = snapshotTableEntry;
        this.snapshotTableEntry.setLiveValue(pv.read());
    }

    void dispose() {
        if (pv != null) {
            PVPool.releasePV(pv);
        }
        if (readbackPv != null) {
            PVPool.releasePV(readbackPv);
        }
    }

    public PV getPv() {
        return pv;
    }

    public PV getReadbackPv() {
        return readbackPv;
    }

    public String getPvName() {
        return pvName;
    }

    public String getReadbackPvName() {
        return readbackPvName;
    }

    public VType getReadbackValue() {
        return readbackValue;
    }
}
