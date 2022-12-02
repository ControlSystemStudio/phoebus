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

import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.SaveAndRestoreClient;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.impl.SaveAndRestoreJerseyClient;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SaveAndRestoreScriptUtil {

    private static SaveAndRestoreClient saveAndRestoreClient;
    private static final Logger logger = Logger.getLogger(SaveAndRestoreScriptUtil.class.getName());

    /**
     * Useful in case a mock client is needed.
     *
     * @param client The client used to interact with the remote service
     */
    public static void setSaveAndRestoreClient(SaveAndRestoreClient client) {
        saveAndRestoreClient = client;
    }

    /**
     * Should be called before a call to the service is invoked.
     */
    private static void ensureClientSet() {
        if (saveAndRestoreClient == null) {
            saveAndRestoreClient = new SaveAndRestoreJerseyClient();
        }
    }

    public static List<Node> getChildNodes(String nodeId) {
        ensureClientSet();
        return saveAndRestoreClient.getChildNodes(nodeId);
    }

    public static List<SnapshotItem> getSnapshotItems(String snapshotId) {
        ensureClientSet();
        return saveAndRestoreClient.getSnapshotData(snapshotId).getSnapshotItems();
    }

    /**
     * Restores PV values from a given snapshot. Before values are written to the PVs, this method will first
     * connect to all of the PVs. If any of the PVs fails to connect, an exception is thrown, i.e. the restore
     * operation is aborted.
     * <p>
     * Once all PVs have been successfully connected, the persisted values in the snapshot are written in an
     * synchronous manner, i.e. the call to the write operation on a PV will wait for completion before a write
     * operation on the next PV is invoked.
     * </p>
     *
     * @param snapshotId     The unique id of a snapshot, which can be copied to the clipboard in the save-and-restore UI.
     * @param connectTimeout The timeout in ms when connecting to the PVs. If not all PVs are connected after
     *                       <code>connectTimeout</code> ms, an exception is thrown.
     * @param writeTimeout   The timeout i ms to wait for a single write operation to complete.
     * @param abortOnFail    Determines if write of PV values should be aborted when a write failure occurs, e.g.
     *                       PV is disconnected or read-only.
     * @param rollBack       Determines if restored PVs should be restored to the original state if a write failure occurs.
     * @return A {@link RestoreReport} holding data that can be used to analyze the outcome of the process.
     * @throws Exception In either of these following cases:
     *                   <ul>
     *                       <li>The remote save-and-restore service is unavailable.</li>
     *                       <li>No snapshot identified by the snapshot id exists.</li>
     *                       <li>The snapshot is not associated with any persisted PV values. This is a corner case...</li>
     *                       <li>If any of the PVs fails to connect.</li>
     *                   </ul>
     */
    public static RestoreReport restore(String snapshotId, int connectTimeout, int writeTimeout, boolean abortOnFail, boolean rollBack) throws Exception {
        ensureClientSet();
        saveAndRestoreClient.getNode(snapshotId); // This will throw an exception if the snapshot does not exist.
        List<SnapshotItem> snapshotItems = saveAndRestoreClient.getSnapshotData(snapshotId).getSnapshotItems();
        List<SnapshotItem> restorableItems =
                snapshotItems.stream().filter(item -> !item.getConfigPv().isReadOnly()).collect(Collectors.toList());
        if (restorableItems.isEmpty()) { // Should really not happen.
            throw new Exception("No restorable PVs found in snapshot id " + snapshotId);
        }

        Map<String, VType> savedValues = new HashMap<>();
        Map<String, PV> pvs = new HashMap<>();
        List<CompletableFuture<VType>> latest = new ArrayList<>();
        boolean allConnected = true;
        try {
            // First connect to all PVs and read values.
            for (SnapshotItem item : restorableItems) {
                final CompletableFuture<VType> done = new CompletableFuture<>();
                latest.add(done);
                String pvName = item.getConfigPv().getPvName();
                final org.phoebus.pv.PV pv = PVPool.getPV(pvName);
                pvs.put(pvName, pv);
                pv.onValueEvent().subscribe(value -> {
                    if (!PV.isDisconnected(value)) {
                        savedValues.put(pvName, value);
                        done.complete(value);
                    }
                });
            }
            CompletableFuture.allOf(latest.toArray(new CompletableFuture[latest.size()]))
                    .get(connectTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to connect and read all PVs", e);
            allConnected = false;
        }

        // If any of the PVs fails to connect, abort the restore process.
        if (!allConnected) {
            pvs.forEach((key, value) -> PVPool.releasePV(value));
            throw new Exception("Failed to connect to all PVs within " + connectTimeout + " ms.");
        }
        String path = saveAndRestoreClient.getFullPath(snapshotId);
        RestoreReport restoreReport = new RestoreReport(new Date(), snapshotId, path);
        Map<String, Object> restoredPVs = new HashMap<>();
        List<String> nonRestoredPvs = new ArrayList<>();
        List<RollBackItem> rollBackItems = new ArrayList<>();
        // All connected, now write values
        boolean writeFailureDetected = false;
        for (SnapshotItem item : restorableItems) {
            String pvName = item.getConfigPv().getPvName();
            PV pv = pvs.get(pvName);
            try {
                Object vType = Utilities.toRawValue(item.getValue());
                Future<?> result = pv.asyncWrite(vType);
                result.get(1000, TimeUnit.MILLISECONDS);
                restoredPVs.put(pvName, vType);
                // A restored PV may be subject to rollback, so add it here
                rollBackItems.add(new RollBackItem(pv, savedValues.get(pvName)));
            } catch (Exception exception) {
                logger.log(Level.WARNING, "Failed to restore/write PV " + pvName);
                writeFailureDetected = true;
                if (abortOnFail) {
                    break;
                }
            }
        }
        if (writeFailureDetected && rollBack) {
            Map<String, Object> rolledBackPVs = rollback(rollBackItems, writeTimeout);
            restoreReport.setRolledBackPVs(rolledBackPVs);
        }
        // Determine the list of PVs that were not restored.
        List<String> restorablePVNames = pvs.values().stream().map(PV::getName).collect(Collectors.toList());
        List<String> restoredPVNames = new ArrayList<>(restoredPVs.keySet());
        restorablePVNames.removeAll(restoredPVNames);
        restoreReport.setNonRestoredPVs(restorablePVNames);

        restoreReport.setRestoredPVs(restoredPVs);

        pvs.forEach((key, value) -> PVPool.releasePV(value));

        return restoreReport;
    }

    private static Map<String, Object> rollback(List<RollBackItem> rollBackItems, int writeTimeout) {
        Map<String, Object> rolledBackPVs = new HashMap<>();
        rollBackItems.forEach(item -> {
            try {
                logger.log(Level.INFO, "Rollback of PV " + item.pv.getName() + " to value " + item.value);
                Future<?> result = item.pv.asyncWrite(Utilities.toRawValue(item.value));
                result.get(writeTimeout, TimeUnit.MILLISECONDS);
                rolledBackPVs.put(item.pv.getName(), item.value);
            } catch (Exception exception) {
                logger.log(Level.WARNING, "Failed to rollback PV " + item.pv.getName(), exception);
            }
        });
        return rolledBackPVs;
    }

    private static class RollBackItem {
        private final VType value;
        private final PV pv;

        public RollBackItem(PV pv, VType value) {
            this.pv = pv;
            this.value = value;
        }
    }
}
