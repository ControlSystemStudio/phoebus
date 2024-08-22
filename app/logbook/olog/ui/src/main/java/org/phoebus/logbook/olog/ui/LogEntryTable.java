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
import org.phoebus.logbook.olog.ui.write.AttachmentsEditorController;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogEntryTable implements AppInstance {
    static Logger log = Logger.getLogger(LogEntryTable.class.getName());

    private static final String HIDE_DETAILS = "hide_details";

    private final LogEntryTableApp app;
    private LogEntryTableViewController controller;

    public UndoAndRedoActions undoAndRedoActions;

    public LogEntryTable(final LogEntryTableApp app) {
        this.app = app;
        undoAndRedoActions = new UndoAndRedoActions();
        try {
            OlogQueryManager ologQueryManager = OlogQueryManager.getInstance();
            SearchParameters searchParameters = new SearchParameters();
            searchParameters.setQuery(ologQueryManager.getQueries().get(0).getQuery());
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(resourceBundle);
            loader.setLocation(this.getClass().getResource("LogEntryTableView.fxml"));

            loader.setControllerFactory(clazz -> {
                try {
                    if (app.getClient() != null) {
                        if (clazz.isAssignableFrom(LogEntryTableViewController.class)) {
                            return clazz.getConstructor(LogClient.class, OlogQueryManager.class, SearchParameters.class, UndoAndRedoActions.class)
                                    .newInstance(app.getClient(), ologQueryManager, searchParameters, undoAndRedoActions);
                        } else if (clazz.isAssignableFrom(AdvancedSearchViewController.class)) {
                            return clazz.getConstructor(LogClient.class, SearchParameters.class)
                                    .newInstance(app.getClient(), searchParameters);
                        } else if (clazz.isAssignableFrom(SingleLogEntryDisplayController.class)) {
                            SingleLogEntryDisplayController singleLogEntryDisplayController = (SingleLogEntryDisplayController) clazz.getConstructor(LogClient.class).newInstance(app.getClient());
                            singleLogEntryDisplayController.setSelectLogEntryInUI(id -> undoAndRedoActions.loadLogEntryWithID(id));
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
                            return clazz.getConstructor(LogClient.class).newInstance(app.getClient());
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

    public class UndoAndRedoActions {

        public ObservableList<Runnable> undoActions;
        public ObservableList<Runnable> redoActions;

        public UndoAndRedoActions() {
            undoActions = FXCollections.observableArrayList();
            redoActions = FXCollections.observableArrayList();
        }

        public void loadLogEntryWithID(Long id) {
            {
                LogEntry currentLogEntry = controller.getLogEntry();

                Runnable undoAction = () -> {
                    boolean selected = controller.selectLogEntry(currentLogEntry);
                    if (!selected) {
                        // The log entry was not available in the TreeView. Set the log entry without selecting it in the treeview:
                        controller.setLogEntry(currentLogEntry);
                    }
                };

                undoActions.add(0, undoAction);
            }

            redoActions = FXCollections.observableArrayList();

            {
                LogEntry logEntry = controller.client.getLog(id);
                boolean selected = controller.selectLogEntry(logEntry);
                if (!selected) {
                    // The log entry was not available in the TreeView. Set the log entry without selecting it in the treeview:
                    controller.setLogEntry(logEntry);
                }
            }
        }

        public void performUndo() {
            if (undoActions.size() > 0) {
                Runnable undoAction = undoActions.get(0);
                undoActions.remove(0);

                {
                    LogEntry currentLogEntry = controller.getLogEntry();
                    Runnable redoAction = () -> {
                        boolean selected = controller.selectLogEntry(currentLogEntry);
                        if (!selected) {
                            // The log entry was not available in the TreeView. Set the log entry without selecting it in the treeview:
                            controller.setLogEntry(currentLogEntry);
                        }
                    };
                    redoActions.add(0, redoAction);
                }

                undoAction.run();
            }
        }

        public void performRedo() {
            if (redoActions.size() > 0) {
                Runnable redoAction = redoActions.get(0);
                redoActions.remove(0);

                {
                    LogEntry currentLogEntry = controller.getLogEntry();

                    Runnable undoAction = () -> {
                        boolean selected = controller.selectLogEntry(currentLogEntry);
                        if (!selected) {
                            // The log entry was not available in the TreeView. Set the log entry without selecting it in the treeview:
                            controller.setLogEntry(currentLogEntry);
                        }
                    };

                    undoActions.add(0, undoAction);
                }

                redoAction.run();
            }
        }
    }
}
