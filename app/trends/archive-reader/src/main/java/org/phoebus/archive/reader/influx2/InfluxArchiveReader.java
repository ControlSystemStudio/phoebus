/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.archive.reader.influx2;

import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.Bucket;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.ui.text.RegExHelper;

/**
 * Archive reader for InfluxDB v2.
 * Uses InfluxDB native capabilities for downsampling and aggregation.
 * Supports automatic bucket detection and all PV name formats.
 */
public class InfluxArchiveReader implements ArchiveReader, IteratorListener {
    private final String baseUrl;
    private final String influxUrl;
    private final InfluxDBClient influxClient;
    protected final QueryApi queryApi;

    private final Map<ValueIterator, InfluxArchiveReader> iterators = Collections.synchronizedMap(
        new WeakHashMap<>());

    private static final Logger logger = Logger.getLogger(InfluxArchiveReader.class.getName());

    public InfluxArchiveReader() {
        this.influxUrl = "influx://" + InfluxPreferences.ip + ":" + InfluxPreferences.port;

        boolean useHttps = InfluxPreferences.useHttps;
        this.baseUrl = useHttps
            ? influxUrl.replace("influx://", "https://")
            : influxUrl.replace("influx://", "http://");

        char[] token = InfluxPreferences.token.toCharArray();
        String org = InfluxPreferences.org;
        this.influxClient = InfluxDBClientFactory.create(baseUrl, token, org);
        this.queryApi = influxClient.getQueryApi();
    }

    public InfluxArchiveReader(final String url) {
        this.influxUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        boolean useHttps = InfluxPreferences.useHttps;
        this.baseUrl = useHttps
            ? influxUrl.replace("influx://", "https://")
            : influxUrl.replace("influx://", "http://");

        char[] token = InfluxPreferences.token.toCharArray();
        String org = InfluxPreferences.org;
        this.influxClient = InfluxDBClientFactory.create(baseUrl, token, org);
        this.queryApi = influxClient.getQueryApi();
    }

    public InfluxArchiveReader(final String url, final String bucket) {
        this(url);
        InfluxPreferences.bucket = bucket;
    }

    public String getURL() {
        return influxUrl;
    }

    @Override
    public String getDescription() {
        return "InfluxDB Archive Reader (Optimized)\n" +
            "Server URL: " + baseUrl + "\n" +
            "Organization: " + InfluxPreferences.org + "\n" +
            "Bucket: " + InfluxPreferences.bucket + "\n";
    }

    @Override
    public List<String> getNamesByPattern(final String globPattern) throws Exception {
        String regex = RegExHelper.fullRegexFromGlob(globPattern);
        String bucket = resolveBucket(null);

        String flux = String.format(
            """
            import "influxdata/influxdb/schema"
            schema.measurements(bucket: "%s", start: 1970-01-01T00:00:00Z)
              |> filter(fn: (r) => r._value =~ /%s/)
              |> limit(n: 1000)""",
            bucket, regex);

        List<FluxTable> tables = queryApi.query(flux);
        List<String> results = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Object value = record.getValueByKey("_value");
                if (value != null) {
                    results.add(value.toString());
                }
            }
        }

        return results;
    }

    /**
     * Checks if a bucket exists in InfluxDB.
     * Used to disambiguate between bucket/measurement and measurement/field formats.
     *
     * @param bucketName the name of the bucket to check
     * @return true if the bucket exists, false otherwise
     */
    private boolean bucketExists(String bucketName) {
        try {
            BucketsApi bucketsApi = influxClient.getBucketsApi();
            List<Bucket> buckets = bucketsApi.findBuckets();

            return buckets.stream()
                .anyMatch(bucket -> bucket.getName().equals(bucketName));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error checking bucket existence: " + bucketName, e);
            return false;
        }
    }

    /**
     * Gets the name of the last created bucket in InfluxDB.
     * Queries the InfluxDB API for all buckets and sorts them by creation date
     * to find the most recently created one.
     *
     * @return The name of the last created bucket, or null if no buckets are found
     */
    public String getLastBucketName() {
        try {
            BucketsApi bucketsApi = influxClient.getBucketsApi();
            List<Bucket> buckets = bucketsApi.findBuckets();

            if (buckets.isEmpty()) {
                logger.log(Level.SEVERE, "No buckets found in InfluxDB");
                return null;
            }

            Optional<Bucket> lastBucket = buckets.stream().max(Comparator.comparing(Bucket::getUpdatedAt));

            return lastBucket.get().getName();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving buckets from InfluxDB", e);
            return null;
        }
    }

    /**
     * Resolves the bucket name to use, handling automatic last bucket detection.
     *
     * @param specifiedBucket the bucket specified in the PV name, or null for auto-detection
     * @return the resolved bucket name
     * @throws Exception if no bucket can be resolved
     */
    private String resolveBucket(String specifiedBucket) throws Exception {
        if (specifiedBucket != null && !specifiedBucket.isEmpty()) {
            return specifiedBucket;
        }

        String lastBucket = getLastBucketName();
        if (lastBucket == null) {
            throw new Exception("No bucket available - no default bucket configured and cannot determine last bucket");
        }

        return lastBucket;
    }

    /**
     * Parses a PV name to extract measurement and field information.
     * Supports automatic bucket detection for all PV name formats:
     * - influx://measurement (uses last bucket, no specific field)
     * - influx://measurement/field (uses last bucket, specific field)
     * - influx://bucket/measurement (specific bucket, no specific field)
     * - influx://bucket/measurement/field (specific bucket, specific field)
     *
     * @param name the PV name to parse
     * @return a map containing bucket, measurement, and optionally field
     * @throws Exception if the PV name format is invalid or bucket cannot be resolved
     */
    protected Map<String, String> parsePvName(String name) throws Exception {
        Map<String, String> result = new HashMap<>();
        name = name.replace("influx://", "");

        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        String[] parts = name.split("/");

        String resolvedBucket = null;
        String resolvedMeasurement;
        String resolvedField = null;
        boolean needsLastBucket = false;

        switch (parts.length) {
            case 1:
                needsLastBucket = true;
                resolvedMeasurement = parts[0];
                break;
            case 2:
                if (bucketExists(parts[0])) {
                    resolvedBucket = parts[0];
                    resolvedMeasurement = parts[1];
                } else {
                    needsLastBucket = true;
                    resolvedMeasurement = parts[0];
                    resolvedField = parts[1];
                }
                break;
            case 3:
                resolvedBucket = parts[0];
                resolvedMeasurement = parts[1];
                resolvedField = Objects.equals(parts[2], "null") ? null : parts[2];
                break;
            default:
                throw new IllegalArgumentException("Invalid InfluxDB PV format: " + name);
        }

        if (needsLastBucket || resolvedBucket == null) {
            resolvedBucket = resolveBucket(null);
        } else {
            resolvedBucket = resolveBucket(resolvedBucket);
        }

        result.put("bucket", resolvedBucket);
        result.put("measurement", resolvedMeasurement);
        if (resolvedField != null) {
            result.put("field", resolvedField);
        }

        return result;
    }

    @Override
    public ValueIterator getRawValues(String name, Instant start, Instant end) throws Exception {
        return getRawValues(name, start, end, null);
    }

    /**
     * Gets raw values for a PV within the specified time range.
     *
     * @param name the PV name
     * @param start the start time
     * @param end the end time
     * @param mean the mean window for aggregation, or null for raw data
     * @return a ValueIterator for the requested data
     * @throws Exception if the PV cannot be found or accessed
     */
    public ValueIterator getRawValues(String name, Instant start, Instant end, String mean) throws Exception {
        Map<String, String> pvInfo = parsePvName(name);
        String measurement = pvInfo.get("measurement");
        String field = pvInfo.get("field");
        String bucket = pvInfo.get("bucket");

        String originalBucket = InfluxPreferences.bucket;
        InfluxPreferences.bucket = bucket;

        InfluxValueIterator it;
        try {
            it = new InfluxValueIterator(this, measurement, field, start, end, this, mean);
        } catch (Exception e) {
            throw new UnknownChannelException(name);
        } finally {
            InfluxPreferences.bucket = originalBucket;
        }

        iterators.put(it, this);
        return it;
    }

    /**
     * Gets the last recorded point for a given PV.
     *
     * @param name the PV name
     * @return the last FluxRecord, or null if not found
     */
    public FluxRecord getLastPoint(String name) {
        try {
            Map<String, String> pvInfo = parsePvName(name);
            String bucket = pvInfo.get("bucket");
            String measurement = pvInfo.get("measurement");
            String field = pvInfo.get("field");

            String fieldFilter = field != null ?
                String.format(" and r._field == \"%s\"", field) : "";

            String flux = String.format(
                "from(bucket: \"%s\") " +
                    "|> range(start: 0) " +
                    "|> filter(fn: (r) => r._measurement == \"%s\"%s) " +
                    "|> last()",
                bucket, measurement, fieldFilter);

            List<FluxTable> tables = queryApi.query(flux);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    return record;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting last point for " + name, e);
        }

        return null;
    }

    @Override
    public ValueIterator getOptimizedValues(String name, Instant start, Instant end, int count) throws Exception {
        Map<String, String> pvInfo = parsePvName(name);
        String measurement = pvInfo.get("measurement");
        String field = pvInfo.get("field");
        String bucket = pvInfo.get("bucket");

        String originalBucket = InfluxPreferences.bucket;
        InfluxPreferences.bucket = bucket;

        InfluxDecimatedValueIterator it;
        try {
            it = new InfluxDecimatedValueIterator(this, measurement, field, start, end, count, this);
        } catch (Exception e) {
            throw new UnknownChannelException(name);
        } finally {
            InfluxPreferences.bucket = originalBucket;
        }

        iterators.put(it, this);
        return it;
    }

    /**
     * Calculates statistics for a PV within the specified time range.
     *
     * @param name the PV name
     * @param start the start time
     * @param end the end time
     * @return a map containing statistical values (count, sum, mean, stdDev, min, max)
     */
    public Map<String, Double> getStatistics(String name, Instant start, Instant end) {
        try {
            Map<String, String> pvInfo = parsePvName(name);
            String measurement = pvInfo.get("measurement");
            String field = pvInfo.get("field");
            String bucket = pvInfo.get("bucket");

            String fieldFilter = buildFieldFilter(bucket, measurement, field);

            if (fieldFilter == null) {
                logger.log(Level.WARNING, "No valid field found for measurement: " + measurement);
                return null;
            }

            String quickCheckFlux = String.format(
                """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "%s" and %s)
                  |> limit(n: 1)
                  |> yield(name: "check")
                """,
                bucket, start.toString(), end.toString(), measurement, fieldFilter);

            List<FluxTable> quickCheckTables;
            try {
                quickCheckTables = queryApi.query(quickCheckFlux);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Quick check query failed for " + name + ": " + e.getMessage());
                return null;
            }

            if (quickCheckTables.isEmpty() || quickCheckTables.get(0).getRecords().isEmpty()) {
                logger.log(Level.FINE, "No data found for " + name + " in time range");
                return createEmptyStats();
            }

            FluxRecord firstRecord = quickCheckTables.get(0).getRecords().get(0);
            Object value = firstRecord.getValueByKey("_value");

            if (value instanceof String) {
                logger.log(Level.FINE, "Skipping statistics for string data: " + name);
                return null;
            }

            String basicStatsFlux = buildBasicStatsFlux(bucket, start, end, measurement, fieldFilter);

            Map<String, Double> stats = createEmptyStats();

            try {
                List<FluxTable> basicStatsTables = queryApi.query(basicStatsFlux);
                processBasicStatsResults(basicStatsTables, stats);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Basic statistics query failed for " + name + ": " + e.getMessage());
                return stats;
            }

            try {
                String medianFlux = buildMedianFlux(bucket, start, end, measurement, fieldFilter);
                List<FluxTable> medianTables = queryApi.query(medianFlux);
                processMedianResults(medianTables, stats);
            } catch (Exception medianEx) {
                logger.log(Level.FINE, "Median calculation failed for " + name + ": " + medianEx.getMessage());
            }

            return stats;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error calculating statistics for " + name + ": " + e.getMessage());
            return createEmptyStats();
        }
    }

    private Map<String, Double> createEmptyStats() {
        Map<String, Double> stats = new HashMap<>();
        stats.put("count", 0.0);
        stats.put("sum", 0.0);
        stats.put("mean", 0.0);
        stats.put("stdDev", 0.0);
        stats.put("min", 0.0);
        stats.put("max", 0.0);
        stats.put("median", 0.0);
        return stats;
    }

    private String buildBasicStatsFlux(String bucket, Instant start, Instant end,
        String measurement, String fieldFilter) {
        return String.format(
            """
            import "types"
            
            data = from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r._measurement == "%s" and %s)
              |> filter(fn: (r) => types.isType(v: r._value, type: "float") or types.isType(v: r._value, type: "int"))
              |> limit(n: 10000)
            
            count = data |> count() |> map(fn: (r) => ({stat: "count", value: float(v: r._value)}))
            sum = data |> sum() |> map(fn: (r) => ({stat: "sum", value: r._value}))
            mean = data |> mean() |> map(fn: (r) => ({stat: "mean", value: r._value}))
            stdDev = data |> stddev() |> map(fn: (r) => ({stat: "stdDev", value: r._value}))
            min = data |> min() |> map(fn: (r) => ({stat: "min", value: r._value}))
            max = data |> max() |> map(fn: (r) => ({stat: "max", value: r._value}))
            
            union(tables: [count, sum, mean, stdDev, min, max])
              |> yield(name: "stats")
            """,
            bucket, start, end, measurement, fieldFilter);
    }

    private String buildMedianFlux(String bucket, Instant start, Instant end,
        String measurement, String fieldFilter) {
        return String.format(
            """
            import "types"
            
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r._measurement == "%s" and %s)
              |> filter(fn: (r) => types.isType(v: r._value, type: "float") or types.isType(v: r._value, type: "int"))
              |> limit(n: 10000)
              |> median(method: "exact_mean")
              |> map(fn: (r) => ({stat: "median", value: r._value}))
              |> yield(name: "median")
            """,
            bucket, start, end, measurement, fieldFilter);
    }

    private void processBasicStatsResults(List<FluxTable> tables, Map<String, Double> stats) {
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String statName = (String) record.getValueByKey("stat");
                Object statValue = record.getValueByKey("value");

                if (statName != null && statValue instanceof Number) {
                    stats.put(statName, ((Number) statValue).doubleValue());
                }
            }
        }
    }

    private void processMedianResults(List<FluxTable> tables, Map<String, Double> stats) {
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String statName = (String) record.getValueByKey("stat");
                Object statValue = record.getValueByKey("value");

                if ("median".equals(statName) && statValue instanceof Number) {
                    stats.put("median", ((Number) statValue).doubleValue());
                    break;
                }
            }
        }
    }

    private String buildFieldFilter(String bucket, String measurement, String specifiedField) {
        try {
            if (specifiedField != null && !specifiedField.isEmpty()) {
                return String.format("r._field == \"%s\"", specifiedField);
            }

            String discoveryFlux = String.format(
                """
                import "influxdata/influxdb/schema"
                schema.fieldKeys(bucket: "%s", predicate: (r) => r._measurement == "%s", start: 1970-01-01T00:00:00Z)
                """,
                bucket, measurement);

            List<FluxTable> fieldTables = queryApi.query(discoveryFlux);
            List<String> availableFields = new ArrayList<>();

            for (FluxTable table : fieldTables) {
                for (FluxRecord record : table.getRecords()) {
                    Object fieldName = record.getValueByKey("_value");
                    if (fieldName != null) {
                        availableFields.add(fieldName.toString());
                    }
                }
            }

            if (availableFields.contains("value")) {
                return "r._field == \"value\"";
            } else if (availableFields.contains("field")) {
                return "r._field == \"field\"";
            } else if (!availableFields.isEmpty()) {
                String firstField = availableFields.get(0);
                return String.format("r._field == \"%s\"", firstField);
            }

            return "true";

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error discovering fields for measurement " + measurement, e);
            return "r._field == \"value\" or r._field == \"field\"";
        }
    }

    @Override
    public void cancel() {
        for (ValueIterator it : iterators.keySet().toArray(new ValueIterator[0])) {
            try {
                it.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing iterator", e);
            }
        }
    }

    @Override
    public void close() {
        cancel();
        if (influxClient != null) {
            influxClient.close();
            logger.log(Level.INFO, "InfluxDB client closed");
        }
    }

    @Override
    public void finished(Object source) {
        if (source instanceof ValueIterator) {
            iterators.remove(source);
        }
    }
}