package org.phoebus.saveandrestore.util;

import org.apache.commons.collections4.CollectionUtils;
import org.epics.vtype.VNumber;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.model.Comparison;
import org.phoebus.applications.saveandrestore.model.ComparisonResult;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.ComparisonMode;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides some utility methods to read and write PVs in an asynchronous manner.
 * And to perform a comparison operation.
 */
public class SnapshotUtil {

    private final Logger LOG = Logger.getLogger(SnapshotUtil.class.getName());

    private final int connectionTimeout = Preferences.connectionTimeout;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

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

        List<RestoreCallable> callables = new ArrayList<>();
        for (SnapshotItem si : cleanedSnapshotItems) {
            RestoreCallable restoreCallable = new RestoreCallable(si, restoreResultList);
            callables.add(restoreCallable);
        }

        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Got exception waiting for all tasks to finish", e);
            // Return empty list here?
            return Collections.emptyList();
        } finally {
            callables.forEach(RestoreCallable::release);
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
        return takeSnapshot(configurationData.getPvList());
    }

    /**
     * Reads all PVs and read-back PVs as defined in the {@link ConfigurationData} argument. For each
     * {@link ConfigPv} item in {@link ConfigurationData} a {@link SnapshotItem} is created.
     * Read operations are concurrent using a thread pool. Failed connections/reads will cause a wait of at most
     * {@link #connectionTimeout} ms on each thread.
     *
     * @param configPvs List of {@link ConfigPv}s defining a {@link Configuration}.
     * @return A list of {@link SnapshotItem}s holding the values read from IOCs.
     */
    public List<SnapshotItem> takeSnapshot(final List<ConfigPv> configPvs) {
        List<SnapshotItem> snapshotItems = new ArrayList<>();
        List<Callable<Void>> callables = new ArrayList<>();
        Map<String, VType> pvValues = Collections.synchronizedMap(new HashMap<>());
        Map<String, VType> readbackPvValues = Collections.synchronizedMap(new HashMap<>());
        for (ConfigPv configPv : configPvs) {
            Callable<Void> pvValueCallable = () -> {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                PV pv = null;
                try {
                    pv = PVPool.getPV(configPv.getPvName());
                    pv.onValueEvent().subscribe(value -> {
                        if (!VTypeHelper.isDisconnected(value)) {
                            countDownLatch.countDown();
                        }
                    });
                    if (!countDownLatch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                        LOG.log(Level.WARNING, "Connection to PV '" + configPv.getPvName() +
                                "' timed out after " + connectionTimeout + " ms.");
                        pvValues.put(configPv.getPvName(), null);
                    } else {
                        pvValues.put(configPv.getPvName(), pv.read());
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to read PV '" + configPv.getPvName() + "'", e);
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

        for (ConfigPv configPv : configPvs) {
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
                            countDownLatch.countDown();
                        }
                    });
                    if (!countDownLatch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                        LOG.log(Level.WARNING, "Connection to read-back PV '" + configPv.getReadbackPvName() +
                                "' timed out after " + connectionTimeout + " ms.");
                        readbackPvValues.put(configPv.getReadbackPvName(), null);
                    } else {
                        readbackPvValues.put(configPv.getReadbackPvName(), pv.read());
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to read read-back PV '" + configPv.getReadbackPvName() + "'", e);
                    readbackPvValues.put(configPv.getReadbackPvName(), null);
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
            synchronized (pvValues) {
                SnapshotItem snapshotItem = new SnapshotItem();
                for (ConfigPv configPv : configPvs) {
                    if (configPv.getPvName().equals(pvName)) {
                        snapshotItem.setConfigPv(configPv);
                        break;
                    }
                }
                VType value = pvValues.get(pvName);
                snapshotItem.setValue(value);
                if (snapshotItem.getConfigPv().getReadbackPvName() != null) {
                    VType readbackValue = readbackPvValues.get(snapshotItem.getConfigPv().getReadbackPvName());
                    snapshotItem.setReadbackValue(readbackValue);
                }
                snapshotItems.add(snapshotItem);
            }
        }

        return snapshotItems;
    }

    private List<SnapshotItem> cleanSnapshotItems(List<SnapshotItem> snapshotItems) {
        return snapshotItems.stream().filter(si -> !si.getConfigPv().isReadOnly()).toList();
    }

    /**
     * Wraps PV functionality such that client code may release PV back to the pool once
     * write/restore operation has succeeded (or failed).
     */
    private class RestoreCallable implements Callable<Void> {

        private final List<RestoreResult> restoreResultList;
        private PV pv;
        private final SnapshotItem snapshotItem;

        public RestoreCallable(SnapshotItem snapshotItem, List<RestoreResult> restoreResultList) {
            this.snapshotItem = snapshotItem;
            this.restoreResultList = restoreResultList;
        }

        @Override
        public Void call() {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            try {
                pv = PVPool.getPV(snapshotItem.getConfigPv().getPvName());
                pv.onValueEvent().subscribe(value -> {
                    if (!PV.isDisconnected(value)) {
                        countDownLatch.countDown();
                    }
                });
                if (!countDownLatch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                    LOG.log(Level.WARNING, "Connection to PV '" + snapshotItem.getConfigPv().getPvName() +
                            "' timed out after " + connectionTimeout + "ms.");
                    RestoreResult restoreResult = new RestoreResult();
                    restoreResult.setSnapshotItem(snapshotItem);
                    restoreResult.setErrorMsg("No monitor event from PV '" + snapshotItem.getConfigPv().getPvName() + "'");
                    restoreResultList.add(restoreResult);
                } else {
                    pv.write(VTypeHelper.toObject(snapshotItem.getValue()));
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to write to PV '" + snapshotItem.getConfigPv().getPvName() + "'", e);
                RestoreResult restoreResult = new RestoreResult();
                restoreResult.setSnapshotItem(snapshotItem);
                restoreResult.setErrorMsg("Failed to write to PV '" + snapshotItem.getConfigPv().getPvName() + "', cause: " + e.getMessage());
                restoreResultList.add(restoreResult);
                countDownLatch.countDown();
            }
            return null;
        }

        public void release() {
            if (pv != null) {
                PVPool.releasePV(pv);
            }
        }
    }

    /**
     * @see #comparePvs(List, List, double, ComparisonMode, boolean)
     */
    public List<ComparisonResult> comparePvs(final List<SnapshotItem> savedSnapshotItems,
                                             double tolerance,
                                             ComparisonMode compareMode,
                                             boolean skipReadback) {
        return comparePvs(savedSnapshotItems, null, tolerance, compareMode, skipReadback);
    }

    /**
     * Performs comparison between stored PV values and live values. Note that comparison uses optional data stored
     * in the snapshot's parent configuration data, see {@link ConfigPv}, if defined. Caller is responsible for
     * passing a list of {@link ConfigPv}s matching the list of {@link SnapshotItem}s. Since configurations may
     * have been updated (e.g. PVs added) after snapshots have been created, any {@link SnapshotItem}'s PV not found
     * in the list of {@link ConfigPv}s will be compared using the provided {@link ComparisonMode} and tolerance.
     * If the list of {@link ConfigPv}s is <code>null</code> or empty, the provided {@link ComparisonMode} and tolerance is used.
     * <p>
     * Equality between a stored value and the live value is determined on each PV like so:
     * <ul>
     *     <li>If the configuration of a PV does not specify a {@link ComparisonMode} and tolerance,
     *     the <code>compareMode</code> and <code>tolerance</code> parameters are used.
     *     <code>compareMode</code> however is optional and defaults to {@link ComparisonMode#ABSOLUTE},
     *     while tolerance defaults to zero.
     *     </li>
     *     <li>
     *         The base (reference) value is always the value stored in the <code>value</code> field of a {@link org.phoebus.applications.saveandrestore.model.SnapshotItem}
     *         object. It corresponds to the <code>pvName</code> field, i.e. never the <code>readbackPvName</code> of
     *         a {@link ConfigPv} object.
     *     </li>
     *     <li>
     *         The live value used in the comparison is either the value corresponding to <code>pvName</code>, or
     *         <code>readbackPvName</code> if specified. The latter can be overridden with the <code>skipReadback</code>
     *         parameter.
     *     </li>
     *     <li>
     *         Comparison will consider {@link ComparisonMode} and tolerance only on numeric scalar types.
     *         See {@link Utilities}.
     *     </li>
     * </ul>
     * </p>
     *
     * @param savedSnapshotItems A list if {@link SnapshotItem}s as pulled from a stored snapshot.
     * @param configPvs         The list of {@link ConfigPv}s the items in <code>savedSnapshotItems</code>.
     *                           May be <code>null</code> or empty.
     * @param tolerance          A tolerance (must be >=0) value used in the comparison.
     * @param comparisonMode        Determines if comparison is relative or absolute.
     * @param skipReadback       Indicates that comparison should not use the read-back PV, even if specified.
     * @return A list of {@link ComparisonResult}s, one for each {@link SnapshotItem}. Note though that
     * if the comparison evaluates to equal, then the actual live and stored value are not added to the {@link ComparisonResult}
     * objects in order to avoid transferring potentially large amounts of data (e.g. large arrays).
     */
    public List<ComparisonResult> comparePvs(final List<SnapshotItem> savedSnapshotItems,
                                             final List<ConfigPv> configPvs,
                                             double tolerance,
                                             ComparisonMode comparisonMode,
                                             boolean skipReadback) {
        if (tolerance < 0) {
            throw new RuntimeException("Tolerance value must be >=0");
        }
        // Default to absolute.
        if(comparisonMode == null){
            comparisonMode = ComparisonMode.ABSOLUTE;
        }
        final Comparison defaultComparison = new Comparison(comparisonMode, tolerance);
        List<ComparisonResult> comparisonResults = new ArrayList<>();

        // Extract the list of ConfigPvs and...
        List<ConfigPv> configPvsFromSnapshot = savedSnapshotItems.stream().map(SnapshotItem::getConfigPv).toList();
        // ...take snapshot to retrieve live values
        List<SnapshotItem> liveSnapshotItems = takeSnapshot(configPvsFromSnapshot);

        savedSnapshotItems.forEach(savedItem -> {
            SnapshotItem liveSnapshotItem = liveSnapshotItems.stream().filter(si -> si.getConfigPv().getPvName().equals(savedItem.getConfigPv().getPvName())).findFirst().orElse(null);
            if (liveSnapshotItem == null) {
                throw new RuntimeException("Unable to match stored PV " + savedItem.getConfigPv().getPvName() + " in list of live PVs");
            }
            VType storedValue = savedItem.getValue(); // Always PV name field, even if read-back PV is specified
            VType liveValue = liveSnapshotItem.getValue();
            VType liveReadbackValue = liveSnapshotItem.getReadbackValue();

            Comparison finalComparison =
                    new Comparison(defaultComparison.getComparisonMode(), defaultComparison.getTolerance());
            // Does this SnapshotItems configuration define per-PV Comparison?
            Comparison perPvComparison = getComparison(configPvs, savedItem.getConfigPv().getPvName());
            if(perPvComparison != null){
                finalComparison = perPvComparison;
            }

            // Determine if comparison is made on read-back or not.
            VType referenceValue = getReferenceValue(liveValue, liveReadbackValue, skipReadback);

            // For relative tolerance and scalar types, compute an absolute tolerance
            // since this is what Utilities.areValuesEqual expects.
            if(finalComparison.getTolerance() > 0 &&
                    finalComparison.getComparisonMode().equals(ComparisonMode.RELATIVE) &&
                    referenceValue instanceof VNumber){
                finalComparison.setTolerance(VTypeHelper.toDouble(referenceValue) * finalComparison.getTolerance());
            }

            Threshold<Number> threshold = new Threshold<>(finalComparison.getTolerance());
            boolean equal = Utilities.areValuesEqual(storedValue, referenceValue, Optional.of(threshold));
            ComparisonResult comparisonResult = new ComparisonResult(savedItem.getConfigPv().getPvName(),
                    equal,
                    finalComparison,
                    equal ? null : storedValue, // Do not add potentially large amounts of data if comparison shows equal
                    equal ? null : liveValue,   // Do not add potentially large amounts of data if comparison shows equal
                    Utilities.deltaValueToString(storedValue, liveValue, Optional.of(threshold)).getString());
            comparisonResults.add(comparisonResult);
        });

        comparisonResults.addAll(handleConfigSnapshotDiff(configPvs, savedSnapshotItems));

        return comparisonResults;
    }

    protected VType getReferenceValue(final VType liveValue, final VType liveReadbackValue, final boolean skipReadback){
        if(skipReadback){
            return liveValue;
        }
        return liveReadbackValue != null ? liveReadbackValue : liveValue;
    }

    /**
     * Locates the {@link ConfigPv} for a PV name as defined in the snapshot. Note that this is needed as the
     * configuration may have changed (e.g. with respect to {@link Comparison} data) since the snapshot was created.
     * @param configPvs List of {@link ConfigPv}s, may be <code>null</code> or empty.
     * @param pvName The PV name to look for.
     * @return A {@link Comparison} object if the corresponding configuration defines it for this PV name.
     * Otherwise <code>null</code>.
     */
    protected Comparison getComparison(List<ConfigPv> configPvs, String pvName){
        if(configPvs == null){
            return null;
        }
        Optional<ConfigPv> configPvOptional = configPvs.stream().filter(cp -> cp.getPvName().equals(pvName)).findFirst();
        if(configPvOptional.isPresent()){
            return configPvOptional.get().getComparison();
        }
        return null;
    }

    /**
     * Check if list of ConfigPvs contains items not found in the snapshot. Since such items cannot be compared,
     * they are by definition non-equal.
     * @param configPvs List of {@link ConfigPv}s, may be <code>null</code> or empty.
     * @param savedSnapshotItems List of saved {@link SnapshotItem}s
     * @return A potentially empty list of {@link ComparisonResult}s, each indicating that a PV was found in the
     * configuration, but not in the saved snapshot.
     */
    protected List<ComparisonResult> handleConfigSnapshotDiff(List<ConfigPv> configPvs, List<SnapshotItem> savedSnapshotItems){
        if(configPvs == null){
            return Collections.emptyList();
        }
        List<ComparisonResult> comparisonResults = new ArrayList<>();

        if(configPvs != null){
            Collection<String> pvNamesInConfig = configPvs.stream().map(ConfigPv::getPvName).toList();
            Collection<String> pvNamesInSnapshot = savedSnapshotItems.stream().map(i -> i.getConfigPv().getPvName()).toList();
            Collection<String> pvNameDiff = CollectionUtils.removeAll(pvNamesInConfig, pvNamesInSnapshot);
            pvNameDiff.forEach(pvName -> {
                ComparisonResult comparisonResult = new ComparisonResult(pvName,
                        false,
                        null,
                        null,
                        null,
                        "PV found in config but not in snapshot");
                comparisonResults.add(comparisonResult);
            });
        }
        return comparisonResults;
    }
}
