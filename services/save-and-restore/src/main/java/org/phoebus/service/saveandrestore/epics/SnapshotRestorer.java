package org.phoebus.service.saveandrestore.epics;

import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SnapshotRestorer {

    private final Logger LOG = Logger.getLogger(SnapshotRestorer.class.getName());

    @Value("${connection.timeout:5000}")
    private int connectionTimeout;

    @Value("${write.timeout:5000}")
    private int writeTimeout;

    @Autowired
    private ExecutorService executorService;

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
        // Attempt to connect to all PVs before trying to write/restore.
        List<PV> connectedPvs = connectPVs(snapshotItems);
        List<RestoreResult> failedPvs = new ArrayList<>();
        final CountDownLatch countDownLatch = new CountDownLatch(snapshotItems.size());
        snapshotItems.forEach(item -> {
            String pvName = item.getConfigPv().getPvName();
            Optional<PV> pvOptional = null;
            try {
                // Check if PV is connected. If not, do not even try to write/restore.
                pvOptional = connectedPvs.stream().filter(pv -> pv.getName().equals(pvName)).findFirst();
                if (pvOptional.isPresent()) {
                    pvOptional.get().write(VTypeHelper.toObject(item.getValue()));
                } else {
                    RestoreResult restoreResult = new RestoreResult();
                    restoreResult.setSnapshotItem(item);
                    restoreResult.setErrorMsg("PV disconnected");
                    failedPvs.add(restoreResult);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to restore PV " + pvName);
                RestoreResult restoreResult = new RestoreResult();
                restoreResult.setSnapshotItem(item);
                restoreResult.setErrorMsg(e.getMessage());
                failedPvs.add(restoreResult);
            } finally {
                if (pvOptional != null && pvOptional.isPresent()) {
                    PVPool.releasePV(pvOptional.get());
                }
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(writeTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.log(Level.INFO, "Encountered InterruptedException", e);
        }

        return failedPvs;
    }

    /**
     * Attempts to connect to all PVs using {@link PVPool}. A connection is considered successful once an
     * event is received that does not indicate disconnection.
     *
     * <p>
     * A timeout of {@link #connectionTimeout} ms is used to wait for a PV to supply a value message indicating
     * successful connection.
     * </p>
     * <p>
     * An {@link ExecutorService} is used to run connection attempts concurrently. However, no timeout is employed
     * for the overall execution of all connection attempts.
     * </p>
     *
     * @param snapshotItems List of {@link SnapshotItem}s in a snapshot.
     * @return A {@link List} of {@link PV}s for which connection succeeded. Ideally this should be all
     * PVs as listed in the input argument.
     */
    private List<PV> connectPVs(List<SnapshotItem> snapshotItems) {
        List<PV> connectedPvs = new ArrayList<>();

        List<? extends Callable<Void>> callables = snapshotItems.stream().map(snapshotItem -> {
            return (Callable<Void>) () -> {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                try {
                    PV pv = PVPool.getPV(snapshotItem.getConfigPv().getPvName());
                    pv.onValueEvent().subscribe(value -> {
                        if (!PV.isDisconnected(value)) {
                            connectedPvs.add(pv);
                            countDownLatch.countDown();
                        }
                    });
                    // Wait for a value message indicating connection
                    countDownLatch.await(connectionTimeout, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to connect to PV " + snapshotItem.getConfigPv().getPvName(), e);
                    countDownLatch.countDown();
                }
                return null;
            };
        }).toList();

        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Got exception waiting for all tasks to finish", e);
            // Return empty list here?
            return Collections.emptyList();
        }

        return connectedPvs;
    }
}
