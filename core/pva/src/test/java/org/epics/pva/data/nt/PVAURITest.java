/*
 *
 * Copyright (C) 2023 European Spallation Source ERIC.
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
 */

package org.epics.pva.data.nt;

import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PVAURITest {
    @Test
    public void testDefaultValues() {
        PVAURI uriAllDefault = new PVAURI("name", "pva", null, "path", null);
        PVAURI uriNonOptDefault = new PVAURI("name", "pva", "path");
        PVAURI uriDefault = new PVAURI("name", "path");

        assertEquals(uriDefault, uriNonOptDefault);
        assertEquals(uriNonOptDefault, uriAllDefault);
    }

    @Test
    public void testFullConstructors() throws NotValueException {
        Map<String, String> queries = new HashMap<>();
        queries.put("number", "1");
        PVAURI uriAll = new PVAURI("name", "pva", "authority", "path", queries);

        assertEquals("pva", uriAll.getScheme());
        assertEquals("authority", uriAll.getAuthority());
        assertEquals("path", uriAll.getPath());
        assertEquals(queries, uriAll.getQuery());
    }

    @Test
    public void testFromStructure() {
        Map<String, String> queries = new HashMap<>();
        queries.put("number", "1");
        PVAURI uriAll = new PVAURI("name", "pva", "authority", "path", queries);

        assertEquals(uriAll, PVAURI.fromStructure(uriAll));
    }

    @Test
    public void testNotValueException() {
        PVAStructure query = new PVAStructure("query", "structure",
                new PVAStructure("bad value", "bad structure"));
        PVAStructure uriStructure = new PVAStructure("name", PVAURI.STRUCT_NAME,
                new PVAString("scheme"), new PVAString("path"), new PVAString("authority"), query);
        PVAURI uri = PVAURI.fromStructure(uriStructure);
        assertThrows(NotValueException.class, uri::getQuery);
    }

    @Test
    void testNullQuery() throws NotValueException {
        PVAURI uri = new PVAURI("name", "path");
        assertEquals(new HashMap<String, String>(), uri.getQuery());
    }
}