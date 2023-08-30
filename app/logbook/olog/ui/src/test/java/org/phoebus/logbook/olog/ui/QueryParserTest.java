package org.phoebus.logbook.olog.ui;

import org.junit.jupiter.api.Test;
import org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

/**
 * Test the parsing of the logbook query URI syntax
 *
 * @author Kunal Shroff
 */
public class QueryParserTest {

    @Test
    public void basic() {
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tags=operation");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("desc", "*Fault*Motor*");
        expectedMap.put("tags", "operation");
        assertEquals(expectedMap, queryParameters);

        // Also test empty query
        uri = URI.create("logbook://?");
        queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        assertTrue(queryParameters.isEmpty());

        String query = "desc=*Fault*Motor*&tags=operation&start=2 days&end=now";
        queryParameters = LogbookQueryUtil.parseHumanReadableQueryString(query);
        assertEquals("*Fault*Motor*", queryParameters.get("desc"));
        assertEquals("operation", queryParameters.get("tags"));
        assertEquals("2 days", queryParameters.get("start"));
        assertEquals("now", queryParameters.get("end"));

        queryParameters = LogbookQueryUtil.parseQueryString(query);
        assertEquals("*Fault*Motor*", queryParameters.get("desc"));
        assertEquals("operation", queryParameters.get("tags"));
        assertEquals(23, queryParameters.get("start").length());
        assertEquals(23, queryParameters.get("end").length());

        uri = URI.create("logbook://?");
        assertTrue(LogbookQueryUtil.parseQueryURI(uri).isEmpty());

        assertTrue(LogbookQueryUtil.parseQueryString(null).isEmpty());
        assertTrue(LogbookQueryUtil.parseQueryString("").isEmpty());

        assertTrue(LogbookQueryUtil.parseHumanReadableQueryString(null).isEmpty());
        assertTrue(LogbookQueryUtil.parseHumanReadableQueryString("").isEmpty());
    }

    @Test
    public void timeParsing() {
        long now = System.currentTimeMillis();
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tags=operation&start=8hours&end=now");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        assertEquals("*Fault*Motor*", queryParameters.get(Keys.DESC.getName()));
        assertEquals("operation", queryParameters.get(Keys.TAGS.getName()));
        assertEquals(now, Instant.from(MILLI_FORMAT.parse(queryParameters.get(Keys.ENDTIME.getName()))).toEpochMilli(), 60000);
        assertEquals((now - (8 * 60 * 60 * 1000)), Instant.from(MILLI_FORMAT.parse(queryParameters.get(Keys.STARTTIME.getName()))).toEpochMilli(), 60000);
    }

    /**
     * Using a key work with no search pattern value is treated the same as ANY
     */
    @Test
    public void emptyValueTest() {
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tags=operation&logbooks");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("desc", "*Fault*Motor*");
        expectedMap.put("tags", "operation");
        expectedMap.put("logbooks", "*");
        assertEquals(expectedMap, queryParameters);
    }

    @Test
    public void testDuplicateKeys() {
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tags=operation&desc=foo");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        assertEquals(2, queryParameters.size());

        String query = "desc=*Fault*Motor*&tags=operation&desc=foo";
        queryParameters = LogbookQueryUtil.parseHumanReadableQueryString(query);
        assertEquals(2, queryParameters.size());

        queryParameters = LogbookQueryUtil.parseQueryString(query);
        assertEquals(2, queryParameters.size());
    }

    @Test
    public void testHiddenKeys() {
        URI uri = URI.create("logbook://?desc=*Fault*Motor*&tags=operation&from=0&size=0&limit=0&sort=up");
        Map<String, String> queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        assertEquals(2, queryParameters.size());

        String query = "desc=*Fault*Motor*&tags=operation&from=0&size=0&limit=0&sort=up";
        queryParameters = LogbookQueryUtil.parseHumanReadableQueryString(query);
        assertEquals(2, queryParameters.size());

        queryParameters = LogbookQueryUtil.parseQueryString(query);
        assertEquals(2, queryParameters.size());
    }

}
