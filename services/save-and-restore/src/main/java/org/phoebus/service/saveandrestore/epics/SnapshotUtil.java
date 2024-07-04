package org.phoebus.service.saveandrestore.epics;

import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides some utility methods to read and write PVs in an asynchronous manner.
 */
public class SnapshotUtil {

    private final Logger LOG = Logger.getLogger(SnapshotUtil.class.getName());

    @SuppressWarnings("unused")
    @Value("${connection.timeout:5000}")
    private int connectionTimeout;

    @SuppressWarnings("unused")
    @Value("${write.timeout:5000}")
    private int writeTimeout;

    @Autowired
    private ExecutorService executorService;

    public SnapshotUtil() {
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
    public synchronized List<RestoreResult> restore(List<SnapshotItem> snapshotItems) {
        // First clean the list of SnapshotItems from read-only elements.
        List<SnapshotItem> cleanedSnapshotItems = cleanSnapshotItems(snapshotItems);
        List<RestoreResult> restoreResultList = new ArrayList<>();

        List<Callable<Void>> callables = new ArrayList<>();
        for (SnapshotItem si : cleanedSnapshotItems) {
            Callable<Void> writePvCallable = () -> {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                PV pv;
                try {
                    pv = PVPool.getPV(si.getConfigPv().getPvName());
                    pv.onValueEvent().throttleLatest(1000, TimeUnit.MILLISECONDS).subscribe(value -> {
                        if (!PV.isDisconnected(value)) {
                            pv.write(VTypeHelper.toObject(si.getValue()));
                            PVPool.releasePV(pv);
                        }
                        countDownLatch.countDown();
                    });
                    if (!countDownLatch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                        RestoreResult restoreResult = new RestoreResult();
                        restoreResult.setSnapshotItem(si);
                        restoreResult.setErrorMsg("No monitor event from PV " + si.getConfigPv().getPvName());
                        restoreResultList.add(restoreResult);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to write to PV " + si.getConfigPv().getPvName(), e);
                    RestoreResult restoreResult = new RestoreResult();
                    restoreResult.setSnapshotItem(si);
                    restoreResult.setErrorMsg(e.getMessage());
                    restoreResultList.add(restoreResult);
                    countDownLatch.countDown();
                }

                return null;
            };
            callables.add(writePvCallable);
        }

        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Got exception waiting for all tasks to finish", e);
            // Return empty list here?
            return Collections.emptyList();
        }

        return restoreResultList;
    }

    /**
     * Reads all PVs and read-back PVs as defined in the {@link ConfigurationData} argument. For each
     * {@link ConfigPv} item in {@link ConfigurationData} a {@link SnapshotItem} is created holding the
     * values read.
     * Read operations are concurrent using a thread pool. Failed connections/reads will cause a wait of at most
     * {@link #connectionTimeout} ms on each thread.
     *
     * @param configurationData Identifies which {@link Configuration} user selected to create a snapshot.
     * @return A list of {@link SnapshotItem}s holding the values read from IOCs.
     */
    public List<SnapshotItem> takeSnapshot(ConfigurationData configurationData) {
        List<SnapshotItem> snapshotItems = new ArrayList<>();
        List<Callable<Void>> callables = new ArrayList<>();
        Map<String, VType> pvValues = new HashMap<>();
        Map<String, VType> readbackPvValues = new HashMap<>();
        for (ConfigPv configPv : configurationData.getPvList()) {
            Callable<Void> pvValueCallable = () -> {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                PV pv = null;
                try {
                    pv = PVPool.getPV(configPv.getPvName());
                    pv.onValueEvent().subscribe(value -> {
                        if (!VTypeHelper.isDisconnected(value)) {
                            pvValues.put(configPv.getPvName(), value);
                            countDownLatch.countDown();
                        }
                    });
                    if (!countDownLatch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                        pvValues.put(configPv.getPvName(), null);
                    }
                } catch (Exception e) {
                    pvValues.put(configPv.getPvName(), null);
                    countDownLatch.countDown();
                } finally {
                    if (pv != null) {
                        PVPool.releasePV(pv);
                    }
                }
                return null;
            };
            callables.add(pvValueCallable);
        }

        for (ConfigPv configPv : configurationData.getPvList()) {
            if (configPv.getReadbackPvName() == null) {
                continue;
            }
            Callable<Void> readbackPvValueCallable = () -> {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                PV pv = null;
                try {
                    pv = PVPool.getPV(configPv.getReadbackPvName());
                    pv.onValueEvent().subscribe(value -> {
                        if (!VTypeHelper.isDisconnected(value)) {
                            readbackPvValues.put(configPv.getPvName(), value);
                            countDownLatch.countDown();
                        }
                    });
                    if (!countDownLatch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                        readbackPvValues.put(configPv.getPvName(), null);
                    }
                } catch (Exception e) {
                    readbackPvValues.put(configPv.getPvName(), null);
                    countDownLatch.countDown();
                } finally {
                    if (pv != null) {
                        PVPool.releasePV(pv);
                    }
                }
                return null;
            };
            callables.add(readbackPvValueCallable);
        }

        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Got exception waiting for all read tasks to finish", e);
            // Return empty list here?
            return Collections.emptyList();
        }

        // Merge data into SnapshotItems
        for (String pvName : pvValues.keySet()) {
            SnapshotItem snapshotItem = new SnapshotItem();
            for (ConfigPv configPv : configurationData.getPvList()) {
                if (configPv.getPvName().equals(pvName)) {
                    snapshotItem.setConfigPv(configPv);
                    break;
                }
            }
            VType value = pvValues.get(pvName);
            snapshotItem.setValue(value);
            VType readbackValue = readbackPvValues.get(pvName);
            if (readbackValue != null) {
                snapshotItem.setReadbackValue(readbackValue);
            }
            snapshotItems.add(snapshotItem);
        }

        return snapshotItems;
    }

    private List<SnapshotItem> cleanSnapshotItems(List<SnapshotItem> snapshotItems) {
        return snapshotItems.stream().filter(si -> !si.getConfigPv().isReadOnly()).toList();
    }
}
