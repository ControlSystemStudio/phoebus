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
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.Property;
import org.phoebus.olog.es.api.model.LogGroupProperty;
import org.phoebus.olog.es.api.model.OlogProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogEntryGroupTest {


    @Test
    public void testGetLogEntryGroupProperty1() {

        Map<String, String> attributes = new HashMap<>();
        String uuid = UUID.randomUUID().toString();
        attributes.put(LogGroupProperty.ATTRIBUTE_ID, uuid);
        Property logGroupProperty = new OlogProperty(LogGroupProperty.NAME, attributes);

        LogEntry logEntry1 = LogEntryBuilder.log()
                .property(logGroupProperty)
                .description("")
                .build();

        assertTrue(LogGroupProperty.getLogGroupProperty(logEntry1).isPresent());

        logEntry1 = LogEntryBuilder.log()
                .description("")
                .build();

        assertFalse(LogGroupProperty.getLogGroupProperty(logEntry1).isPresent());

        logGroupProperty = new OlogProperty("Some other name", attributes);
        logEntry1 = LogEntryBuilder.log()
                .property(logGroupProperty)
                .description("")
                .build();

        assertFalse(LogGroupProperty.getLogGroupProperty(logEntry1).isPresent());
    }
}
