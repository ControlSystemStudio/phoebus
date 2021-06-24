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

import org.phoebus.logbook.LogEntry;

import java.util.List;

/**
 * Wrapper around a {@link LogEntry} as selected by the user from the search result list,
 * and a list of {@link LogEntry}s defining a group of linked entries by means of the
 * special purpose property {@link org.phoebus.olog.es.api.model.LogGroupProperty}. The
 * selected log entry <b>must</b> be included in the list of related log entries.
 *
 * If the selected {@link LogEntry} is not included in a log entry group, the list of
 * related log entries will be null or empty.
 */
public class LogEntryGroup {

    private LogEntry selectedLogEntry;
    private List<LogEntry> groupedLogEntries;

    public LogEntryGroup(LogEntry selectedLogEntry){
        this.selectedLogEntry = selectedLogEntry;
    }

    public LogEntryGroup(LogEntry selectedLogEntry, List<LogEntry> relatedLogEntries){
        this.selectedLogEntry = selectedLogEntry;
        this.groupedLogEntries = relatedLogEntries;
    }

    public LogEntry getSelectedLogEntry(){
        return selectedLogEntry;
    }

    public List<LogEntry> getGroupedLogEntries(){
        return groupedLogEntries;
    }
}
