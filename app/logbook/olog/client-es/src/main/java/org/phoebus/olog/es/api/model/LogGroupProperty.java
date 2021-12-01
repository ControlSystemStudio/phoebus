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

import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LogGroupProperty {

    public static final String NAME = "Log Entry Group";
    public static final String ATTRIBUTE_ID = "id";

    /**
     * Iterates through the list of log entries and collects all log group ids. The purpose is to establish
     * whether the selected list contains log entries belonging to different log entry groups.
     *
     * @param logEntries List of (user selected) log entries
     * @return The log entry group {@link Property} if only one single is found. If none are found a
     * new log entry group {@link Property} is returned.
     * @throws LogbookException if more than one log entry group {@link Property} is encountered.
     */
    public static Property getLogEntryGroupProperty(List<LogEntry> logEntries) throws LogbookException {
        List<Property> logGroupProperties = getLogEntryGroupProperties(logEntries);
        if (logGroupProperties.isEmpty()) {
            return create();
        }
        if (logGroupProperties.size() == 1) {
            return logGroupProperties.get(0);
        }
        throw new LogbookException("More than one log entry group property found.");
    }

    private static List<Property> getLogEntryGroupProperties(List<LogEntry> logEntries) {
        List<Property> logEntryGroupProperties = new ArrayList<>();
        logEntries.forEach(l -> {
            Optional<Property> logGroupProperty =
                    LogGroupProperty.getLogGroupProperty(l);
            if (logGroupProperty.isPresent()) {
                logEntryGroupProperties.add(logGroupProperty.get());
            }
        });
        return logEntryGroupProperties;
    }


    /**
     * @param logEntry
     * @return The value of the Log Entry Group id attribute, if property exists in the log entry
     * and if id attribute value is non-null and non-empty.
     */
    public static Optional<Property> getLogGroupProperty(LogEntry logEntry) {
        Collection<Property> properties = logEntry.getProperties();
        if (properties == null || properties.isEmpty()) {
            return Optional.empty();
        }
        Optional<Property> property =
                properties.stream().filter(p -> p.getName().equals(NAME)).findFirst();
        if (property.isPresent()) {
            String id = property.get().getAttributes().get(ATTRIBUTE_ID);
            if (id != null && !id.isEmpty()) {
                return Optional.of(property.get());
            } else return Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    /**
     * @return A new {@link Property} identifying a unique log entry group.
     */
    public static Property create() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(ATTRIBUTE_ID, UUID.randomUUID().toString());
        return new OlogProperty(NAME, attributes);
    }
}
