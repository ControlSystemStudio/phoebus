package org.phoebus.service.saveandrestore.epics;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.epics.pva.client.PVAClient;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.epics.vtype.VUByteArray;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VULongArray;
import org.epics.vtype.VUShortArray;

import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

public class SnapshotRestorer {

    PVAClient pva;
    private final Logger LOG = Logger.getLogger(SnapshotRestorer.class.getName());

    public SnapshotRestorer() throws Exception {
        pva = new PVAClient();
        final File site_settings = new File("settings.ini");
        if (site_settings.canRead()) {
            LOG.config("Loading settings from " + site_settings);
            PropertyPreferenceLoader.load(new FileInputStream(site_settings));
        }
    }

    /**
     * Restore PV values from a list of snapshot items
     *
     * <p>
     * Writes concurrently the pv value to the non null set PVs in
     * the snapshot items.
     * Uses synchonized to ensure only one frontend can write at a time.
     * Returns a list of the snapshot items you have set, with an error message if
     * an error occurred.
     *
     * @param snapshotItems {@link SnapshotItem}
     */
    public synchronized List<RestoreResult> restorePVValues(List<SnapshotItem> snapshotItems) {

        var futures = snapshotItems.stream().filter(
                (snapshot_item) -> snapshot_item.getConfigPv().getPvName() != null)
                .map((snapshotItem) -> {
                    var pvName = snapshotItem.getConfigPv().getPvName();
                    var pvValue = snapshotItem.getValue();
                    Object rawValue = vTypeToObject(pvValue);
                    PV pv;
                    CompletableFuture<?> future;
                    try {
                        pv = PVPool.getPV(pvName);
                        future = pv.asyncWrite(rawValue);
                    } catch (Exception e) {
                        var restoreResult = new RestoreResult();
                        var errorMsg = e.getMessage();
                        restoreResult.setSnapshotItem(snapshotItem);
                        restoreResult.setErrorMsg(errorMsg);
                        LOG.warning(String.format("Error writing to channel %s %s", pvName, errorMsg));
                        return CompletableFuture.completedFuture(restoreResult);
                    }
                    return future.handle((result, ex) -> {
                        String errorMsg;
                        if (ex != null) {
                            errorMsg = ex.getMessage();
                            LOG.warning(String.format("Error writing to channel %s %s", pvName, errorMsg));
                        } else {
                            errorMsg = null;
                        }
                        var restoreResult = new RestoreResult();
                        restoreResult.setSnapshotItem(snapshotItem);
                        restoreResult.setErrorMsg(errorMsg);
                        return restoreResult;
                    });
                })
                .collect(Collectors.toList());

        CompletableFuture<Void> all_done = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // Wait on the futures concurrently
        all_done.join();

        // Joins should not block as all the futures should be completed.
        return futures.stream().map(
                (future) -> future.join()).collect(Collectors.toList());
    }

    /**
     * Convert a vType to its Object representation
     * 
     * @param type {@link VType}
     */
    private Object vTypeToObject(VType type) {
        if (type == null) {
            return null;
        }
        if (type instanceof VNumberArray) {
            if (type instanceof VIntArray || type instanceof VUIntArray) {
                return VTypeHelper.toIntegers(type);
            } else if (type instanceof VDoubleArray) {
                return VTypeHelper.toDoubles(type);
            } else if (type instanceof VFloatArray) {
                return VTypeHelper.toFloats(type);
            } else if (type instanceof VLongArray || type instanceof VULongArray) {
                return VTypeHelper.toLongs(type);
            } else if (type instanceof VShortArray || type instanceof VUShortArray) {
                return VTypeHelper.toShorts(type);
            } else if (type instanceof VByteArray || type instanceof VUByteArray) {
                return VTypeHelper.toBytes(type);
            }
        } else if (type instanceof VEnumArray) {
            List<String> data = ((VEnumArray) type).getData();
            return data.toArray(new String[data.size()]);
        } else if (type instanceof VStringArray) {
            List<String> data = ((VStringArray) type).getData();
            return data.toArray(new String[data.size()]);
        } else if (type instanceof VBooleanArray) {
            return VTypeHelper.toBooleans(type);
        } else if (type instanceof VNumber) {
            return ((VNumber) type).getValue();
        } else if (type instanceof VEnum) {
            return ((VEnum) type).getIndex();
        } else if (type instanceof VString) {
            return ((VString) type).getValue();
        } else if (type instanceof VBoolean) {
            return ((VBoolean) type).getValue();
        }
        return null;
    }
}
