package org.phoebus.pv.alarm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

        assertEquals("Failed to encode pv name the delimiter", encodedPathWithDelimiter, AlarmContext.encodedURLPath(pathWithDelimiter));
        assertEquals("Failed to encode pv name with colon", encodedPathWithColon, AlarmContext.encodedURLPath(pathWithColon));
        assertEquals("Failed to encode pv name with delimiter and colon", encodedPathWithDelimiterAndColon, AlarmContext.encodedURLPath(pathWithDelimiterAndColon));
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

        assertEquals("Failed to decode pv name the delimiter", pathWithDelimiter, AlarmContext.decodedURLPath(encodedPathWithDelimiter));
        assertEquals("Failed to decode pv name with colon", pathWithColon, AlarmContext.decodedURLPath(encodedPathWithColon));
        assertEquals("Failed to decode pv name with delimiter and colon", pathWithDelimiterAndColon, AlarmContext.decodedURLPath(encodedPathWithDelimiterAndColon));
    }

//    @Test
//    public void testKafkaPathEncoding()
//    {
//        String pathWithDelimiter = "OPR/TEST/sim://test";
//        String pathWithColon = "OPR/TEST/SR:test:pv";
//        String pathWithDelimiterAndColon = "OPR/TEST/sim://SR:test:pv";
//
//        String kafkaPathWithDelimiter = "OPR/TEST/sim:\\/\\/test";
//        String kafkaPathWithColon = "OPR/TEST/SR:test:pv";
//        String kafkaPathWithDelimiterAndColon = "OPR/TEST/sim:\\/\\/SR:test:pv";
//
//        assertEquals("Failed to encode pv kafka path with delimiter", kafkaPathWithDelimiter, AlarmContext.encodedKafkaPath(pathWithDelimiter));
//        assertEquals("Failed to encode pv kafka path with colon", kafkaPathWithColon, AlarmContext.encodedKafkaPath(pathWithColon));
//        assertEquals("Failed to encode pv kafka path with delimiter and colon", kafkaPathWithDelimiterAndColon, AlarmContext.encodedKafkaPath(pathWithDelimiterAndColon));
//    }

    @Test
    public void testKafkaPathDecoding()
    {
        String pathWithDelimiter = "OPR/TEST/sim://test";
        String pathWithColon = "OPR/TEST/SR:test:pv";
        String pathWithDelimiterAndColon = "OPR/TEST/sim://SR:test:pv";

        String encodedPathWithDelimiter = "OPR/TEST/sim:\\/\\/test";
        String encodedPathWithColon = "OPR/TEST/SR:test:pv";
        String encodedPathWithDelimiterAndColon = "OPR/TEST/sim:\\/\\/SR:test:pv";

        assertEquals("Failed to decode pv kafka path the delimiter", pathWithDelimiter, AlarmContext.decodedKafaPath(encodedPathWithDelimiter));
        assertEquals("Failed to decode pv kafka path with colon", pathWithColon, AlarmContext.decodedKafaPath(encodedPathWithColon));
        assertEquals("Failed to decode pv kafka path with delimiter and colon", pathWithDelimiterAndColon, AlarmContext.decodedKafaPath(encodedPathWithDelimiterAndColon));
    }
}
