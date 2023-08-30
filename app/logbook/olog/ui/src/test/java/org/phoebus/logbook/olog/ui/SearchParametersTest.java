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
 */

package org.phoebus.logbook.olog.ui;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchParametersTest {

    @Test
    public void testGetValue() {
        SearchParameters searchParameters = new SearchParameters();

        String query = "start=12 hours&end=now&foo=bar&desc=abc&owner=John Doe&tags=A,B,C&logbooks=a,b,c&level=Event";
        searchParameters.setQuery(query);
        String value = searchParameters.getValue();

        assertTrue(value.contains("start=12 hours"));
        assertTrue(value.contains("end=now"));
        assertTrue(value.contains("desc=abc"));
        assertTrue(value.contains("owner=John Doe"));
        assertTrue(value.contains("tags=A,B,C"));
        assertTrue(value.contains("logbooks=a,b,c"));
        assertTrue(value.contains("level=Event"));

        assertFalse(value.contains("foo=bar"));
    }
}
