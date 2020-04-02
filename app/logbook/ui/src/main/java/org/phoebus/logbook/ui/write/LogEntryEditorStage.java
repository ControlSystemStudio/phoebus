/*
 * Copyright (C) 2019 European Spallation Source ERIC.
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

package org.phoebus.logbook.ui.write;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class LogEntryEditorStage extends Stage
{

    /** Width of labels on views leftmost column. */
    public static final int labelWidth = 80;

    /**
     * A stand-alone window containing components needed to create a logbook entry.
     * @param parent The {@link Node} from which the user - through context menu or application menu - requests a new
     *               logbook entry.
     * @param logEntryModel Pre-populated data for the log entry, e.g. date and (optionally) screen shot.
     * @param completionHandler If non-null, called when the submission to the logbook service has completed.
     */
    public LogEntryEditorStage(Node parent, LogEntryModel logEntryModel, LogEntryCompletionHandler completionHandler)
    {
        initModality(Modality.APPLICATION_MODAL);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LogEntryEditor.fxml"));
        fxmlLoader.setController(new LogEntryEditorController(parent, logEntryModel, completionHandler));
        try {
            fxmlLoader.load();
        } catch (
                IOException exception) {
            throw new RuntimeException(exception);
        }

        Scene scene = new Scene(fxmlLoader.getRoot());
        setScene(scene);
    }
}
