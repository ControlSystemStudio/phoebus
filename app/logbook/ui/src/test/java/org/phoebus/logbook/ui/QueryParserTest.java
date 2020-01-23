package org.phoebus.logbook.ui;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.phoebus.logbook.ui.LogbookQueryUtil.Keys;

import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;
/**
 * Test the parsing of the logbook query URI syntax
 * 
 * @author Kunal Shroff
 *
 */
public class QueryParserTest {

    @Test
    public void basic() {
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tag=operation");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("desc", "*Fault*Motor*");
        expectedMap.put("tag", "operation");
        assertEquals(expectedMap, queryParameters);

    }

    @Test
    public void timeParsing() {
        long now = System.currentTimeMillis();
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tag=operation&start=8hours&end=now");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        assertEquals("*Fault*Motor*", queryParameters.get(Keys.SEARCH.getName()));
        assertEquals("operation", queryParameters.get(Keys.TAGS.getName()));
        assertEquals(now, Instant.from(MILLI_FORMAT.parse(queryParameters.get(Keys.ENDTIME.getName()))).toEpochMilli(), 60000);
        assertEquals((now-(8*60*60*1000)), Instant.from(MILLI_FORMAT.parse(queryParameters.get(Keys.STARTTIME.getName()))).toEpochMilli(), 60000);
    }

    /**
     * Using a key work with no search pattern value is treated the same as ANY
     */
    @Test
    public void emptyValueTest() {
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tag=operation&logbook");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("desc", "*Fault*Motor*");
        expectedMap.put("tag", "operation");
        expectedMap.put("logbook", "*");
        assertEquals(expectedMap, queryParameters);
    }

    /**
     * Since the current logbook client api does not support multi value maps reusing the same query key word should throw an exception.
     */
    @Test(expected = Exception.class)
    public void multiValueTest() {
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tag=operation&tag=loto");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
    }
}
