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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.VNumber;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.pv.TimeHelper;

/**
 * Simplified iterator for InfluxDBv2 raw data.
 */
public class InfluxValueIterator implements ValueIterator {
    private final InfluxArchiveReader reader;
    private final String name;
    private final String field;
    private final Instant start;
    private final Instant end;
    private final IteratorListener listener;
    private final Iterator<VType> valueIterator;
    private final String meanWindow;
    private boolean closed = false;

    public InfluxValueIterator(InfluxArchiveReader reader, String name, String field,
        Instant start, Instant end, IteratorListener listener,
        String meanWindow) throws Exception {
        this.reader = reader;
        this.name = name;
        this.field = field;
        this.start = start;
        this.end = end;
        this.listener = listener;
        this.meanWindow = meanWindow;

        List<VType> values = fetchAndConvertRecords();
        this.valueIterator = values.iterator();
    }

    public InfluxValueIterator(InfluxArchiveReader reader, String name, String field,
        Instant start, Instant end, IteratorListener listener) throws Exception {
        this(reader, name, field, start, end, listener, null);
    }

    private List<VType> fetchAndConvertRecords() throws Exception {
        List<FluxRecord> records = fetchRecords();
        List<VType> values = new ArrayList<>();

        for (FluxRecord record : records) {
            VType value = createVType(record);
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    private List<FluxRecord> fetchRecords() throws Exception {
        String startISO = DateTimeFormatter.ISO_INSTANT.format(start);
        String endISO = DateTimeFormatter.ISO_INSTANT.format(end);
        String bucket = InfluxPreferences.bucket;
        String flux = buildFluxQuery(bucket, startISO, endISO, name, field, meanWindow);

        try {
            List<FluxTable> tables = reader.queryApi.query(flux);

            if (tables == null || tables.isEmpty()) {
                return new ArrayList<>();
            }

            List<FluxRecord> allRecords = new ArrayList<>();
            for (FluxTable table : tables) {
                allRecords.addAll(table.getRecords());
            }

            allRecords.sort(Comparator.comparing(FluxRecord::getTime));

            return allRecords;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error querying InfluxDB for raw data: " + name, e);
            throw new Exception("Failed to fetch raw data for " + name, e);
        }
    }

    private String buildFluxQuery(String bucket, String start, String stop,
        String measurement, String field, String meanWindow) {

        String fieldFilter;
        if (field != null && !field.isEmpty()) {
            fieldFilter = String.format("r._field == \"%s\"", field);
        } else {
            fieldFilter = discoverFieldFilter(bucket, measurement);
        }

        String baseQuery = String.format(
            "from(bucket: \"%s\") " +
                "|> range(start: %s, stop: %s) " +
                "|> filter(fn: (r) => r._measurement == \"%s\" and %s)",
            bucket, start, stop, measurement, fieldFilter);

        if (meanWindow != null && !meanWindow.trim().isEmpty()) {
            baseQuery += String.format(" |> aggregateWindow(every: %s, fn: mean, createEmpty: false)", meanWindow);
        }

        baseQuery += " |> sort(columns: [\"_time\"])";

        return baseQuery;
    }

    private String discoverFieldFilter(String bucket, String measurement) {
        try {
            String discoveryFlux = String.format(
                """
                import "influxdata/influxdb/schema"
                schema.fieldKeys(bucket: "%s", predicate: (r) => r._measurement == "%s", start: 1970-01-01T00:00:00Z)
                """,
                bucket, measurement);

            List<FluxTable> fieldTables = reader.queryApi.query(discoveryFlux);
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
            logger.log(Level.WARNING, "Error discovering fields, using fallback", e);
            return "r._field == \"value\" or r._field == \"field\"";
        }
    }

    public VType createVType(FluxRecord record) {
        if (record == null) return null;

        Object val = record.getValueByKey("_value");
        Instant timestamp = record.getTime();

        if (timestamp == null) {
            logger.log(Level.WARNING, "Record has null timestamp for PV: " + name);
            return null;
        }

        if (val instanceof String) {
            return VString.of(val.toString(),
                Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "OK"),
                TimeHelper.fromInstant(timestamp));
        }

        if (!(val instanceof Number)) {
            if (val != null) {
                logger.log(Level.FINE, String.format("Non-numeric value for PV %s: %s (%s)",
                    name, val, val.getClass().getSimpleName()));
            }
            return VNumber.of(0.0,
                Alarm.of(AlarmSeverity.INVALID, AlarmStatus.CLIENT, "Invalid data"),
                TimeHelper.fromInstant(timestamp),
                Display.none());
        }

        double numVal = ((Number) val).doubleValue();

        if (Double.isNaN(numVal) || Double.isInfinite(numVal)) {
            return VNumber.of(0.0,
                Alarm.of(AlarmSeverity.INVALID, AlarmStatus.CLIENT, "Invalid numeric value"),
                TimeHelper.fromInstant(timestamp),
                Display.none());
        }

        return VNumber.of(numVal,
            Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "OK"),
            TimeHelper.fromInstant(timestamp),
            Display.none());
    }

    @Override
    public synchronized boolean hasNext() {
        return !closed && valueIterator.hasNext();
    }

    @Override
    public synchronized VType next() {
        if (!hasNext()) return null;
        return valueIterator.next();
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            listener.finished(this);
            logger.log(Level.FINE, "Closed iterator for PV: " + name);
        }
    }
}
