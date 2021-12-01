package org.phoebus.logbook.olog.ui;

import org.junit.Test;
import org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

        // Also test empty query
        uri = URI.create("logbook://?");
        queryParameters = LogbookQueryUtil.parseQueryURI(uri);
        assertTrue(queryParameters.isEmpty());

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

    @Test
    public void testDetermineSortOrder(){
        String query = "a=b&C=D";
        String modifiedQuery = LogbookQueryUtil.addSortOrder(query, false);
        assertEquals("a=b&C=D&sort=down", modifiedQuery);

        modifiedQuery = LogbookQueryUtil.addSortOrder(query, true);
        assertEquals("a=b&C=D&sort=up", modifiedQuery);

        query = "a=b&sort=up&C=D";
        modifiedQuery = LogbookQueryUtil.addSortOrder(query, false);
        assertEquals("a=b&C=D&sort=down", modifiedQuery);

        query = "a=b&sort&C=D";
        modifiedQuery = LogbookQueryUtil.addSortOrder(query, false);
        assertEquals("a=b&C=D&sort=down", modifiedQuery);

        query = "a=b&sort=down&C=D";
        modifiedQuery = LogbookQueryUtil.addSortOrder(query, true);
        assertEquals("a=b&C=D&sort=up", modifiedQuery);

        // Also test empty query
        query = "";
        modifiedQuery = LogbookQueryUtil.addSortOrder(query, true);
        assertEquals("sort=up", modifiedQuery);

        query = null;
        modifiedQuery = LogbookQueryUtil.addSortOrder(query, true);
        assertEquals("sort=up", modifiedQuery);
    }
}
