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

import org.junit.Test;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.olog.es.api.model.LogGroupProperty;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LogEntryTableViewControllerTest {

    @Test
    public void testGetLogEntryGroupProperty1() throws LogbookException {
        Property p = LogGroupProperty.create();
        LogEntry logEntry1 = LogEntryBuilder.log()
                .property(p)
                .description("")
                .build();
        LogEntry logEntry2 = LogEntryBuilder.log()
                .description("")
                .build();

        LogEntryTableViewController controller =
                new LogEntryTableViewController(null);
        Property property = controller.getLogEntryGroupProperty(Arrays.asList(logEntry1, logEntry2));
        assertEquals(p.getAttributes().get(LogGroupProperty.ATTRIBUTE_ID), property.getAttributes().get(LogGroupProperty.ATTRIBUTE_ID));
    }

    @Test
    public void testGetLogEntryGroupProperty2() throws LogbookException {
        LogEntry logEntry1 = LogEntryBuilder.log()
                .description("")
                .build();
        LogEntry logEntry2 = LogEntryBuilder.log()
                .description("")
                .build();

        LogEntryTableViewController controller =
                new LogEntryTableViewController(null);
        Property property = controller.getLogEntryGroupProperty(Arrays.asList(logEntry1, logEntry2));
        assertNotNull(property);
        assertNotNull(property.getAttributes().get(LogGroupProperty.ATTRIBUTE_ID));
    }

    @Test(expected = LogbookException.class)
    public void testGetLogEntryGroupProperty3() throws LogbookException {
        LogEntry logEntry1 = LogEntryBuilder.log()
                .property(LogGroupProperty.create())
                .description("")
                .build();
        LogEntry logEntry2 = LogEntryBuilder.log()
                .property(LogGroupProperty.create())
                .description("")
                .build();
        LogEntryTableViewController controller =
                new LogEntryTableViewController(null);
        controller.getLogEntryGroupProperty(Arrays.asList(logEntry1, logEntry2));
    }
}
