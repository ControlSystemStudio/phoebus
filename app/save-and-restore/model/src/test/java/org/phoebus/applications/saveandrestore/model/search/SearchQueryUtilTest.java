/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.applications.saveandrestore.model.search;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SearchQueryUtilTest {

    @Test
    public void testParseHumanReadableString1(){
        String query = "name=aName&type=Folder, Snapshot&user=John&desc=foo&start=7 days&end=now";
        Map<String, String> map = SearchQueryUtil.parseHumanReadableQueryString(query);
        assertEquals("aName", map.get(Keys.NAME.getName()));
        assertEquals("Folder,Snapshot", map.get(Keys.TYPE.getName()));
        assertEquals("John", map.get(Keys.USER.getName()));
        assertEquals("foo", map.get(Keys.DESC.getName()));
        assertEquals("7 days", map.get(Keys.STARTTIME.getName()));
        assertEquals("now", map.get(Keys.ENDTIME.getName()));
    }

    @Test
    public void testParseHumanReadableString2(){
        String query = "name=aName&unsupported=value";
        Map<String, String> map = SearchQueryUtil.parseHumanReadableQueryString(query);
        assertEquals("aName", map.get(Keys.NAME.getName()));
        assertNull(map.get("unsupported"));
    }

    @Test
    public void testParseHumanReadableString4(){
        String query = "name=";
        Map<String, String> map = SearchQueryUtil.parseHumanReadableQueryString(query);
        assertEquals("", map.get(Keys.NAME.getName()));
        query = "name";
        map = SearchQueryUtil.parseHumanReadableQueryString(query);
        assertEquals("*", map.get(Keys.NAME.getName()));
    }

    @Test
    public void testParseHumanReadableString3(){
        assertTrue(SearchQueryUtil.parseHumanReadableQueryString(null).isEmpty());
        assertTrue(SearchQueryUtil.parseHumanReadableQueryString("").isEmpty());
    }

    @Test
    public void testSearchParamsToString1(){
        Map<String, String> map = new HashMap<>();
        map.put(Keys.NAME.getName(), "aName");
        map.put(Keys.TYPE.getName(), "Folder,Configuration");
        map.put(Keys.DESC.getName(), "foo");
        map.put(Keys.USER.getName(), "John");
        map.put(Keys.STARTTIME.getName(), "7 days");
        map.put(Keys.ENDTIME.getName(), "now");

        String queryString = SearchQueryUtil.toQueryString(map);
        assertTrue(queryString.contains(Keys.NAME.getName() + "=aName"));
        assertTrue(queryString.contains(Keys.TYPE.getName() + "=Folder,Configuration"));
        assertTrue(queryString.contains(Keys.DESC.getName() + "=foo"));
        assertTrue(queryString.contains(Keys.USER.getName() + "=John"));
        assertTrue(queryString.contains(Keys.STARTTIME.getName() + "=7 days"));
        assertTrue(queryString.contains(Keys.ENDTIME.getName() + "=now"));
    }

    @Test
    public void testSearchParamsToString2(){
        Map<String, String> map = new HashMap<>();
        map.put(Keys.NAME.getName(), "aName");
        map.put("unsupported", "value");

        String queryString = SearchQueryUtil.toQueryString(map);
        assertTrue(queryString.contains(Keys.NAME.getName() + "=aName"));
        assertFalse(queryString.contains("unsupported"));
    }

    @Test
    public void testSearchParamsToString3(){
        assertEquals("", SearchQueryUtil.toQueryString(null));
    }
}
