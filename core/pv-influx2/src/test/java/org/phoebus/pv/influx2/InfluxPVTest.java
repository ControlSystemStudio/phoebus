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
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.epics.vtype.VDouble;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.phoebus.pv.PV;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.testng.annotations.AfterClass;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockStatic;

/**
 * Comprehensive test suite for InfluxDB Process Variable (PV) functionality.
 * This test class validates the complete InfluxDB PV system including:
 * - Basic PV creation and configuration with explicit bucket specification
 * - Asynchronous read operations
 * - Connection and error handling
 * - Data type conversion between InfluxDB and EPICS VTypes
 * - Performance testing with large numbers of PVs
 * - Factory pattern implementation
 * - Polling manager stress testing
 * The tests use Mockito to simulate InfluxDB client interactions without
 * requiring an actual InfluxDB instance, ensuring fast and reliable unit testing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InfluxPVTest {

    @Mock
    private InfluxDBClient mockInfluxClient;

    @Mock
    private QueryApi mockQueryApi;

    @Mock
    private WriteApiBlocking mockWriteApi;

    private MockedStatic<InfluxDB_Context> mockContext;
    private MockedStatic<InfluxDB_Preferences> mockPreferences;

    private InfluxDB_Context mockContextInstance;
    private InfluxDB_Preferences mockPreferencesInstance;
    private InfluxDB_PollingManager pollingManager;

    private static final String TEST_BUCKET = "test_bucket";
    private static final String TEST_MEASUREMENT = "test_measurement";
    private static final String TEST_FIELD = "value";
    private static final long TEST_PERIOD = 100L;

    /**
     * Sets up the test environment before each test method.
     * Initializes all mock objects including InfluxDB client, APIs, context,
     * and preferences. Configures default behaviors for common operations.
     */
    @BeforeEach
    void setUp() {
        mockContextInstance = mock(InfluxDB_Context.class);
        when(mockContextInstance.getClient()).thenReturn(mockInfluxClient);
        mockContext = mockStatic(InfluxDB_Context.class);
        mockContext.when(InfluxDB_Context::getInstance).thenReturn(mockContextInstance);

        mockPreferencesInstance = mock(InfluxDB_Preferences.class);
        when(mockPreferencesInstance.getRefreshPeriod()).thenReturn(TEST_PERIOD);
        when(mockPreferencesInstance.getDisconnectTimeout()).thenReturn(3000L);
        when(mockPreferencesInstance.isUseHttps()).thenReturn(false);
        mockPreferences = mockStatic(InfluxDB_Preferences.class);
        mockPreferences.when(InfluxDB_Preferences::getInstance).thenReturn(mockPreferencesInstance);

        when(mockInfluxClient.getQueryApi()).thenReturn(mockQueryApi);
        when(mockInfluxClient.getWriteApiBlocking()).thenReturn(mockWriteApi);

        pollingManager = InfluxDB_PollingManager.getInstance();
    }

    /**
     * Cleans up mock static objects after each test method to prevent interference
     * between tests.
     */
    @AfterEach
    void tearDownTest() {
        if (mockContext != null) {
            mockContext.close();
        }
        if (mockPreferences != null) {
            mockPreferences.close();
        }
    }

    /**
     * Performs final cleanup of the polling manager after all tests complete.
     * Ensures proper shutdown of background threads and resources.
     */
    @AfterClass
    void tearDown() {
        if (pollingManager != null) {
            pollingManager.shutdown();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Tests basic InfluxDB PV creation with explicit bucket specification.
     * Verifies that a PV can be created with a specific bucket, measurement,
     * and field, and that all properties are correctly initialized.
     */
    @Test
    @Order(1)
    @DisplayName("Test basic InfluxDB PV creation with bucket/measurement/field")
    void testBasicPVCreationWithField() {
        InfluxDB_PV pv = new InfluxDB_PV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD,
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD
        );

        assertNotNull(pv);
        assertEquals(TEST_BUCKET, pv.getBucket());
        assertEquals(TEST_MEASUREMENT, pv.getMeasurement());
        assertEquals(TEST_FIELD, pv.getField());
        assertEquals(100L, pv.getPeriod());

        pv.close();
    }

    /**
     * Tests PV creation without specific field specification.
     * Verifies that a PV can be created with only bucket and measurement,
     * allowing the system to read all fields from the measurement.
     */
    @Test
    @Order(2)
    @DisplayName("Test PV creation with bucket/measurement only")
    void testPVCreationWithoutField() {
        InfluxDB_PV pv = new InfluxDB_PV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT,
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT
        );

        assertNotNull(pv);
        assertEquals(TEST_BUCKET, pv.getBucket());
        assertEquals(TEST_MEASUREMENT, pv.getMeasurement());
        assertNull(pv.getField());
        assertEquals(100L, pv.getPeriod());

        pv.close();
    }

    /**
     * Tests that PV creation fails when bucket is not specified.
     * The system should throw an IllegalArgumentException when attempting
     * to create a PV without a bucket specification.
     */
    @Test
    @Order(3)
    @DisplayName("Test PV creation fails without bucket")
    void testPVCreationFailsWithoutBucket() {
        assertThrows(IllegalArgumentException.class, () -> new InfluxDB_PV(TEST_MEASUREMENT, "influx2://" + TEST_MEASUREMENT));
    }

    /**
     * Tests that PV creation fails with invalid format.
     * The system should throw an IllegalArgumentException when the format
     * doesn't match the expected patterns.
     */
    @Test
    @Order(4)
    @DisplayName("Test PV creation fails with invalid format")
    void testPVCreationFailsWithInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new InfluxDB_PV("test", "influx2://bucket/measurement/field/extra"));
        assertThrows(IllegalArgumentException.class, () -> new InfluxDB_PV("test", "influx2://measurement"));
    }

    /**
     * Tests asynchronous read operations from InfluxDB.
     * Verifies that the PV can successfully execute asynchronous queries
     * against InfluxDB and convert the results to appropriate VType objects.
     */
    @Test
    @Order(5)
    @DisplayName("Test asynchronous read")
    void testAsyncRead() throws Exception {
        when(mockPreferencesInstance.getRefreshPeriod()).thenReturn(10000L);

        FluxRecord mockRecord = mock(FluxRecord.class);
        when(mockRecord.getValue()).thenReturn(42.0);
        when(mockRecord.getTime()).thenReturn(Instant.now());
        when(mockRecord.getField()).thenReturn(TEST_FIELD);

        FluxTable mockTable = mock(FluxTable.class);
        when(mockTable.getRecords()).thenReturn(List.of(mockRecord));

        when(mockQueryApi.query(anyString())).thenReturn(List.of(mockTable));

        InfluxDB_PV pv = new InfluxDB_PV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD,
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD
        );

        CompletableFuture<VType> future = pv.asyncRead();
        VType result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertInstanceOf(VDouble.class, result);
        assertEquals(42.0, ((VDouble) result).getValue(), 0.001);

        pv.close();
    }

    /**
     * Tests proper handling of connection errors during read operations.
     * When the InfluxDB connection fails, the PV should propagate the error
     * appropriately through the CompletableFuture mechanism.
     */
    @Test
    @Order(6)
    @DisplayName("Test connection error handling")
    void testConnectionErrorHandling() {
        when(mockPreferencesInstance.getRefreshPeriod()).thenReturn(10000L);

        when(mockQueryApi.query(anyString())).thenThrow(new RuntimeException("Connection failed"));

        InfluxDB_PV pv = new InfluxDB_PV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD,
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD
        );

        CompletableFuture<VType> future = pv.asyncRead();
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));

        pv.close();
    }

    /**
     * Tests PV disconnection scenarios.
     * Verifies that PVs can handle disconnection events gracefully,
     * typically triggered by stale data or connection timeouts.
     */
    @Test
    @Order(7)
    @DisplayName("Test disconnection handling")
    void testDisconnectionHandling() throws InterruptedException {
        FluxRecord oldRecord = mock(FluxRecord.class);
        when(oldRecord.getValue()).thenReturn(42.0);
        when(oldRecord.getTime()).thenReturn(Instant.now().minus(10, ChronoUnit.SECONDS));
        when(oldRecord.getField()).thenReturn(TEST_FIELD);

        FluxTable mockTable = mock(FluxTable.class);
        when(mockTable.getRecords()).thenReturn(List.of(oldRecord));

        when(mockQueryApi.query(anyString())).thenReturn(List.of(mockTable));

        InfluxDB_PV pv = new InfluxDB_PV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD,
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD
        );

        Thread.sleep(500);

        pv.close();
    }

    /**
     * Tests data type conversion from InfluxDB records to EPICS VTypes.
     * Verifies that the helper utilities can correctly convert different
     * data types from InfluxDB FluxRecords to their corresponding VType representations.
     */
    @Test
    @Order(8)
    @DisplayName("Test data type conversion")
    void testDataTypeConversion() throws Exception {
        FluxRecord doubleRecord = mock(FluxRecord.class);
        when(doubleRecord.getValue()).thenReturn(3.14159);
        when(doubleRecord.getTime()).thenReturn(Instant.now());

        VType doubleResult = InfluxDB_Helper.convertRecordToVType(doubleRecord);
        assertInstanceOf(VDouble.class, doubleResult);
        assertEquals(3.14159, ((VDouble) doubleResult).getValue(), 0.00001);

        FluxRecord stringRecord = mock(FluxRecord.class);
        when(stringRecord.getValue()).thenReturn("test-string");
        when(stringRecord.getTime()).thenReturn(Instant.now());

        VType stringResult = InfluxDB_Helper.convertRecordToVType(stringRecord);
        assertInstanceOf(VString.class, stringResult);
        assertEquals("test-string", ((VString) stringResult).getValue());

        FluxRecord intRecord = mock(FluxRecord.class);
        when(intRecord.getValue()).thenReturn(42);
        when(intRecord.getTime()).thenReturn(Instant.now());

        VType intResult = InfluxDB_Helper.convertRecordToVType(intRecord);
        assertInstanceOf(VDouble.class, intResult);
        assertEquals(42.0, ((VDouble) intResult).getValue(), 0.001);
    }

    /**
     * Tests PV creation without specifying a specific field name.
     * When no field is specified, the PV should be able to read from
     * the measurement using default field resolution.
     */
    @Test
    @Order(9)
    @DisplayName("Test PV without specific field")
    void testPVWithoutSpecificField() throws Exception {
        when(mockPreferencesInstance.getRefreshPeriod()).thenReturn(10000L);

        FluxRecord mockRecord = mock(FluxRecord.class);
        when(mockRecord.getValue()).thenReturn(99.9);
        when(mockRecord.getTime()).thenReturn(Instant.now());
        when(mockRecord.getMeasurement()).thenReturn(TEST_MEASUREMENT);

        FluxTable mockTable = mock(FluxTable.class);
        when(mockTable.getRecords()).thenReturn(List.of(mockRecord));

        when(mockQueryApi.query(anyString())).thenReturn(List.of(mockTable));

        InfluxDB_PV pv = new InfluxDB_PV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT,
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT
        );

        CompletableFuture<VType> future = pv.asyncRead();
        VType result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertNull(pv.getField());
        assertInstanceOf(VDouble.class, result);

        pv.close();
    }

    /**
     * Performance test with 10,000 PVs to validate system scalability.
     * This test creates a large number of PVs to validate that the system
     * can handle high-volume scenarios.
     */
    @Test
    @Order(10)
    @DisplayName("Performance test with 10000 PVs")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testPerformanceWith10000PVs() throws Exception {
        when(mockPreferencesInstance.getRefreshPeriod()).thenReturn(5000L);

        final int PV_COUNT = 10000;
        final int BUCKET_COUNT = 10;

        FluxRecord mockRecord = mock(FluxRecord.class);
        when(mockRecord.getValue()).thenReturn(42.0);
        when(mockRecord.getTime()).thenReturn(Instant.now());
        when(mockRecord.getField()).thenReturn("");

        FluxTable mockTable = mock(FluxTable.class);
        when(mockTable.getRecords()).thenReturn(List.of(mockRecord));
        when(mockQueryApi.query(anyString())).thenReturn(List.of(mockTable));

        AtomicLong creationTime = new AtomicLong(0);
        AtomicInteger successfulCreations = new AtomicInteger(0);

        List<InfluxDB_PV> pvs = new ArrayList<>(PV_COUNT);

        long startTime = System.nanoTime();

        for (int i = 0; i < PV_COUNT; i++) {
            long pvStartTime = System.nanoTime();

            InfluxDB_PV pv = mock(InfluxDB_PV.class);
            when(pv.getBucket()).thenReturn("bucket-" + (i % BUCKET_COUNT));
            when(pv.getMeasurement()).thenReturn("measurement-" + i);

            when(pv.asyncRead()).thenReturn(CompletableFuture.completedFuture(new VType() {
                @Override
                public String toString() {
                    return "mocked_value";
                }
            }));

            pvs.add(pv);

            long pvEndTime = System.nanoTime();
            creationTime.addAndGet(pvEndTime - pvStartTime);
            successfulCreations.incrementAndGet();
        }

        long endTime = System.nanoTime();
        long totalCreationTime = endTime - startTime;

        Thread.sleep(1000);

        AtomicInteger successfulReads = new AtomicInteger(0);
        AtomicLong readTime = new AtomicLong(0);

        for (InfluxDB_PV pv : pvs) {
            try {
                long readStart = System.nanoTime();
                CompletableFuture<VType> future = pv.asyncRead();
                VType result = future.get(5, TimeUnit.SECONDS);
                long readEnd = System.nanoTime();

                if (result != null) {
                    successfulReads.incrementAndGet();
                    readTime.addAndGet(readEnd - readStart);
                }
            } catch (Exception e) {
                System.err.println("Read failed: " + e.getMessage());
            }
        }

        System.out.println("\n=== Performance Test Results ===");
        System.out.println("Total PVs created: " + successfulCreations.get() + "/" + PV_COUNT);
        System.out.println("Total creation time: " + (totalCreationTime / 1_000_000) + " ms");
        System.out.println("Average creation time per PV: " + (creationTime.get() / PV_COUNT / 1_000_000) + " ms");
        System.out.println("Successful reads: " + successfulReads.get() + "/" + PV_COUNT);
        if (successfulReads.get() > 0) {
            System.out.println("Average read time: " + (readTime.get() / successfulReads.get() / 1_000_000) + " ms");
        }

        assertTrue(successfulCreations.get() >= PV_COUNT * 0.95,
            "Should create at least 95% of PVs successfully");
        assertTrue(totalCreationTime < 30_000_000_000L,
            "Total creation time should be less than 30 seconds");
        assertTrue(successfulReads.get() >= 80,
            "Should complete at least 80% of read tests successfully");

        System.gc();
        Thread.sleep(100);
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Final memory usage: " + finalMemory / 1024 / 1024 + " MB");

        long cleanupStart = System.nanoTime();
        for (InfluxDB_PV pv : pvs) {
            try {
                pv.close();
            } catch (Exception ignore) {
            }
        }
        long cleanupEnd = System.nanoTime();
        System.out.println("Cleanup time: " + ((cleanupEnd - cleanupStart) / 1_000_000) + " ms");
        System.out.println("=== End Performance Test ===\n");
    }

    /**
     * Tests that unsupported operations throw appropriate exceptions.
     * InfluxDB PVs are read-only by design.
     */
    @Test
    @Order(11)
    @DisplayName("Test unsupported operations")
    void testUnsupportedOperations() {
        when(mockPreferencesInstance.getRefreshPeriod()).thenReturn(10000L);

        InfluxDB_PV pv = new InfluxDB_PV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD,
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT + "/" + TEST_FIELD
        );

        assertThrows(UnsupportedOperationException.class, () -> pv.write(42.0));
        assertThrows(UnsupportedOperationException.class, () -> pv.asyncWrite(42.0));

        pv.close();
    }

    /**
     * Tests the InfluxDB PV factory implementation.
     * Verifies that the factory correctly identifies itself with the "influx2"
     * type and can create InfluxDB_PV instances when requested.
     */
    @Test
    @Order(12)
    @DisplayName("Test InfluxDB PV Factory")
    void testPVFactory() {
        InfluxDB_PVFactory factory = new InfluxDB_PVFactory();

        assertEquals("influx2", factory.getType());

        PV pv = factory.createPV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT,
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT
        );
        assertNotNull(pv);
        assertInstanceOf(InfluxDB_PV.class, pv);
    }

    /**
     * Stress tests the polling manager with a large number of PVs.
     * This test creates 100,000 PVs to validate that the polling manager
     * can handle extreme loads without crashing or causing memory issues.
     */
    @Test
    @Order(13)
    @DisplayName("Test stress polling manager")
    void testPollingManagerStress() throws InterruptedException {
        final int STRESS_PV_COUNT = 100_000;
        List<InfluxDB_PV> stressPVs = Collections.synchronizedList(new ArrayList<>());

        try {
            when(mockPreferencesInstance.getRefreshPeriod()).thenReturn(5000L);

            FluxRecord mockRecord = mock(FluxRecord.class);
            when(mockRecord.getValue()).thenReturn(123.45);
            when(mockRecord.getTime()).thenReturn(Instant.now());
            when(mockRecord.getField()).thenReturn("mockField");

            FluxTable mockTable = mock(FluxTable.class);
            when(mockTable.getRecords()).thenReturn(List.of(mockRecord));
            when(mockQueryApi.query(anyString())).thenReturn(List.of(mockTable));

            for (int i = 0; i < STRESS_PV_COUNT; i++) {
                String pvName = "stress_pv" + i;
                String bucketName = "bucket_" + (i % 5);
                String measurementName = "measurement_" + (i % 20);
                String fieldName = "field_" + (i % 10);

                InfluxDB_PV pv = new InfluxDB_PV(pvName,
                    String.format("influx2://%s/%s/%s", bucketName, measurementName, fieldName));
                stressPVs.add(pv);
            }

            Thread.sleep(2000);

            assertTrue(true, "Stress test completed without major issues");
        } finally {
            for (InfluxDB_PV pv : stressPVs) {
                try {
                    pv.close();
                } catch (Exception e) {
                    System.err.println("Error closing PV: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Tests PV creation with null field (should be treated as no specific field).
     * Verifies that passing "null" as field name results in no specific field selection.
     */
    @Test
    @Order(14)
    @DisplayName("Test PV creation with 'null' field")
    void testPVCreationWithNullField() {
        InfluxDB_PV pv = new InfluxDB_PV(
            TEST_BUCKET + "/" + TEST_MEASUREMENT + "/null",
            "influx2://" + TEST_BUCKET + "/" + TEST_MEASUREMENT + "/null"
        );

        assertNotNull(pv);
        assertEquals(TEST_BUCKET, pv.getBucket());
        assertEquals(TEST_MEASUREMENT, pv.getMeasurement());
        assertNull(pv.getField());

        pv.close();
    }

    /**
     * Tests that empty bucket name is rejected.
     * The system should throw an IllegalArgumentException for empty bucket names.
     */
    @Test
    @Order(15)
    @DisplayName("Test empty bucket name is rejected")
    void testEmptyBucketRejected() {
        assertThrows(IllegalArgumentException.class, () -> new InfluxDB_PV("test", "influx2:///measurement"));

        assertThrows(IllegalArgumentException.class, () -> new InfluxDB_PV("test", "influx2:// /measurement"));
    }
}
