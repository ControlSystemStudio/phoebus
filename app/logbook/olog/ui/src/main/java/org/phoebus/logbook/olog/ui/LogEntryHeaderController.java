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

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.phoebus.logbook.LogEntry;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

/**
 * Simple controller used to set log entry UI elements in the title of the {@link javafx.scene.control.TitledPane}.
 */
public class LogEntryHeaderController {

    @FXML
    private Label time;

    @FXML
    private Label author;

    public void setLogEntry(LogEntry logEntry){
        time.setText(SECONDS_FORMAT.format(logEntry.getCreatedDate()));
        author.setText(logEntry.getOwner());
    }
}
