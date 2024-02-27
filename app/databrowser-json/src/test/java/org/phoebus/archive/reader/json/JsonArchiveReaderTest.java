/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.json;

import org.epics.util.stats.Range;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.junit.jupiter.api.Test;
import org.phoebus.archive.reader.UnknownChannelException;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link JsonArchiveReader}.
 */
public class JsonArchiveReaderTest extends HttpServerTestBase {

    /**
     * Tests the {@link JsonArchiveReader#cancel()} method.
     */
    @Test
    public void cancel() {
        final var channel_name = "some-channel";
        final var start = Instant.ofEpochMilli(123L);
        final var end = Instant.ofEpochMilli(456L);
        final var preferences = new JsonArchivePreferences(true);
        // We need two samples, so that we can cancel the iterator after
        // retrieving the first one.
        final var samples_json = """
                [ {
                   "time" : 123457000001,
                   "severity" : {
                     "level" : "OK",
                     "hasValue" : true
                   },
                   "status" : "NO_ALARM",
                   "quality" : "Original",
                   "metaData" : {
                     "type" : "numeric",
                     "precision" : 3,
                     "units" : "mA",
                     "displayLow" : 0.0,
                     "displayHigh" : 300.0,
                     "warnLow" : 5.0,
                     "warnHigh" : 100.0,
                     "alarmLow" : 2.0,
                     "alarmHigh" : "NaN"
                   },
                   "type" : "double",
                   "value" : [ 27.2, 48.3 ]
                 }, {
                   "time_modified" : 123457000002,
                   "severity" : {
                     "level" : "MAJOR",
                     "hasValue" : true
                   },
                   "status" : "TEST_STATUS",
                   "quality" : "Original",
                   "metaData" : {
                     "type" : "numeric",
                     "precision" : 3,
                     "units" : "mA",
                     "displayLow" : 0.0,
                     "displayHigh" : 300.0,
                     "warnLow" : 5.0,
                     "warnHigh" : 100.0,
                     "alarmLow" : 2.0,
                     "alarmHigh" : "NaN"
                   },
                   "type" : "double",
                   "value" : [ 31.9 ]
                 } ]
                """;
        withSamples(
                1, channel_name, samples_json, (base_url) -> {
                    try (
                            final var reader = new JsonArchiveReader(
                                    "json:" + base_url, preferences);
                            final var iterator = reader.getRawValues(
                                    channel_name, start, end
                            )
                    ) {
                        // Retrieve the first sample.
                        iterator.next();
                        // Cancel all iterators.
                        reader.cancel();
                        // Now, hasNext() should return false.
                        assertFalse(iterator.hasNext());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Tests creating a {@link JsonArchiveReader} with an archive key which
     * does not specify a valid archive on the archive server.
     */
    @Test
    public void createWithInvalidArchiveKey() {
        final var archive_info_json = """
                [ {
                  "key" : 2,
                  "name" : "Some name",
                  "description" : "Some description"
                } ]
                """;
        withArchiveInfo(archive_info_json, (base_url) -> {
                assertThrows(IllegalArgumentException.class, () -> {
                    new JsonArchiveReader(
                            "json:" + base_url,
                            new JsonArchivePreferences(true));
                });
        });
    }

    /**
     * Tests creating a {@link JsonArchiveReader} with a base URL that is
     * invalid (does not start with <code>json:</code>).
     */
    @Test
    public void createWithInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            new JsonArchiveReader(
                    "http://invalid.example.com",
                    new JsonArchivePreferences(true));
        });
    }

    /**
     * Tests the {@link JsonArchiveReader#getDescription()} function.
     */
    @Test
    public void getDescription() {
        var archive_info_json = """
                [ {
                  "key" : 1,
                  "name" : "Some name",
                  "description" : "Some description"
                } ]
                """;
        final var preferences = new JsonArchivePreferences(true);
        withArchiveInfo(archive_info_json, (base_url) -> {
                    try (final var reader = new JsonArchiveReader(
                            "json:" + base_url, preferences)) {
                        assertEquals(
                                "Some description", reader.getDescription());
                    }
                });
        archive_info_json = """
                [ {
                   "key" : 1,
                   "name" : "Some name",
                   "description" : "Another description"
                 }, {
                   "key" : 3,
                   "name" : "Some name",
                   "description" : "Yet another description"
                } ]
                """;
        withArchiveInfo(archive_info_json, (base_url) -> {
                    try (final var reader = new JsonArchiveReader(
                            "json:" + base_url, preferences)) {
                        assertEquals(
                                "Another description", reader.getDescription());
                    }
                    try (final var reader = new JsonArchiveReader(
                            "json:" + base_url + ";key=3", preferences)) {
                        assertEquals(
                                "Yet another description",
                                reader.getDescription());
                    }
                });
    }

    /**
     * Tests the {@link
     * JsonArchiveReader#getOptimizedValues(String, Instant, Instant, int)}
     * function.
     */
    @Test
    public void getOptimizedValues() {
        final var samples_json = """
                [ {
                  "time" : 123,
                  "severity" : {
                    "level" : "OK",
                    "hasValue" : true
                  },
                  "status" : "NO_ALARM",
                  "quality" : "Interpolated",
                  "metaData" : {
                    "type" : "numeric",
                    "precision" : 1,
                    "units" : "V",
                    "displayLow" : -100.0,
                    "displayHigh" : 100.0,
                    "warnLow" : "NaN",
                    "warnHigh" : "NaN",
                    "alarmLow" : "NaN",
                    "alarmHigh" : "NaN"
                  },
                  "type" : "minMaxDouble",
                  "value" : [ -5.0, -1.2 ],
                  "minimum" : -15.1,
                  "maximum" : 2.7
                }, {
                  "time" : 456,
                  "severity" : {
                    "level" : "OK",
                    "hasValue" : true
                  },
                  "status" : "NO_ALARM",
                  "quality" : "Interpolated",
                  "metaData" : {
                    "type" : "numeric",
                    "precision" : 1,
                    "units" : "V",
                    "displayLow" : -100.0,
                    "displayHigh" : 100.0,
                    "warnLow" : "NaN",
                    "warnHigh" : "NaN",
                    "alarmLow" : "NaN",
                    "alarmHigh" : "NaN"
                  },
                  "type" : "minMaxDouble",
                  "value" : [ 4.7 ],
                  "minimum" : -3.9,
                  "maximum" : 17.1
                } ]
                """;
        final var channel_name = "double-channel";
        final var start = Instant.ofEpochMilli(0L);
        final var end = Instant.ofEpochMilli(1L);
        final var preferences = new JsonArchivePreferences(true);
        var requests = withSamples(
                7, channel_name, samples_json, (base_url) -> {
                    try (
                            final var reader = new JsonArchiveReader(
                                    "json:" + base_url + ";key=7",
                                    preferences);
                            final var iterator = reader.getOptimizedValues(
                                    channel_name, start, end, 10)
                    ) {
                        // Check the first sample. The statistics VType does
                        // not support arrays, so we expect a VDoubleArray.
                        final var double_array = (VDoubleArray) iterator.next();
                        assertEquals(2, double_array.getData().size());
                        assertEquals(
                                -5.0, double_array.getData().getDouble(0));
                        assertEquals(
                                -1.2, double_array.getData().getDouble(1));
                        assertEquals(
                                "NO_ALARM", double_array.getAlarm().getName());
                        assertEquals(
                                AlarmSeverity.NONE,
                                double_array.getAlarm().getSeverity());
                        assertEquals(
                                Range.undefined(),
                                double_array.getDisplay().getAlarmRange());
                        assertEquals(
                                Range.undefined(),
                                double_array.getDisplay().getControlRange()
                        );
                        assertEquals(
                                Range.of(-100.0, 100.0),
                                double_array.getDisplay().getDisplayRange());
                        assertEquals(
                                Range.undefined(),
                                double_array.getDisplay().getWarningRange());
                        assertEquals(
                                1,
                                double_array
                                        .getDisplay()
                                        .getFormat()
                                        .getMinimumFractionDigits());
                        assertEquals(
                                1,
                                double_array
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                        assertEquals(
                                "V",
                                double_array.getDisplay().getUnit());
                        assertEquals(
                                Instant.ofEpochSecond(0, 123L),
                                double_array.getTime().getTimestamp());
                        // Check the second sample.
                        final var statistics = (VStatistics) iterator.next();
                        assertEquals(
                                4.7,
                                statistics.getAverage().doubleValue());
                        assertEquals(
                                -3.9,
                                statistics.getMin().doubleValue());
                        assertEquals(
                                17.1,
                                statistics.getMax().doubleValue());
                        assertEquals(
                                0,
                                statistics.getNSamples().intValue());
                        assertEquals(
                                Double.NaN,
                                statistics.getStdDev().doubleValue());
                        assertEquals(
                                "NO_ALARM",
                                statistics.getAlarm().getName());
                        assertEquals(
                                AlarmSeverity.NONE,
                                statistics.getAlarm().getSeverity());
                        assertEquals(
                                Range.undefined(),
                                statistics.getDisplay().getAlarmRange());
                        assertEquals(
                                Range.undefined(),
                                statistics.getDisplay().getControlRange()
                        );
                        assertEquals(
                                Range.of(-100.0, 100.0),
                                statistics.getDisplay().getDisplayRange());
                        assertEquals(
                                Range.undefined(),
                                statistics.getDisplay().getWarningRange());
                        assertEquals(
                                1,
                                statistics
                                        .getDisplay()
                                        .getFormat()
                                        .getMinimumFractionDigits());
                        assertEquals(
                                1,
                                statistics
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                        assertEquals(
                                "V",
                                statistics.getDisplay().getUnit());
                        assertEquals(
                                Instant.ofEpochSecond(0L, 456L),
                                statistics.getTime().getTimestamp());
                        // There should be no more samples.
                        assertFalse(iterator.hasNext());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        assertEquals(1, requests.size());
        final var request = requests.get(0);
        assertEquals("GET", request.method());
        final var query_params = parseQueryString(request.uri().getQuery());
        assertEquals("0", query_params.get("start"));
        assertEquals("1000000", query_params.get("end"));
        assertEquals("10", query_params.get("count"));
    }

    /**
     * Tests the
     * {@link JsonArchiveReader#getRawValues(String, Instant, Instant)}
     * function with double samples. Of the tests for numeric values, this is
     * the most detailed one.
     */
    @Test
    public void getRawValuesWithDoubleSamples() {
        final var samples_json = """
                [ {
                   "time" : 123457000001,
                   "severity" : {
                     "level" : "OK",
                     "hasValue" : true
                   },
                   "status" : "NO_ALARM",
                   "quality" : "Original",
                   "metaData" : {
                     "type" : "numeric",
                     "precision" : 3,
                     "units" : "mA",
                     "displayLow" : 0.0,
                     "displayHigh" : 300.0,
                     "warnLow" : 5.0,
                     "warnHigh" : 100.0,
                     "alarmLow" : 2.0,
                     "alarmHigh" : "NaN"
                   },
                   "type" : "double",
                   "value" : [ 27.2, 48.3 ]
                 }, {
                   "time" : 123457000002,
                   "severity" : {
                     "level" : "MAJOR",
                     "hasValue" : true
                   },
                   "status" : "TEST_STATUS",
                   "quality" : "Original",
                   "metaData" : {
                     "type" : "numeric",
                     "precision" : 3,
                     "units" : "mA",
                     "displayLow" : 0.0,
                     "displayHigh" : 300.0,
                     "warnLow" : 5.0,
                     "warnHigh" : 100.0,
                     "alarmLow" : 2.0,
                     "alarmHigh" : "NaN"
                   },
                   "type" : "double",
                   "value" : [ 31.9 ]
                 } ]
                """;
        final var channel_name = "double-channel";
        final var start = Instant.ofEpochMilli(123456L);
        final var end = Instant.ofEpochMilli(456789L);
        final var preferences = new JsonArchivePreferences(true);
        var requests = withSamples(
                2, channel_name, samples_json, (base_url) -> {
                    try (
                            final var reader = new JsonArchiveReader(
                                    "json:" + base_url + ";key=2",
                                    preferences);
                            final var iterator = reader.getRawValues(
                                    channel_name, start, end)
                    ) {
                        // Check the first sample.
                        assertTrue(iterator.hasNext());
                        final var double_array = (VDoubleArray) iterator.next();
                        assertEquals(2, double_array.getData().size());
                        assertEquals(
                                27.2, double_array.getData().getDouble(0));
                        assertEquals(
                                48.3, double_array.getData().getDouble(1));
                        assertEquals(
                                "NO_ALARM", double_array.getAlarm().getName());
                        assertEquals(
                                AlarmSeverity.NONE,
                                double_array.getAlarm().getSeverity());
                        assertEquals(
                                2.0,
                                double_array
                                        .getDisplay().getAlarmRange().getMinimum());
                        assertEquals(
                                Double.POSITIVE_INFINITY,
                                double_array
                                        .getDisplay().getAlarmRange().getMaximum());
                        assertEquals(
                                Range.undefined(),
                                double_array.getDisplay().getControlRange()
                        );
                        assertEquals(
                                0.0,
                                double_array
                                        .getDisplay().getDisplayRange().getMinimum());
                        assertEquals(
                                300.0,
                                double_array
                                        .getDisplay().getDisplayRange().getMaximum());
                        assertEquals(
                                5.0,
                                double_array
                                        .getDisplay().getWarningRange().getMinimum());
                        assertEquals(
                                100.0,
                                double_array
                                        .getDisplay().getWarningRange().getMaximum());
                        assertEquals(
                                3,
                                double_array
                                        .getDisplay()
                                        .getFormat()
                                        .getMinimumFractionDigits());
                        assertEquals(
                                3,
                                double_array
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                        assertEquals(
                                "mA",
                                double_array.getDisplay().getUnit());
                        assertEquals(
                                Instant.ofEpochSecond(123L, 457000001L),
                                double_array.getTime().getTimestamp());
                        // Check the second sample (only the parts that differ
                        // from the first on).
                        assertTrue(iterator.hasNext());
                        final var double_scalar = (VDouble) iterator.next();
                        assertEquals(
                                31.9, double_scalar.getValue().doubleValue());
                        assertEquals(
                                "TEST_STATUS",
                                double_scalar.getAlarm().getName());
                        assertEquals(
                                AlarmSeverity.MAJOR,
                                double_scalar.getAlarm().getSeverity());
                        assertEquals(
                                Instant.ofEpochSecond(123L, 457000002L),
                                double_scalar.getTime().getTimestamp());
                        // There should be no more samples.
                        assertFalse(iterator.hasNext());
                        assertThrows(NoSuchElementException.class, iterator::next);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
        });
        assertEquals(1, requests.size());
        final var request = requests.get(0);
        assertEquals("GET", request.method());
        final var query_params = parseQueryString(request.uri().getQuery());
        assertEquals("123456000000", query_params.get("start"));
        assertEquals("456789000000", query_params.get("end"));
        assertFalse(query_params.containsKey("count"));
    }

    /**
     * Tests the
     * {@link JsonArchiveReader#getRawValues(String, Instant, Instant)} method
     * with enum samples.
     */
    @Test
    public void getRawValuesWithEnumSamples() {
        final var samples_json = """
                [ {
                  "time" : 123000000009,
                  "severity" : {
                    "level" : "OK",
                    "hasValue" : true
                  },
                  "status" : "NO_ALARM",
                  "quality" : "Original",
                  "metaData" : {
                    "type" : "enum",
                    "states" : [ "High", "Low" ]
                  },
                  "type" : "enum",
                  "value" : [ 1, 0 ]
                }, {
                  "time" : 124000000011,
                  "severity" : {
                    "level" : "INVALID",
                    "hasValue" : true
                  },
                  "status" : "LINK",
                  "quality" : "Original",
                  "metaData" : {
                    "type" : "enum",
                    "states" : [ "High", "Low" ]
                  },
                  "type" : "enum",
                  "value" : [ 1 ]
                }, {
                  "time" : 124000000012,
                  "severity" : {
                    "level" : "OK",
                    "hasValue" : true
                  },
                  "status" : "NO_ALARM",
                  "quality" : "Original",
                  "metaData" : {
                    "type" : "enum",
                    "states" : [ "High", "Low" ]
                  },
                  "type" : "enum",
                  "value" : [ 1, 2 ]
                }, {
                  "time" : 124000000013,
                  "severity" : {
                    "level" : "OK",
                    "hasValue" : true
                  },
                  "status" : "NO_ALARM",
                  "quality" : "Original",
                  "metaData" : {
                    "type" : "enum",
                    "states" : [ "High", "Low" ]
                  },
                  "type" : "enum",
                  "value" : [ -1 ]
                } ]
                """;
        final var channel_name = "enum-channel";
        final var start = Instant.ofEpochMilli(4321L);
        final var end = Instant.ofEpochMilli(999999L);
        final var preferences = new JsonArchivePreferences(true);
        var requests = withSamples(
                1, channel_name, samples_json, (base_url) -> {
                    try (
                            final var reader = new JsonArchiveReader(
                                    "json:" + base_url, preferences);
                            final var iterator = reader.getRawValues(
                                    channel_name, start, end)
                    ) {
                        // Check the first sample.
                        final var enum_array = (VEnumArray) iterator.next();
                        assertEquals(2, enum_array.getIndexes().size());
                        assertEquals(1, enum_array.getIndexes().getInt(0));
                        assertEquals(0, enum_array.getIndexes().getInt(1));
                        assertEquals(
                                "NO_ALARM",
                                enum_array.getAlarm().getName());
                        assertEquals(
                                AlarmSeverity.NONE,
                                enum_array.getAlarm().getSeverity());
                        assertEquals(
                                Arrays.asList("High", "Low"),
                                enum_array.getDisplay().getChoices());
                        assertEquals(
                                Instant.ofEpochSecond(123L, 9L),
                                enum_array.getTime().getTimestamp());
                        // Check the second sample (only the parts that differ
                        // from the first on).
                        final var enum_scalar = (VEnum) iterator.next();
                        assertEquals(1, enum_scalar.getIndex());
                        assertEquals(
                                "LINK",
                                enum_scalar.getAlarm().getName());
                        assertEquals(
                                AlarmSeverity.INVALID,
                                enum_scalar.getAlarm().getSeverity());
                        assertEquals(
                                Instant.ofEpochSecond(124L, 11L),
                                enum_scalar.getTime().getTimestamp());
                        // Check the third sample. As this sample contains a
                        // value for which there is no label, we expect a
                        // VIntArray instead of a VEnumArray.
                        final var int_array = (VIntArray) iterator.next();
                        assertEquals(2, int_array.getData().size());
                        assertEquals(1, int_array.getData().getInt(0));
                        assertEquals(2, int_array.getData().getInt(1));
                        assertEquals(
                                0,
                                int_array
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                        // Check the fourth sample. As this sample contains a
                        // value for which there is no label, we expect a
                        // VInt instead of a VEnum.
                        final var int_scalar = (VInt) iterator.next();
                        assertEquals(-1, int_scalar.getValue().intValue());
                        // There should be no more samples.
                        assertFalse(iterator.hasNext());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        assertEquals(1, requests.size());
        final var request = requests.get(0);
        assertEquals("GET", request.method());
        final var query_params = parseQueryString(request.uri().getQuery());
        assertEquals("4321000000", query_params.get("start"));
        assertEquals("999999000000", query_params.get("end"));
        assertFalse(query_params.containsKey("count"));
    }

    /**
     * Tests the
     * {@link JsonArchiveReader#getRawValues(String, Instant, Instant)} method
     * with long samples.
     */
    @Test
    public void getRawValuesWithLongSamples() {
        final var samples_json = """
                [ {
                  "time" : 456000000001,
                  "severity" : {
                    "level" : "MAJOR",
                    "hasValue" : true
                  },
                  "status" : "SOME_ALARM",
                  "quality" : "Original",
                  "metaData" : {
                    "type" : "numeric",
                    "precision" : 0,
                    "units" : "pcs.",
                    "displayLow" : 1.0,
                    "displayHigh" : 100.0,
                    "warnLow" : 0.0,
                    "warnHigh" : 0.0,
                    "alarmLow" : 0.0,
                    "alarmHigh" : 0.0
                  },
                  "type" : "long",
                  "value" : [ 14, 2 ]
                }, {
                  "time" : 456000000002,
                  "severity" : {
                    "level" : "INVALID",
                    "hasValue" : true
                  },
                  "status" : "INVALID_ALARM",
                  "quality" : "Original",
                  "metaData" : {
                    "type" : "numeric",
                    "precision" : 0,
                    "units" : "pcs.",
                    "displayLow" : 1.0,
                    "displayHigh" : 100.0,
                    "warnLow" : 0.0,
                    "warnHigh" : 0.0,
                    "alarmLow" : 0.0,
                    "alarmHigh" : 0.0
                  },
                  "type" : "long",
                  "value" : [ 19 ]
                } ]
                """;
        final var channel_name = "long-channel";
        final var start = Instant.ofEpochMilli(4321L);
        final var end = Instant.ofEpochMilli(999999L);
        final var preferences = new JsonArchivePreferences(true);
        var requests = withSamples(
                1, channel_name, samples_json, (base_url) -> {
                    try (
                            final var reader = new JsonArchiveReader(
                                    "json:" + base_url, preferences);
                            final var iterator = reader.getRawValues(
                                    channel_name, start, end)
                    ) {

                        // Check the first sample. We do not check the limits
                        // because the code parsing them is identical the same
                        // for the double samples, and we already check them
                        // there.
                        final var long_array = (VLongArray) iterator.next();
                        assertEquals(2, long_array.getData().size());
                        assertEquals(14, long_array.getData().getLong(0));
                        assertEquals(2, long_array.getData().getLong(1));
                        assertEquals(
                                "SOME_ALARM",
                                long_array.getAlarm().getName());
                        assertEquals(
                                AlarmSeverity.MAJOR,
                                long_array.getAlarm().getSeverity());
                        assertEquals(
                                0,
                                long_array
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                        assertEquals(
                                "pcs.",
                                long_array.getDisplay().getUnit());
                        assertEquals(
                                Instant.ofEpochSecond(456L, 1L),
                                long_array.getTime().getTimestamp());
                        // Check the second sample (only the parts that differ
                        // from the first on).
                        final var long_scalar = (VLong) iterator.next();
                        assertEquals(19, long_scalar.getValue().longValue());
                        assertEquals(
                                "INVALID_ALARM",
                                long_scalar.getAlarm().getName());
                        assertEquals(
                                AlarmSeverity.INVALID,
                                long_scalar.getAlarm().getSeverity());
                        assertEquals(
                                Instant.ofEpochSecond(456L, 2L),
                                long_scalar.getTime().getTimestamp());
                        // There should be no more samples.
                        assertFalse(iterator.hasNext());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        assertEquals(1, requests.size());
        final var request = requests.get(0);
        assertEquals("GET", request.method());
        final var query_params = parseQueryString(request.uri().getQuery());
        assertEquals("4321000000", query_params.get("start"));
        assertEquals("999999000000", query_params.get("end"));
        assertFalse(query_params.containsKey("count"));
    }

    /**
     * Tests the
     * {@link JsonArchiveReader#getRawValues(String, Instant, Instant)} method
     * with a malformed response.
     */
    @Test
    public void getRawValuesWithMalformedResponse() {
        final var channel_name = "some-channel";
        final var start = Instant.ofEpochMilli(123L);
        final var end = Instant.ofEpochMilli(456L);
        final var preferences = new JsonArchivePreferences(true);
        // First, we test that we get an immediate exception if the first
        // sample is malformed.
        var samples_json = """
                [ {
                   "time_modified" : 123457000001,
                   "severity" : {
                     "level" : "OK",
                     "hasValue" : true
                   },
                   "status" : "NO_ALARM",
                   "quality" : "Original",
                   "metaData" : {
                     "type" : "numeric",
                     "precision" : 3,
                     "units" : "mA",
                     "displayLow" : 0.0,
                     "displayHigh" : 300.0,
                     "warnLow" : 5.0,
                     "warnHigh" : 100.0,
                     "alarmLow" : 2.0,
                     "alarmHigh" : "NaN"
                   },
                   "type" : "double",
                   "value" : [ 27.2, 48.3 ]
                 } ]
                """;

        withSamples(
                1, channel_name, samples_json, (base_url) -> {
                    try (final var reader = new JsonArchiveReader(
                            "json:" + base_url, preferences)) {
                                assertThrows(IOException.class, () -> {
                                    reader.getRawValues(
                                            channel_name, start, end);
                                });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        // Second, we test that we do not get an exception when a subsequent
        // sample is malformed. Instead, we expect hasNext() to return false.
        // As a side effect, an error message should be logged. but we cannot
        // test this easily.
        samples_json = """
                [ {
                   "time" : 123457000001,
                   "severity" : {
                     "level" : "OK",
                     "hasValue" : true
                   },
                   "status" : "NO_ALARM",
                   "quality" : "Original",
                   "metaData" : {
                     "type" : "numeric",
                     "precision" : 3,
                     "units" : "mA",
                     "displayLow" : 0.0,
                     "displayHigh" : 300.0,
                     "warnLow" : 5.0,
                     "warnHigh" : 100.0,
                     "alarmLow" : 2.0,
                     "alarmHigh" : "NaN"
                   },
                   "type" : "double",
                   "value" : [ 27.2, 48.3 ]
                 }, {
                   "time_modified" : 123457000002,
                   "severity" : {
                     "level" : "MAJOR",
                     "hasValue" : true
                   },
                   "status" : "TEST_STATUS",
                   "quality" : "Original",
                   "metaData" : {
                     "type" : "numeric",
                     "precision" : 3,
                     "units" : "mA",
                     "displayLow" : 0.0,
                     "displayHigh" : 300.0,
                     "warnLow" : 5.0,
                     "warnHigh" : 100.0,
                     "alarmLow" : 2.0,
                     "alarmHigh" : "NaN"
                   },
                   "type" : "double",
                   "value" : [ 31.9 ]
                 } ]
                """;
        withSamples(1, channel_name, samples_json, (base_url) -> {
            try (
                    final var reader = new JsonArchiveReader(
                            "json:" + base_url, preferences);
                    final var iterator = reader.getRawValues(
                            channel_name, start, end)
                ) {
                // We should be able to retrieve the first sample, but
                // not the second one.
                assertTrue(iterator.hasNext());
                iterator.next();
                // Before calling hasNext() the second time, we suppress error
                // logging for the iterator.
                final var iterator_logger = Logger.getLogger(
                        iterator.getClass().getName());
                final var log_level = iterator_logger.getLevel();
                iterator_logger.setLevel(Level.OFF);
                try {
                    assertFalse(iterator.hasNext());
                } finally {
                    iterator_logger.setLevel(log_level);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Tests the
     * {@link JsonArchiveReader#getRawValues(String, Instant, Instant)} method
     * with no samples.
     */
    @Test
    public void getRawValuesWithNoSamples() {
        final var channel_name = "empty-channel";
        final var start = Instant.ofEpochMilli(456L);
        final var end = Instant.ofEpochMilli(789L);
        final var preferences = new JsonArchivePreferences(true);
        var requests = withSamples(
                1, channel_name, "[]", (base_url) -> {
                    try (
                            final var reader = new JsonArchiveReader(
                                "json:" + base_url, preferences);
                            final var iterator = reader.getRawValues(
                                    channel_name, start, end)
                    ) {
                        // The iterator should be empty.
                        assertFalse(iterator.hasNext());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        assertEquals(1, requests.size());
        final var request = requests.get(0);
        assertEquals("GET", request.method());
        final var query_params = parseQueryString(request.uri().getQuery());
        assertEquals("456000000", query_params.get("start"));
        assertEquals("789000000", query_params.get("end"));
        assertFalse(query_params.containsKey("count"));
    }

    /**
     * Tests the
     * {@link JsonArchiveReader#getRawValues(String, Instant, Instant)} method
     * with string samples.
     */
    @Test
    public void getRawValuesWithStringSamples() {
        final var samples_json = """
                [ {
                  "time" : 123000000001,
                  "severity" : {
                    "level" : "OK",
                    "hasValue" : true
                  },
                  "status" : "NO_ALARM",
                  "quality" : "Original",
                  "type" : "string",
                  "value" : [ "abc", "def", "ghi" ]
                }, {
                  "time" : 123000000002,
                  "severity" : {
                    "level" : "OK",
                    "hasValue" : true
                  },
                  "status" : "NO_ALARM",
                  "quality" : "Original",
                  "type" : "string",
                  "value" : [ "123" ]
                } ]
                """;
        final var channel_name = "long-channel";
        final var start = Instant.ofEpochMilli(0L);
        final var end = Instant.ofEpochMilli(999000L);
        final var preferences = new JsonArchivePreferences(true);
        var requests = withSamples(
                1, channel_name, samples_json, (base_url) -> {
                    try (
                            final var reader = new JsonArchiveReader(
                                    "json:" + base_url, preferences);
                            final var iterator = reader.getRawValues(
                                    channel_name, start, end)
                        ) {
                        // Check the first sample.
                        final var string_array = (VStringArray) iterator.next();
                        assertEquals(3, string_array.getData().size());
                        assertEquals("abc", string_array.getData().get(0));
                        assertEquals("def", string_array.getData().get(1));
                        assertEquals("ghi", string_array.getData().get(2));
                        assertEquals(
                                Instant.ofEpochSecond(123L, 1L),
                                string_array.getTime().getTimestamp());
                        // Check the second sample.
                        final var string_scalar = (VString) iterator.next();
                        assertEquals("123", string_scalar.getValue());
                        assertEquals(
                                Instant.ofEpochSecond(123L, 2L),
                                string_scalar.getTime().getTimestamp());
                        // There should be no more samples.
                        assertFalse(iterator.hasNext());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        assertEquals(1, requests.size());
        final var request = requests.get(0);
        assertEquals("GET", request.method());
        final var query_params = parseQueryString(request.uri().getQuery());
        assertEquals("0", query_params.get("start"));
        assertEquals("999000000000", query_params.get("end"));
        assertFalse(query_params.containsKey("count"));
    }

    /**
     * Tests the
     * {@link JsonArchiveReader#getRawValues(String, Instant, Instant)} method
     * with a channel name that is not known by the server.
     */
    @Test
    public void getRawValuesWithUnknownChannel() {
        final var start = Instant.ofEpochMilli(123L);
        final var end = Instant.ofEpochMilli(456L);
        final var preferences = new JsonArchivePreferences(true);
        withSamples(1, "some-channel", "", (base_url) -> {
            try (final var reader = new JsonArchiveReader(
                    "json:" + base_url, preferences)) {
                assertThrows(UnknownChannelException.class, () -> {
                    reader.getRawValues("another-channel", start, end);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Tests the {@link JsonArchivePreferences#honor_zero_precision()} flag.
     */
    @Test
    public void honorZeroPrecision() {
        final var samples_json = """
                [ {
                   "time" : 123457000001,
                   "severity" : {
                     "level" : "OK",
                     "hasValue" : true
                   },
                   "status" : "NO_ALARM",
                   "quality" : "Original",
                   "metaData" : {
                     "type" : "numeric",
                     "precision" : 0,
                     "units" : "mA",
                     "displayLow" : 0.0,
                     "displayHigh" : 300.0,
                     "warnLow" : 5.0,
                     "warnHigh" : 100.0,
                     "alarmLow" : 2.0,
                     "alarmHigh" : "NaN"
                   },
                   "type" : "double",
                   "value" : [ 1.5 ]
                }, {
                  "time" : 456000000002,
                  "severity" : {
                    "level" : "INVALID",
                    "hasValue" : true
                  },
                  "status" : "INVALID_ALARM",
                  "quality" : "Original",
                  "metaData" : {
                    "type" : "numeric",
                    "precision" : 0,
                    "units" : "pcs.",
                    "displayLow" : 1.0,
                    "displayHigh" : 100.0,
                    "warnLow" : 0.0,
                    "warnHigh" : 0.0,
                    "alarmLow" : 0.0,
                    "alarmHigh" : 0.0
                  },
                  "type" : "long",
                  "value" : [ 19 ]
                 } ]
                """;
        final var channel_name = "double-channel";
        final var start = Instant.ofEpochMilli(123456L);
        final var end = Instant.ofEpochMilli(456789L);
        // When honor_zero_precision is set, a sample with a precision of zero
        // should have a number format that does not include fractional digits.
        withSamples(
                1, channel_name, samples_json, (base_url) -> {
                    final var preferences = new JsonArchivePreferences(true);
                    try (
                            final var reader = new JsonArchiveReader(
                                    "json:" + base_url, preferences);
                            final var iterator = reader.getRawValues(
                                    channel_name, start, end)
                    ) {
                        final var double_scalar = (VDouble) iterator.next();
                        assertEquals(
                                0,
                                double_scalar
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                        final var long_scalar = (VLong) iterator.next();
                        assertEquals(
                                0,
                                long_scalar
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        // When honor_zero_precision is clear, a sample with a precision of
        // zero should have a number format that includes fractional digits,
        // but only for double samples and not for long samples.
        withSamples(
                1, channel_name, samples_json, (base_url) -> {
                    final var preferences = new JsonArchivePreferences(false);
                    try (
                            final var reader = new JsonArchiveReader(
                                    "json:" + base_url, preferences);
                            final var iterator = reader.getRawValues(
                                    channel_name, start, end)
                    ) {
                        final var double_scalar = (VDouble) iterator.next();
                        assertNotEquals(
                                0,
                                double_scalar
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                        final var long_scalar = (VLong) iterator.next();
                        assertEquals(
                                0,
                                long_scalar
                                        .getDisplay()
                                        .getFormat()
                                        .getMaximumFractionDigits());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
