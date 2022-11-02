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

package org.phoebus.olog.es.api.model;

import org.junit.jupiter.api.Test;
import org.phoebus.logbook.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OlogPropertyTest {

    @Test
    public void testEquals(){
        Property property1 = new OlogProperty("name1");
        Property property2 = new OlogProperty("name2");
        assertFalse(property1.equals(property2));

        property2 = new OlogProperty("name1");
        assertTrue(property1.equals(property2));

        assertFalse(property1.equals(new Object()));
    }

    @Test
    public void testHashCode(){
        Property property1 = new OlogProperty("name1");
        Property property2 = new OlogProperty("name2");
        assertNotEquals(property1.hashCode(), property2.hashCode());

        property2 = new OlogProperty("name1");
        assertEquals(property1.hashCode(), property2.hashCode());

        assertNotEquals(property1.hashCode(), new Object().hashCode());
    }
}
