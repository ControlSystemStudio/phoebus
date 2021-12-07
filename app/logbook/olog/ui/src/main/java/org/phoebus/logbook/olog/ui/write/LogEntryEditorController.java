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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogFactory;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.olog.ui.LogbookUIPreferences;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.security.tokens.SimpleAuthenticationToken;

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
    private LogEntryCompletionHandler completionHandler;

    private Logger logger = Logger.getLogger(LogEntryEditorController.class.getName());

    @FXML
    private Button submitButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label completionMessageLabel;
    @FXML
    private AttachmentsViewController attachmentsViewController;
    @FXML
    private LogPropertiesEditorController logPropertiesEditorController;
    @FXML
    private FieldsViewController fieldsViewController;
    @FXML
    private TextArea textArea;

    private LogFactory logFactory;

    private ExecutorService executorService;

    private SimpleBooleanProperty progressIndicatorVisibility =
            new SimpleBooleanProperty(false);

    private LogEntry replyTo;


    public LogEntryEditorController(Node parent, LogEntry replyTo, LogEntryCompletionHandler logEntryCompletionHandler) {
        this.parent = parent;
        this.replyTo = replyTo;
        this.completionHandler = logEntryCompletionHandler;
        this.executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        this.logFactory = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory);
    }

    @FXML
    public void initialize() {
        submitButton.disableProperty().bind(fieldsViewController.getInputValidProperty().not());
        completionMessageLabel.visibleProperty().bind(completionMessageLabel.textProperty().isNotEmpty());
        progressIndicator.visibleProperty().bind(progressIndicatorVisibility);
        attachmentsViewController.setTextArea(fieldsViewController.getTextArea());
    }

    /**
     * Handler for Cancel button
     */
    @FXML
    public void cancel() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    @FXML
    public void submit() {
        progressIndicatorVisibility.setValue(true);
        completionMessageLabel.textProperty().setValue("");
        OlogLog ologLog = new OlogLog();
        ologLog.setTitle(fieldsViewController.getTitle());
        ologLog.setDescription(fieldsViewController.getDescription());
        ologLog.setLevel(fieldsViewController.getSelectedLevel());
        ologLog.setLogbooks(fieldsViewController.getSelectedLogbooks());
        ologLog.setTags(fieldsViewController.getSelectedTags());
        ologLog.setAttachments(attachmentsViewController.getAttachments());
        ologLog.setProperties(logPropertiesEditorController.getProperties());

        Future<LogEntry> future = executorService.submit(() -> {
            if (LogbookUIPreferences.save_credentials) {
                // Get the SecureStore. Store username and password.
                try {
                    SecureStore store = new SecureStore();
                    ScopedAuthenticationToken scopedAuthenticationToken =
                            new ScopedAuthenticationToken(LogService.AUTHENTICATION_SCOPE, fieldsViewController.getUsernameProperty(), fieldsViewController.getPasswordProperty());
                    store.setScopedAuthentication(scopedAuthenticationToken);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Secure Store file not found.", ex);
                }
            }

            LogClient logClient =
                    logFactory.getLogClient(new SimpleAuthenticationToken(fieldsViewController.getUsernameProperty(), fieldsViewController.getPasswordProperty()));

            if(replyTo == null){
                return logClient.set(ologLog);
            }
            else {
                return logClient.reply(ologLog, replyTo);
            }
        });
        try {
            LogEntry result = future.get();
            if (result != null) {
                if (completionHandler != null) {
                    completionHandler.handleResult(result);
                }
                attachmentsViewController.deleteTemporaryFiles();
                cancel();
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Unable to submit log entry", e);
            completionMessageLabel.textProperty().setValue(org.phoebus.logbook.Messages.SubmissionFailed);
        } catch (ExecutionException e) {
            logger.log(Level.WARNING, "Unable to submit log entry", e);
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                completionMessageLabel.textProperty().setValue(e.getCause().getMessage());
            } else if (e.getMessage() != null) {
                completionMessageLabel.textProperty().setValue(e.getMessage());
            } else {
                completionMessageLabel.textProperty().setValue(org.phoebus.logbook.Messages.SubmissionFailed);
            }
        } finally {
            progressIndicatorVisibility.setValue(false);
        }
    }
}
