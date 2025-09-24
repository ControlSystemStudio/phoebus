/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.influx2;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;

/**
 * Represents a read-only Process Variable (PV) backed by InfluxDB.
 * <p>
 * This class uses the InfluxDBPollingManager to efficiently poll InfluxDB
 * by batching requests with similar periods and the same bucket.
 * <p>
 * Write operations are not supported.
 */
public class InfluxDB_PV extends PV {
    /** The name of the InfluxDB bucket */
    private final String bucket;

    /** The name of the measurement to read from */
    private final String measurement;

    /** The field to read from (can be null for default) */
    private final String field;

    /** Period of sampling in milliseconds */
    private final long period;

    /** The InfluxDB client used for querying */
    private final InfluxDBClient influxClient;

    /** Timestamp of PV last update. */
    private Instant lastUpdate;

    /** Object */
    private Object lastValue;

    /**
     * Constructs an {@code InfluxDB_PV} with the specified name and base_name.
     * The base_name is expected to be in one of the following forms:
     * - {@code influx2://bucket/measurement} - specific bucket, no specific field
     * - {@code influx2://bucket/measurement/field} - specific bucket, specific field
     * <p>
     * The bucket must always be specified explicitly.
     *
     * @param name      the name of the PV
     * @param base_name the base URI specifying the InfluxDB bucket, measurement, optional field
     * @throws IllegalArgumentException if the format is invalid or bucket is missing
     */
    public InfluxDB_PV(String name, String base_name) {
        super(name);

        String new_base_name = base_name.replace("influx2://", "");
        String[] parts = new_base_name.split("/");

        influxClient = InfluxDB_Context.getInstance().getClient();
        lastUpdate = Instant.now();

        switch (parts.length) {
            case 2:
                this.bucket = parts[0];
                this.measurement = parts[1];
                this.field = null;
                break;
            case 3:
                this.bucket = parts[0];
                this.measurement = parts[1];
                this.field = Objects.equals(parts[2], "null") ? null : parts[2];
                break;
            default:
                throw new IllegalArgumentException(
                    "Invalid InfluxDB PV format: " + base_name +
                        ". Expected format: influx2://bucket/measurement or influx2://bucket/measurement/field"
                );
        }
        if (this.bucket == null || this.bucket.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Bucket must be specified in InfluxDB PV: " + base_name
            );
        }

        InfluxDB_Preferences prefs = InfluxDB_Preferences.getInstance();
        period = prefs.getRefreshPeriod();

        logger.log(Level.INFO, "Creating InfluxDB PV with bucket: {0}, measurement: {1}, field: {2}, period: {3}ms",
            new Object[]{bucket, measurement, field != null ? field : "all fields", period});

        InfluxDB_PollingManager.getInstance().registerPV(this);
    }

    /**
     * Get the bucket for this PV
     * @return the bucket name
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Get the measurement for this PV
     * @return the measurement name
     */
    public String getMeasurement() {
        return measurement;
    }

    /**
     * Get the field for this PV
     * @return the field name or null if no specific field
     */
    public String getField() {
        return field;
    }

    /**
     * Get the polling period for this PV
     * @return the period in milliseconds
     */
    public long getPeriod() {
        return period;
    }

    protected void updateValue(VType value, Object plainValue) {
        if (value != null && plainValue != null) {
            lastUpdate = Instant.now();
            lastValue = plainValue;
        }

        notifyListenersOfValue(value);
    }

    /**
     * Update the PV to a disconnected state with an alarm
     */
    protected void updateToDisconnectedState() {
        VType disconnected;

        if (lastValue == null) {
            disconnected = VString.of(
                "Disconnected",
                Alarm.of(AlarmSeverity.INVALID, AlarmStatus.CLIENT, "Disconnected"),
                Time.of(lastUpdate != null ? lastUpdate : Instant.now())
            );
        } else {
            VType converted = VType.toVType(lastValue, Alarm.noValue(), Time.of(lastUpdate), Display.none());

            disconnected = Objects.requireNonNullElseGet(converted, () -> VString.of(
                "Disconnected: " + lastValue.toString(),
                Alarm.of(AlarmSeverity.INVALID, AlarmStatus.CLIENT, "Disconnected"),
                Time.of(lastUpdate)
            ));
        }

        notifyListenersOfValue(disconnected);
    }

    /**
     * Asynchronously queries InfluxDB for the most recent value of the configured measurement.
     * <p>
     * This method can be used for on-demand reading, but normal updates come through
     * the PollingManager.
     *
     * @return a future that completes with the latest {@link VType} value
     */
    @Override
    public CompletableFuture<VType> asyncRead() {
        CompletableFuture<VType> future = new CompletableFuture<>();

        String flux;
        if (field != null) {
            flux = String.format("from(bucket: \"%s\") |> range(start: -10s) " +
                    "|> filter(fn: (r) => r._measurement == \"%s\" and r._field == \"%s\") |> last()",
                bucket, measurement, field);
        } else {
            flux = String.format("from(bucket: \"%s\") |> range(start: -10s) " +
                    "|> filter(fn: (r) => r._measurement == \"%s\") |> last()",
                bucket, measurement);
        }

        QueryApi queryApi = influxClient.getQueryApi();

        try {
            List<FluxTable> tables = queryApi.query(flux);

            if (tables.isEmpty()) {
                future.completeExceptionally(new Exception("No data returned from InfluxDB"));
            } else {
                FluxTable table = tables.get(0);
                if (table.getRecords().isEmpty()) {
                    future.completeExceptionally(new Exception("No records in the result from InfluxDB"));
                } else {
                    FluxRecord record = table.getRecords().get(0);
                    VType value = InfluxDB_Helper.convertRecordToVType(record);
                    notifyListenersOfValue(value);
                    future.complete(value);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Query error for PV: " + getName(), e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Write operation is not supported for InfluxDB PVs.
     *
     * @param new_value the value to write (ignored)
     * @throws UnsupportedOperationException always
     */
    @Override
    public void write(Object new_value) {
        throw new UnsupportedOperationException("Write not supported for InfluxDB PV");
    }

    /**
     * Asynchronous write is not supported for InfluxDB PVs.
     *
     * @param new_value the value to write (ignored)
     * @return nothing, always throws exception
     * @throws UnsupportedOperationException always
     */
    @Override
    public CompletableFuture<?> asyncWrite(Object new_value) {
        throw new UnsupportedOperationException("Async write not supported for InfluxDB PV");
    }

    /**
     * Unregisters this PV from the polling manager.
     * Called when the PV is being closed.
     */
    @Override
    protected void close() {
        InfluxDB_PollingManager.getInstance().unregisterPV(this);
    }
}
