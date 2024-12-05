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
import org.phoebus.logbook.LogEntry;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Stage} subclass rendering a UI for the purpose of editing/submitting a {@link LogEntry}.
 * Callers that need to handle the outcome (i.e. when the {@link Stage} is closed) should
 * call first call {@link #showAndWait()} and then get a potential result using {@link #getLogEntryResult()}.
 */
public class LogEntryEditorStage extends Stage
{

    private LogEntryEditorController logEntryEditorController;
      /**
     * A stand-alone window containing components needed to create a logbook entry.
     * @param parent The {@link Node} from which the user - through context menu or application menu - requests a new
     *               logbook entry.
     * @param logEntryModel Pre-populated data for the log entry, e.g. date and (optionally) screenshot.
     */
    public LogEntryEditorStage(Node parent, LogEntryModel logEntryModel)
    {
        initModality(Modality.APPLICATION_MODAL);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LogEntryEditor.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                if(clazz.isAssignableFrom(LogEntryEditorController.class)){
                    logEntryEditorController = (LogEntryEditorController) clazz.getConstructor(Node.class, LogEntryModel.class)
                            .newInstance(parent, logEntryModel);
                    return logEntryEditorController;
                }
                else if(clazz.isAssignableFrom(FieldsViewController.class)){
                    return clazz.getConstructor(LogEntryModel.class)
                            .newInstance(logEntryModel);
                }
                else if(clazz.isAssignableFrom(AttachmentsViewController.class)){
                    return clazz.getConstructor(Node.class, List.class, List.class, Boolean.class)
                                    .newInstance(parent, logEntryModel.getImages(), logEntryModel.getFiles(), true);
                }
            } catch (Exception e) {
                Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.SEVERE, "Failed to construct controller for log editor UI", e);
            }
            return null;
        });

        try {
            fxmlLoader.load();
        } catch (
                IOException exception) {
            Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.WARNING, "Unable to load fxml for log entry editor UI", exception);
        }

        Scene scene = new Scene(fxmlLoader.getRoot());
        setScene(scene);
    }

    /**
     *
     * @return A potentially empty result of {@link LogEntry} submission.
     */
    public Optional<LogEntry> getLogEntryResult(){
        return logEntryEditorController.getLogEntryResult();
    }
}
