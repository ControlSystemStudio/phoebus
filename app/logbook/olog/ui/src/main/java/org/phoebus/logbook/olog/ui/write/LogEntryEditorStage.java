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

package org.phoebus.logbook.olog.ui.write;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.framework.nls.NLS;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.olog.ui.AttachmentsPreviewController;
import org.phoebus.logbook.olog.ui.Messages;

import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogEntryEditorStage extends Stage
{
    /**
     * A stand-alone window containing components needed to create a logbook entry.
     * @param logEntry Pre-populated data for the log entry, e.g. date and (optionally) screen shot.
     */
    public LogEntryEditorStage(LogEntry logEntry)
    {
        this(logEntry, null, null);
    }

    /**
     * A stand-alone window containing components needed to create a logbook entry.
     * @param logEntry Pre-populated data for the log entry, e.g. date and (optionally) screen shot.
     * @param completionHandler A completion handler called when service call completes.
     */
    public LogEntryEditorStage(LogEntry logEntry, LogEntry replyTo, LogEntryCompletionHandler completionHandler)
    {
        initModality(Modality.WINDOW_MODAL);
        ResourceBundle resourceBundle =  NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader =
                new FXMLLoader(getClass().getResource("LogEntryEditor.fxml"), resourceBundle);
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                if(clazz.isAssignableFrom(LogEntryEditorController.class)){
                    return clazz.getConstructor(LogEntry.class, LogEntry.class, LogEntryCompletionHandler.class)
                            .newInstance(logEntry, replyTo, completionHandler);
                }
                else if(clazz.isAssignableFrom(AttachmentsViewController.class)){
                    return clazz.getConstructor(LogEntry.class)
                                    .newInstance(logEntry);
                }
                else if(clazz.isAssignableFrom(AttachmentsPreviewController.class)){
                    return clazz.getConstructor().newInstance();
                }
                else if(clazz.isAssignableFrom(LogPropertiesEditorController.class)) {
                    return clazz.getConstructor(Collection.class).newInstance(logEntry.getProperties());
                }
            } catch (Exception e) {
                Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.SEVERE, "Failed to construct controller for log editor UI", e);
            }
            return null;
        });

        try {
            fxmlLoader.load();
        } catch (
                Exception exception) {
            Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.WARNING, "Unable to load fxml for log entry editor UI", exception);
        }

        Scene scene = new Scene(fxmlLoader.getRoot());
        setScene(scene);
    }
}
