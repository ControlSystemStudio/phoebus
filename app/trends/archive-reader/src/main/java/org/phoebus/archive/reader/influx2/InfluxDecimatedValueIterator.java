/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.archive.reader.influx2;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.VNumber;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.pv.TimeHelper;

/**
 * Value iterator for decimated numeric data from InfluxDB.
 * Uses InfluxDB's native aggregateWindow function to decimate data on the server side.
 * This approach is much more efficient for large datasets.
 */
public class InfluxDecimatedValueIterator implements ValueIterator {
    private final InfluxArchiveReader reader;
    private final String name;
    private final String field;
    private final Instant start;
    private final Instant end;
    private final int maxPoints;
    private final IteratorListener listener;
    private final Iterator<VType> valueIterator;
    private boolean closed = false;

    private static final double MIN_EPSILON = 0.001;
    private static final double MAX_EPSILON = 100.0;
    private static final double BASE_EPSILON = 0.1;
    private static final long ZOOM_THRESHOLD_SECONDS = 3600;
    private static final double EPSILON_SCALE_FACTOR = 2.0;

    public InfluxDecimatedValueIterator(InfluxArchiveReader reader, String name, String field,
        Instant start, Instant end, int maxPoints,
        IteratorListener listener) {
        this.reader = reader;
        this.name = name;
        this.field = field;
        this.start = start;
        this.end = end;
        this.maxPoints = maxPoints;
        this.listener = listener;

        List<FluxRecord> records = fetchDecimatedRecords();
        List<VType> values = convertRecordsToVTypes(records);

        this.valueIterator = values.iterator();
    }

    private List<FluxRecord> fetchDecimatedRecords() {
        String startISO = DateTimeFormatter.ISO_INSTANT.format(start);
        String endISO = DateTimeFormatter.ISO_INSTANT.format(end);
        String bucket = InfluxPreferences.bucket;

        double epsilon = calculateDynamicEpsilon();

        String flux = buildFluxQuery(bucket, startISO, endISO, name, field, epsilon);

        try {
            List<FluxTable> tables = reader.queryApi.query(flux);

            if (tables == null || tables.isEmpty()) {
                logger.log(Level.INFO, String.format("No data returned from RDP query for %s with epsilon %.6f", name, epsilon));
                return new ArrayList<>();
            }

            List<FluxRecord> allRecords = new ArrayList<>();
            for (FluxTable table : tables) {
                allRecords.addAll(table.getRecords());
            }

            allRecords.sort(Comparator.comparing(FluxRecord::getTime));

            logger.log(Level.FINE, String.format("RDP decimation for %s: %d points with epsilon %.6f (time range: %s)",
                name, allRecords.size(), epsilon, Duration.between(start, end)));

            return allRecords;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error querying InfluxDB for PV " + name, e);
            return new ArrayList<>();
        }
    }

    /**
     * Calculates dynamic epsilon based on zoom level, data density, and value range
     */
    private double calculateDynamicEpsilon() {
        Duration timeRange = Duration.between(start, end);
        long timeRangeSeconds = timeRange.getSeconds();

        double zoomFactor = calculateZoomFactor(timeRangeSeconds);
        double baseEpsilon = BASE_EPSILON * zoomFactor;

        double densityFactor = calculateDensityFactor(timeRangeSeconds);
        double densityAdjustedEpsilon = baseEpsilon * densityFactor;

        double rangeAdjustedEpsilon = adjustEpsilonForDataRange(densityAdjustedEpsilon);

        double finalEpsilon = Math.max(MIN_EPSILON, Math.min(MAX_EPSILON, rangeAdjustedEpsilon));

        logger.log(Level.FINE, String.format(
            "Dynamic epsilon calculation for %s: timeRange=%ds, zoomFactor=%.3f, densityFactor=%.3f, finalEpsilon=%.6f",
            name, timeRangeSeconds, zoomFactor, densityFactor, finalEpsilon));

        return finalEpsilon;
    }

    /**
     * Calculates zoom factor based on time range
     */
    private double calculateZoomFactor(long timeRangeSeconds) {
        if (timeRangeSeconds <= 60) {
            return 0.01;
        } else if (timeRangeSeconds <= 300) {
            return 0.05;
        } else if (timeRangeSeconds <= 900) {
            return 0.1;
        } else if (timeRangeSeconds <= ZOOM_THRESHOLD_SECONDS) {
            double ratio = (double) timeRangeSeconds / ZOOM_THRESHOLD_SECONDS;
            return 0.1 + (0.9 * ratio);
        } else if (timeRangeSeconds <= 86400) {
            double ratio = (double) timeRangeSeconds / 86400.0;
            return 1.0 + (ratio * EPSILON_SCALE_FACTOR);
        } else if (timeRangeSeconds <= 604800) {
            double ratio = (double) timeRangeSeconds / 604800.0;
            return 3.0 + (ratio * EPSILON_SCALE_FACTOR * 2);
        } else {
            return Math.min(10.0, 7.0 + Math.log10(timeRangeSeconds / 604800.0));
        }
    }

    /**
     * Calculates density factor based on expected data points vs desired points
     */
    private double calculateDensityFactor(long timeRangeSeconds) {
        if (timeRangeSeconds <= maxPoints) {
            return 0.5;
        } else {
            double ratio = (double) timeRangeSeconds / maxPoints;
            return Math.min(5.0, Math.log(ratio) + 1.0);
        }
    }

    /**
     * Adjusts epsilon based on data value range characteristics
     * This method queries a sample of the data to understand value distribution
     */
    private double adjustEpsilonForDataRange(double baseEpsilon) {
        try {
            String bucket = InfluxPreferences.bucket;
            String startISO = DateTimeFormatter.ISO_INSTANT.format(start);
            String endISO = DateTimeFormatter.ISO_INSTANT.format(end);

            String fieldFilter = field != null ?
                String.format(" and r._field == \"%s\"", field) : "";

            String rangeQuery = String.format(
                """
                import "types"
                
                data = from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "%s"%s)
                  |> filter(fn: (r) => types.isType(v: r._value, type: "float") or types.isType(v: r._value, type: "int"))
                  |> sample(n: 1000)
                
                min_val = data |> min() |> map(fn: (r) => ({stat: "min", value: r._value}))
                max_val = data |> max() |> map(fn: (r) => ({stat: "max", value: r._value}))
                mean_val = data |> mean() |> map(fn: (r) => ({stat: "mean", value: r._value}))
                stddev_val = data |> stddev() |> map(fn: (r) => ({stat: "stddev", value: r._value}))
                
                union(tables: [min_val, max_val, mean_val, stddev_val])
                """,
                bucket, startISO, endISO, name, fieldFilter);

            List<FluxTable> rangeTables = reader.queryApi.query(rangeQuery);

            double minValue = Double.NaN;
            double maxValue = Double.NaN;
            double meanValue = Double.NaN;
            double stddevValue = Double.NaN;

            for (FluxTable table : rangeTables) {
                for (FluxRecord record : table.getRecords()) {
                    String statName = (String) record.getValueByKey("stat");
                    Object statValue = record.getValueByKey("value");

                    if (statValue instanceof Number number) {
                        switch (Objects.requireNonNull(statName)) {
                            case "min" -> minValue = number.doubleValue();
                            case "max" -> maxValue = number.doubleValue();
                            case "mean" -> meanValue = number.doubleValue();
                            case "stddev" -> stddevValue = number.doubleValue();
                        }
                    }
                }
            }

            if (!Double.isNaN(minValue) && !Double.isNaN(maxValue)) {
                double valueRange = Math.abs(maxValue - minValue);

                if (valueRange > 0) {
                    double rangeBasedEpsilon = valueRange * 0.001;

                    if (!Double.isNaN(stddevValue) && stddevValue > 0) {
                        double noiseBasedEpsilon = stddevValue * 0.1;
                        rangeBasedEpsilon = Math.min(rangeBasedEpsilon, noiseBasedEpsilon);
                    }

                    double combinedEpsilon = Math.sqrt(baseEpsilon * rangeBasedEpsilon);

                    logger.log(Level.FINE, String.format(
                        "Range-adjusted epsilon for %s: range=%.3f, stddev=%.3f, base=%.6f, range-based=%.6f, final=%.6f",
                        name, valueRange, stddevValue, baseEpsilon, rangeBasedEpsilon, combinedEpsilon));

                    return combinedEpsilon;
                }
            }

        } catch (Exception e) {
            logger.log(Level.FINE, "Could not determine data range for epsilon adjustment: " + e.getMessage());
        }

        return baseEpsilon;
    }

    /**
     * Builds the Flux query with dynamic RDP epsilon
     */
    private String buildFluxQuery(String bucket, String start, String stop,
        String measurement, String field, double epsilon) {

        String fieldFilter = field != null ?
            String.format(" and r._field == \"%s\"", field) : "";

        String baseFilter = String.format(
            "from(bucket: \"%s\") " +
                "|> range(start: %s, stop: %s) " +
                "|> filter(fn: (r) => r._measurement == \"%s\"%s)",
            bucket, start, stop, measurement, fieldFilter);

        return buildRDPQuery(baseFilter, epsilon);
    }

    /**
     * Builds RDP query with specified epsilon
     */
    private String buildRDPQuery(String baseFilter, double epsilon) {
        return String.format(
            "import \"experimental/polyline\" " +
                "%s " +
                "|> polyline.rdp(epsilon: %.6f)",
            baseFilter, epsilon);
    }

    /**
     * Converts FluxRecords to VType objects
     */
    private List<VType> convertRecordsToVTypes(List<FluxRecord> records) {
        List<VType> values = new ArrayList<>();

        for (FluxRecord record : records) {
            VType value = createVType(record);
            if (value != null) {
                values.add(value);
            }
        }

        logger.log(Level.FINE, String.format("Converted %d FluxRecords to %d VTypes for %s",
            records.size(), values.size(), name));

        return values;
    }

    /**
     * Creates a VType from a FluxRecord
     */
    private VType createVType(FluxRecord record) {
        Object val = record.getValueByKey("_value");
        Instant timestamp = record.getTime();

        if (timestamp == null) {
            logger.log(Level.WARNING, "Record has null timestamp");
            return null;
        }

        if (!(val instanceof Number)) {
            logger.log(Level.FINE, "Non-numeric value: " + val);
            return VNumber.of(0.0,
                Alarm.of(AlarmSeverity.INVALID, AlarmStatus.CLIENT, "Invalid data"),
                TimeHelper.fromInstant(timestamp),
                Display.none());
        }

        return VNumber.of((Number) val,
            Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "OK"),
            TimeHelper.fromInstant(timestamp),
            Display.none());
    }

    @Override
    public boolean hasNext() {
        return !closed && valueIterator.hasNext();
    }

    @Override
    public VType next() {
        if (!hasNext()) return null;
        return valueIterator.next();
    }

    @Override
    public void close() {
        closed = true;
        listener.finished(this);
    }
}
