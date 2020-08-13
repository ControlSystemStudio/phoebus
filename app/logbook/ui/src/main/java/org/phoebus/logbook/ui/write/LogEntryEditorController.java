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

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.ui.Messages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the {@link LogEntryEditorStage}.
 */
public class LogEntryEditorController {

    private Node parent;
    private LogEntryModel model;
    private LogEntryCompletionHandler completionHandler;

    private Logger logger = Logger.getLogger(LogEntryEditorController.class.getName());

    @FXML
    private VBox fields;
    @FXML
    private VBox attachments;
    @FXML
    private Button cancel;
    @FXML
    private Button submit;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label completionMessageLabel;
    @FXML
    private AttachmentsViewController attachmentsViewController;

    private ExecutorService executorService;

    private SimpleBooleanProperty progressIndicatorVisibility =
            new SimpleBooleanProperty(false);


    public LogEntryEditorController(Node parent, LogEntryModel model, LogEntryCompletionHandler logEntryCompletionHandler){
        this.parent = parent;
        this.model = model;
        this.completionHandler = logEntryCompletionHandler;
        this.executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @FXML
    public void initialize(){
        localize();
        submit.disableProperty().bind(model.getReadyToSubmitProperty().not());
        completionMessageLabel.visibleProperty().bind(completionMessageLabel.textProperty().isNotEmpty());
        progressIndicator.visibleProperty().bind(progressIndicatorVisibility);
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
        progressIndicatorVisibility.setValue(true);
        completionMessageLabel.textProperty().setValue("");
        model.setImages(attachmentsViewController.getImages());
        model.setFiles(attachmentsViewController.getFiles());
        try {
            Future<LogEntry> future = executorService.submit(() -> model.submitEntry());
            LogEntry result = future.get();
            if(result != null){
                if(completionHandler != null){
                    completionHandler.handleResult(result);
                }
                cancel();
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Unable to submit log entry", e);
            completionMessageLabel.textProperty().setValue(org.phoebus.logbook.Messages.SubmissionFailed);
        } catch(ExecutionException e){
            logger.log(Level.WARNING, "Unable to submit log entry", e);
            if(e.getCause() != null && e.getCause().getMessage() != null){
                completionMessageLabel.textProperty().setValue(e.getCause().getMessage());
            }
            else if(e.getMessage() != null){
                completionMessageLabel.textProperty().setValue(e.getMessage());
            }
            else{
                completionMessageLabel.textProperty().setValue(org.phoebus.logbook.Messages.SubmissionFailed);
            }
        }
        finally {
            progressIndicatorVisibility.setValue(false);
        }
    }

    private void localize(){
        submit.setText(Messages.Submit);
        submit.setTooltip(new Tooltip(Messages.SubmitTooltip));
        cancel.setText(Messages.Cancel);
        cancel.setTooltip(new Tooltip(Messages.CancelTooltip));
    }
}
