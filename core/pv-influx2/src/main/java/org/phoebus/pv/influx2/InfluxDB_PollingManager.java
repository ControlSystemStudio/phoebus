/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.influx2;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.stream.Collectors;

import org.epics.vtype.VType;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

/**
 * A manager class that optimizes InfluxDB polling by batching PV requests.
 * <p>
 * This class groups PVs with similar polling periods and same buckets to reduce
 * the total number of InfluxDB queries required. It also handles dynamic bucket
 * changes for PVs that track the "last" bucket.
 */
public class InfluxDB_PollingManager {
    /** Logger for this class */
    private static final Logger LOGGER = Logger.getLogger(InfluxDB_PollingManager.class.getName());

    /** The InfluxDB client used for querying */
    private final InfluxDBClient influxClient;

    /** Shared executor for all polling tasks */
    private final ScheduledExecutorService scheduler;

    /** Maps bucket+period to its polling group */
    private final Map<String, PollingGroup> pollingGroups = new ConcurrentHashMap<>();

    /** Maps a registered PV to its polling group key */
    private final Map<InfluxDB_PV, String> pvToGroupKey = new ConcurrentHashMap<>();

    /** Maximum number of fields to include in a single query */
    private static final int MAX_FIELDS_PER_QUERY = 100;

    /**
     * Class representing a group of PVs that are polled together
     * with the same bucket and polling period
     */
    private class PollingGroup {
        final String bucket;
        final long periodMs;
        final Set<InfluxDB_PV> pvSet = Collections.synchronizedSet(new HashSet<>());
        ScheduledFuture<?> scheduledTask;

        PollingGroup(String bucket, long periodMs) {
            this.bucket = bucket;
            this.periodMs = periodMs;
        }

        void schedule() {
            scheduledTask = scheduler.scheduleAtFixedRate(
                this::pollGroup,
                0,
                periodMs,
                TimeUnit.MILLISECONDS
            );
        }

        void pollGroup() {
            if (pvSet.isEmpty()) {
                return;
            }

            Map<String, List<InfluxDB_PV>> measurementGroups = new HashMap<>();

            synchronized (pvSet) {
                for (InfluxDB_PV pv : pvSet) {
                    if (Objects.equals(pv.getBucket(), this.bucket)) {
                        measurementGroups
                            .computeIfAbsent(pv.getMeasurement(), k -> new ArrayList<>())
                            .add(pv);
                    }
                }
            }

            for (Map.Entry<String, List<InfluxDB_PV>> entry : measurementGroups.entrySet()) {
                String measurement = entry.getKey();
                List<InfluxDB_PV> pvs = entry.getValue();

                if (!pvs.isEmpty()) {
                    executeOptimizedQuery(bucket, measurement, pvs);
                }
            }
        }

        void executeOptimizedQuery(String bucket, String measurement, List<InfluxDB_PV> pvs) {
            try {
                Set<String> uniqueFields = pvs.stream()
                    .map(InfluxDB_PV::getField)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                if (uniqueFields.isEmpty()) {
                    String flux = String.format("from(bucket: \"%s\") |> range(start: -10s) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\") |> last()",
                        bucket, measurement);

                    executeQueryAndDistributeResults(flux, measurement, pvs);
                    return;
                }

                List<String> fieldsList = new ArrayList<>(uniqueFields);
                Map<InfluxDB_PV, Boolean> processedPVs = new HashMap<>();

                for (int i = 0; i < fieldsList.size(); i += MAX_FIELDS_PER_QUERY) {
                    int end = Math.min(i + MAX_FIELDS_PER_QUERY, fieldsList.size());
                    List<String> batchFields = fieldsList.subList(i, end);

                    String fieldFilter = batchFields.stream()
                        .map(field -> "r._field == \"" + field + "\"")
                        .collect(Collectors.joining(" or "));

                    String flux = String.format("from(bucket: \"%s\") |> range(start: -10s) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\" and (%s)) |> last()",
                        bucket, measurement, fieldFilter);

                    List<InfluxDB_PV> batchPVs = pvs.stream()
                        .filter(pv -> {
                            String field = pv.getField();
                            return field == null || batchFields.contains(field);
                        })
                        .filter(pv -> !processedPVs.getOrDefault(pv, false))
                        .collect(Collectors.toList());

                    if (!batchPVs.isEmpty()) {
                        executeQueryAndDistributeResults(flux, measurement, batchPVs);

                        batchPVs.forEach(pv -> processedPVs.put(pv, true));
                    }
                }

                List<InfluxDB_PV> unprocessedPVs = pvs.stream()
                    .filter(pv -> !processedPVs.getOrDefault(pv, false))
                    .collect(Collectors.toList());

                if (!unprocessedPVs.isEmpty()) {
                    String flux = String.format("from(bucket: \"%s\") |> range(start: -10s) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\") |> last()",
                        bucket, measurement);

                    executeQueryAndDistributeResults(flux, measurement, unprocessedPVs);
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error executing batch query", e);
            }
        }

        private void executeQueryAndDistributeResults(String flux, String measurement, List<InfluxDB_PV> pvs) {
            try {
                QueryApi queryApi = influxClient.getQueryApi();
                List<FluxTable> tables = queryApi.query(flux);

                if (!tables.isEmpty()) {
                    Map<String, FluxRecord> recordsByField = new HashMap<>();
                    for (FluxTable table : tables) {
                        for (FluxRecord record : table.getRecords()) {
                            String field = record.getField();
                            recordsByField.put(field, record);
                        }
                    }

                    for (InfluxDB_PV pv : pvs) {
                        String field = pv.getField();
                        long disconnectTimeoutMs = InfluxDB_Preferences.getInstance().getDisconnectTimeout();

                        if (field != null) {
                            FluxRecord record = recordsByField.get(field);

                            if (record != null) {
                                Duration timeSinceUpdate = Duration.between(Objects.requireNonNull(record.getTime()), Instant.now());

                                if (timeSinceUpdate.toMillis() > disconnectTimeoutMs) {
                                    pv.updateToDisconnectedState();
                                } else {
                                    VType value = InfluxDB_Helper.convertRecordToVType(record);
                                    if (value != null) {
                                        pv.updateValue(value, record.getValue());
                                    }
                                }
                            } else {
                                pv.updateToDisconnectedState();
                            }
                        } else {
                            List<FluxRecord> matchingRecords = tables.stream()
                                .flatMap(table -> table.getRecords().stream())
                                .filter(record -> measurement.equals(record.getMeasurement())).toList();

                            if (!matchingRecords.isEmpty()) {
                                FluxRecord record = matchingRecords.get(0);
                                Duration timeSinceUpdate = Duration.between(Objects.requireNonNull(record.getTime()), Instant.now());

                                if (timeSinceUpdate.toMillis() > disconnectTimeoutMs) {
                                    pv.updateToDisconnectedState();
                                } else {
                                    VType value = InfluxDB_Helper.convertRecordToVType(record);
                                    if (value != null) {
                                        pv.updateValue(value, record.getValue());
                                    }
                                }
                            } else {
                                pv.updateToDisconnectedState();
                            }
                        }
                    }
                } else {
                    for (InfluxDB_PV pv : pvs) {
                        pv.updateToDisconnectedState();
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error executing batch query", e);
                for (InfluxDB_PV pv : pvs) {
                    pv.updateToDisconnectedState();
                }
            }
        }

        void cancel() {
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
                scheduledTask = null;
            }
        }
    }

    /**
     * Private constructor to enforce singleton pattern
     */
    private InfluxDB_PollingManager() {
        influxClient = InfluxDB_Context.getInstance().getClient();
        scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread thread = new Thread(r, "InfluxDB-PV-BatchPoller");
                thread.setDaemon(true);
                return thread;
            }
        );
    }

    private static class Holder {
        private static final InfluxDB_PollingManager INSTANCE = new InfluxDB_PollingManager();
    }

    /**
     * Gets the singleton instance of the polling manager
     *
     * @return the polling manager instance
     */
    public static InfluxDB_PollingManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Registers a PV to be polled by the manager
     *
     * @param pv the InfluxDB PV to register
     */
    public void registerPV(InfluxDB_PV pv) {
        registerPVInternal(pv);
    }

    /**
     * Internal method to register a PV (used for both initial registration and reorganization)
     *
     * @param pv the InfluxDB PV to register
     */
    private void registerPVInternal(InfluxDB_PV pv) {
        String bucket = pv.getBucket();
        if (bucket == null) {
            LOGGER.log(Level.WARNING, "Cannot register PV {0} - no bucket available", pv.getName());
            return;
        }

        String groupKey = createGroupKey(bucket, pv.getPeriod());

        pollingGroups.computeIfAbsent(groupKey, k -> {
            String[] parts = k.split(":");
            String bucketName = parts[0];
            long period = Long.parseLong(parts[1]);

            PollingGroup group = new PollingGroup(bucketName, period);
            group.schedule();
            return group;
        }).pvSet.add(pv);

        pvToGroupKey.put(pv, groupKey);
    }

    /**
     * Unregisters a PV from the manager
     *
     * @param pv the InfluxDB PV to unregister
     */
    public void unregisterPV(InfluxDB_PV pv) {
        String groupKey = pvToGroupKey.remove(pv);
        if (groupKey != null) {
            PollingGroup group = pollingGroups.get(groupKey);
            if (group != null) {
                group.pvSet.remove(pv);
                if (group.pvSet.isEmpty()) {
                    group.cancel();
                    pollingGroups.remove(groupKey);
                }
            }
        }
    }

    /**
     * Creates a group key for the given bucket and period
     * This will group similar periods together to reduce the number of scheduled tasks
     *
     * @param bucket the InfluxDB bucket
     * @param periodMs the polling period in milliseconds
     * @return a key string for grouping
     */
    private String createGroupKey(String bucket, long periodMs) {
        long bucketedPeriod;

        if (periodMs <= 100) {
            bucketedPeriod = periodMs;
        } else if (periodMs <= 1000) {
            bucketedPeriod = Math.round(periodMs / 100.0) * 100;
        } else if (periodMs <= 10000) {
            bucketedPeriod = Math.round(periodMs / 1000.0) * 1000;
        } else {
            bucketedPeriod = Math.round(periodMs / 5000.0) * 5000;
        }

        return bucket + ":" + bucketedPeriod;
    }

    /**
     * Shuts down the polling manager and cancels all polling tasks
     */
    public void shutdown() {
        for (PollingGroup group : pollingGroups.values()) {
            group.cancel();
        }

        pollingGroups.clear();
        pvToGroupKey.clear();
        scheduler.shutdownNow();
    }
}
