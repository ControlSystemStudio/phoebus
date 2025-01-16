package org.phoebus.logbook.olog.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.olog.ui.query.OlogQueryManager;
import org.phoebus.logbook.olog.ui.spi.Decoration;
import org.phoebus.logbook.olog.ui.write.AttachmentsEditorController;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogEntryTable implements AppInstance {
    static Logger log = Logger.getLogger(LogEntryTable.class.getName());

    private static final String HIDE_DETAILS = "hide_details";

    private final LogEntryTableApp app;
    private LogEntryTableViewController controller;

    public GoBackAndGoForwardActions goBackAndGoForwardActions;
    public LogEntryTable(final LogEntryTableApp app) {
        this.app = app;
        goBackAndGoForwardActions = new GoBackAndGoForwardActions();

        List<Decoration> decorations = new LinkedList<>();
        {
            ServiceLoader<Decoration> decorationClasses = ServiceLoader.load(Decoration.class);
            for (Decoration decoration : decorationClasses) {
                decorations.add(decoration);
            }
        }

        try {
            OlogQueryManager ologQueryManager = OlogQueryManager.getInstance();
            SearchParameters searchParameters = new SearchParameters();
            searchParameters.setQuery(ologQueryManager.getQueries().get(0).getQuery());
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(resourceBundle);
            loader.setLocation(this.getClass().getResource("LogEntryTableView.fxml"));

            LogClient logClient = app.getClient();

            loader.setControllerFactory(clazz -> {
                try {
                    if (logClient != null) {
                        if (clazz.isAssignableFrom(LogEntryTableViewController.class)) {
                            LogEntryTableViewController logEntryTableViewController = (LogEntryTableViewController) clazz.getConstructor(LogClient.class, OlogQueryManager.class, SearchParameters.class)
                                    .newInstance(logClient, ologQueryManager, searchParameters);
                            logEntryTableViewController.setGoBackAndGoForwardActions(goBackAndGoForwardActions);
                            logEntryTableViewController.setDecorations(decorations);
                            return logEntryTableViewController;
                        } else if (clazz.isAssignableFrom(AdvancedSearchViewController.class)) {
                            return clazz.getConstructor(LogClient.class, SearchParameters.class).newInstance(logClient, searchParameters);
                        } else if (clazz.isAssignableFrom(SingleLogEntryDisplayController.class)) {
                            SingleLogEntryDisplayController singleLogEntryDisplayController = (SingleLogEntryDisplayController) clazz.getConstructor(LogClient.class).newInstance(logClient);
                            singleLogEntryDisplayController.setSelectLogEntryInUI(id -> goBackAndGoForwardActions.loadLogEntryWithID(id));
                            return singleLogEntryDisplayController;
                        } else if (clazz.isAssignableFrom(LogEntryDisplayController.class)) {
                            return clazz.getConstructor().newInstance();
                        } else if (clazz.isAssignableFrom(LogPropertiesController.class)) {
                            return clazz.getConstructor().newInstance();
                        } else if (clazz.isAssignableFrom(AttachmentsViewController.class)) {
                            return clazz.getConstructor().newInstance();
                        } else if (clazz.isAssignableFrom(AttachmentsEditorController.class)) {
                            return clazz.getConstructor().newInstance();
                        } else if (clazz.isAssignableFrom(MergedLogEntryDisplayController.class)) {
                            return clazz.getConstructor(LogClient.class).newInstance(logClient);
                        }
                    } else {
                        // no logbook client available
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to open log viewer");
                        alert.setContentText("No logbook client found.");
                        alert.showAndWait();
                    }
                } catch (Exception e) {
                    ExceptionDetailsErrorDialog.openError("Error",
                            "Failed to open log table viewer: Logfactory could now create a logbook client", e);
                    log.log(Level.SEVERE, "Failed to construct controller for log table view", e);
                }
                return null;
            });
            loader.load();
            controller = loader.getController();
            DockItem tab = new DockItem(this, loader.getRoot());
            tab.addClosedNotification(()-> controller.shutdown());
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e) {
            log.log(Level.WARNING, "Cannot load UI", e);
        }
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return app;
    }

    public void setResource(URI resource) {
        String query = resource.getQuery();
        controller.setQuery(query);
    }

    @Override
    public void restore(Memento memento) {
        Optional<Boolean> hideDetails = memento.getBoolean(HIDE_DETAILS);
        controller.setShowDetails(hideDetails.isEmpty() || hideDetails.get());
    }

    @Override
    public void save(final Memento memento) {
        OlogQueryManager.getInstance().save();
        memento.setBoolean(HIDE_DETAILS, controller.getShowDetails());
    }

    /**
     * Handler for a {@link LogEntry} change, new or updated.
     * A search is triggered to make sure the result list reflects the change, and
     * the detail view controller is called to refresh, if applicable.
     * @param logEntry A new or updated {@link LogEntry}
     */
    public void logEntryChanged(LogEntry logEntry){
        controller.logEntryChanged(logEntry);
    }

    protected class GoBackAndGoForwardActions {

        private GoBackAndGoForwardActions() {
            goBackActions = FXCollections.observableArrayList();
            goForwardActions = FXCollections.observableArrayList();
        }

        protected ObservableList<Runnable> goBackActions;
        protected ObservableList<Runnable> goForwardActions;

        private boolean isRecordingHistoryDisabled = false; // Used to not add go-back actions when clicking "back".

        protected boolean getIsRecordingHistoryDisabled() {
            return isRecordingHistoryDisabled;
        }
        public void setIsRecordingHistoryDisabled(boolean isRecordingHistoryDisabled) {
            this.isRecordingHistoryDisabled = isRecordingHistoryDisabled;
        }

        private void gotoLogEntry(LogEntry logEntry) {
            isRecordingHistoryDisabled = true;
            boolean selected = controller.selectLogEntry(logEntry);
            if (!selected) {
                // The log entry was not available in the TreeView. Set the log entry without selecting it in the treeview:
                controller.setLogEntry(logEntry);
            }
            isRecordingHistoryDisabled = false;
        }

        protected void addGoBackAction() {
            LogEntry currentLogEntry = controller.getLogEntry();

            if (currentLogEntry != null) {
                goBackActions.add(0, () -> gotoLogEntry(currentLogEntry));
            }
        }

        private void addGoForwardAction() {
            LogEntry currentLogEntry = controller.getLogEntry();

            if (currentLogEntry != null) {
                goForwardActions.add(0, () -> gotoLogEntry(currentLogEntry));
            }
        }

        protected boolean loadLogEntryWithID(Long id) {
            try {
                LogEntry logEntry = controller.client.getLog(id);
                goForwardActions.clear();
                addGoBackAction();
                gotoLogEntry(logEntry);
                return true;
            }
            catch (RuntimeException runtimeException) {
                return false;
            }
        }

        protected void goBack() {
            if (goBackActions.size() > 0) {
                addGoForwardAction();
                Runnable goBackAction = goBackActions.get(0);
                goBackActions.remove(0);
                goBackAction.run();
            }
        }

        protected void goForward() {
            if (goForwardActions.size() > 0) {
                addGoBackAction();
                Runnable goForwardAction = goForwardActions.get(0);
                goForwardActions.remove(0);
                goForwardAction.run();
            }
        }
    }
}
