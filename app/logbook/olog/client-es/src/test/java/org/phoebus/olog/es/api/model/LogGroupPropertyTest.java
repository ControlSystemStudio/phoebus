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
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class LogGroupPropertyTest {

    @Test
    public void testHasLogGroup() {
        Map<String, String> attributes1 = new HashMap<>();
        attributes1.put(LogGroupProperty.ATTRIBUTE_ID, "id value");
        Property property1 = PropertyImpl.of(LogGroupProperty.NAME, attributes1);

        Map<String, String> attributes2 = new HashMap<>();
        attributes2.put("whatever", "id value");
        Property property2 = PropertyImpl.of("whatever", attributes2);

        OlogLog logEntry = new OlogLog();
        logEntry.setProperties(Arrays.asList(property1, property2));

        assertEquals("id value", LogGroupProperty.getLogGroupProperty(logEntry).get().getAttributes().get(LogGroupProperty.ATTRIBUTE_ID));
    }

    @Test
    public void testHasNotLogGroup1() {
        Map<String, String> attributes1 = new HashMap<>();
        attributes1.put(LogGroupProperty.ATTRIBUTE_ID, "");
        Property property1 = PropertyImpl.of(LogGroupProperty.NAME, attributes1);

        OlogLog logEntry = new OlogLog();
        logEntry.setProperties(List.of(property1));

        assertTrue(LogGroupProperty.getLogGroupProperty(logEntry).isEmpty());
    }

    @Test
    public void testHasNotLogGroup2() {
        OlogLog logEntry = new OlogLog();
        assertTrue(LogGroupProperty.getLogGroupProperty(logEntry).isEmpty());
    }

    @Test
    public void testHasNotLogGroup3() {
        Map<String, String> attributes1 = new HashMap<>();
        attributes1.put("attr 1", "id value");
        Property property1 = PropertyImpl.of("prop1", attributes1);

        Map<String, String> attributes2 = new HashMap<>();
        attributes2.put("attr 2", "id value");
        Property property2 = PropertyImpl.of("prop2", attributes2);

        OlogLog logEntry = new OlogLog();
        logEntry.setProperties(Arrays.asList(property1, property2));

        assertTrue(LogGroupProperty.getLogGroupProperty(logEntry).isEmpty());
    }

    @Test
    public void testCreate(){
        Property property = LogGroupProperty.create();
        assertEquals(1, property.getAttributes().size());
        assertNotNull(property.getAttributes().get(LogGroupProperty.ATTRIBUTE_ID));
    }

    @Test
    public void testCheckLogEntryGroupProperty(){
        Map<String, String> attributes1 = new HashMap<>();
        attributes1.put(LogGroupProperty.ATTRIBUTE_ID, "id value");
        Property property1 = PropertyImpl.of(LogGroupProperty.NAME, attributes1);

        OlogLog logEntry = new OlogLog();
        logEntry.setProperties(List.of(property1));

        Map<String, String> attributes2 = new HashMap<>();
        attributes1.put("another attr", "value");
        Property property2 = PropertyImpl.of("another name", attributes2);

        OlogLog logEntry2 = new OlogLog();
        logEntry2.setProperties(List.of(property2));

        try {
            Property property = LogGroupProperty.getLogEntryGroupProperty(Arrays.asList(logEntry, logEntry2));
            assertEquals("id value", property.getAttributes().get(LogGroupProperty.ATTRIBUTE_ID));

        } catch (LogbookException e) {
            fail();
        }
    }

    @Test
    public void testCheckLogEntryGroupPropertyNoEntryHasProperties(){
        Map<String, String> attributes1 = new HashMap<>();
        attributes1.put("attr", "id value");
        Property property1 = PropertyImpl.of("some name", attributes1);

        OlogLog logEntry = new OlogLog();
        logEntry.setProperties(List.of(property1));

        Map<String, String> attributes2 = new HashMap<>();
        attributes1.put("another attr", "value");
        Property property2 = PropertyImpl.of("another name", attributes2);

        OlogLog logEntry2 = new OlogLog();
        logEntry2.setProperties(List.of(property2));

        try {
            Property property = LogGroupProperty.getLogEntryGroupProperty(Arrays.asList(logEntry, logEntry2));
            assertNotNull(property.getAttributes().get(LogGroupProperty.ATTRIBUTE_ID));

        } catch (LogbookException e) {
            fail();
        }
    }

    @Test
    public void testCheckLogEntryGroupPropertyMultipleLogEntryGroups(){

        OlogLog logEntry = new OlogLog();
        logEntry.setProperties(List.of(LogGroupProperty.create()));

        OlogLog logEntry2 = new OlogLog();
        logEntry2.setProperties(List.of(LogGroupProperty.create()));

        assertThrows(LogbookException.class,
                () -> LogGroupProperty.getLogEntryGroupProperty(Arrays.asList(logEntry, logEntry2)));
    }
}
