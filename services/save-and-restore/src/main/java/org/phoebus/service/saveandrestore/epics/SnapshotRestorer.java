package org.phoebus.service.saveandrestore.epics;

import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SnapshotRestorer {

    private final Logger LOG = Logger.getLogger(SnapshotRestorer.class.getName());

    public SnapshotRestorer() {
        final File site_settings = new File("settings.ini");
        if (site_settings.canRead()) {
            LOG.config("Loading settings from " + site_settings);
            try {
                PropertyPreferenceLoader.load(new FileInputStream(site_settings));
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Unable to read settings.ini, falling back to default values.");
            }
        }
    }

    /**
     * Restore PV values from a list of snapshot items
     *
     * <p>
     * Writes concurrently the pv value to the non-null set PVs in
     * the snapshot items.
     * Uses synchronized to ensure only one frontend can write at a time.
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
                    Object rawValue = VTypeHelper.toObject(pvValue);
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
                .toList();

        CompletableFuture<Void> all_done = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // Wait on the futures concurrently
        all_done.join();

        // Joins should not block as all the futures should be completed.
        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }
}
