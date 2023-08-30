package org.phoebus.pv.alarm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlarmContextTest {

    @Test
    public void testURLEncoding()
    {
        String pathWithDelimiter = "OPR/TEST/sim://test";
        String pathWithColon = "OPR/TEST/SR:test:pv";
        String pathWithDelimiterAndColon = "OPR/TEST/sim://SR:test:pv";

        String encodedPathWithDelimiter = "OPR/TEST/sim%3A%2F%2Ftest";
        String encodedPathWithColon = "OPR/TEST/SR%3Atest%3Apv";
        String encodedPathWithDelimiterAndColon = "OPR/TEST/sim%3A%2F%2FSR%3Atest%3Apv";

        assertEquals(
                encodedPathWithDelimiter,
                AlarmContext.encodedURLPath(pathWithDelimiter),
                "Failed to encode pv name the delimiter");
        assertEquals(
                encodedPathWithColon,
                AlarmContext.encodedURLPath(pathWithColon),
                "Failed to encode pv name with colon");
        assertEquals(
                encodedPathWithDelimiterAndColon,
                AlarmContext.encodedURLPath(pathWithDelimiterAndColon),
                "Failed to encode pv name with delimiter and colon");
    }

    @Test
    public void testURLDecoding()
    {
        String pathWithDelimiter = "OPR/TEST/sim://test";
        String pathWithColon = "OPR/TEST/SR:test:pv";
        String pathWithDelimiterAndColon = "OPR/TEST/sim://SR:test:pv";

        String encodedPathWithDelimiter = "OPR/TEST/sim%3A%2F%2Ftest";
        String encodedPathWithColon = "OPR/TEST/SR%3Atest%3Apv";
        String encodedPathWithDelimiterAndColon = "OPR/TEST/sim%3A%2F%2FSR%3Atest%3Apv";

        assertEquals(
                pathWithDelimiter,
                AlarmContext.decodedURLPath(encodedPathWithDelimiter),
                "Failed to decode pv name the delimiter");
        assertEquals(
                pathWithColon,
                AlarmContext.decodedURLPath(encodedPathWithColon),
                "Failed to decode pv name with colon");
        assertEquals(
                pathWithDelimiterAndColon,
                AlarmContext.decodedURLPath(encodedPathWithDelimiterAndColon),
                "Failed to decode pv name with delimiter and colon");
    }

    @Test
    public void testKafkaPathDecoding()
    {
        String pathWithDelimiter = "OPR/TEST/sim://test";
        String pathWithColon = "OPR/TEST/SR:test:pv";
        String pathWithDelimiterAndColon = "OPR/TEST/sim://SR:test:pv";

        String encodedPathWithDelimiter = "OPR/TEST/sim:\\/\\/test";
        String encodedPathWithColon = "OPR/TEST/SR:test:pv";
        String encodedPathWithDelimiterAndColon = "OPR/TEST/sim:\\/\\/SR:test:pv";

        assertEquals(
                pathWithDelimiter,
                AlarmContext.decodedKafaPath(encodedPathWithDelimiter),
                "Failed to decode pv kafka path the delimiter");
        assertEquals(
                pathWithColon,
                AlarmContext.decodedKafaPath(encodedPathWithColon),
                "Failed to decode pv kafka path with colon");
        assertEquals(
                pathWithDelimiterAndColon,
                AlarmContext.decodedKafaPath(encodedPathWithDelimiterAndColon),
                "Failed to decode pv kafka path with delimiter and colon");
    }
}
