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

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.ui.Messages;

import java.io.IOException;

/**
 * Controller for the {@link LogEntryEditorStage}.
 */
public class LogEntryEditorController {

    private Node parent;
    private LogEntryModel model;
    private LogEntryCompletionHandler completionHandler;


    @FXML
    private VBox fields;
    @FXML
    private VBox attachments;
    @FXML
    private Button cancel;
    @FXML
    private Button submit;

    @FXML
    private AttachmentsViewController attachmentsViewController;

    public LogEntryEditorController(Node parent, LogEntryModel model, LogEntryCompletionHandler logEntryCompletionHandler){
        this.parent = parent;
        this.model = model;
        this.completionHandler = logEntryCompletionHandler;
    }

    @FXML
    public void initialize(){

        localize();
        submit.disableProperty().bind(model.getReadyToSubmitProperty().not());

    }

    /**
     * Handler for Cancel button
     */
    @FXML
    public void cancel(){
        ((Stage)cancel.getScene().getWindow()).close();
    }

    /**
     * Handler for the Submit button
     */
    @FXML
    public void submit(){
        model.setImages(attachmentsViewController.getImages());
        model.setFiles(attachmentsViewController.getFiles());
        LogEntry logEntry = null;
        try {
            logEntry = model.submitEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(completionHandler != null){
            completionHandler.handleResult(logEntry);
        }
        cancel();
    }

    private void localize(){
        submit.setText(Messages.Submit);
        submit.setTooltip(new Tooltip(Messages.SubmitTooltip));
        cancel.setText(Messages.Cancel);
        cancel.setTooltip(new Tooltip(Messages.CancelTooltip));
    }
}
