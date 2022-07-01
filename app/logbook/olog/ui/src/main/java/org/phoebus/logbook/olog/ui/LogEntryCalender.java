package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.olog.ui.query.OlogQueryManager;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogEntryCalender implements AppInstance {

    final static Logger log = Logger.getLogger(LogEntryCalender.class.getName());
    private static final String LOG_CALENDER_QUERY = "log_calender_query";

    private final LogEntryCalenderApp app;
    private DockItem tab;

    private LogEntryCalenderViewController controller;

    LogEntryCalender(final LogEntryCalenderApp app) {
        this.app = app;
        try {
            OlogQueryManager ologQueryManager = OlogQueryManager.getInstance();
            SearchParameters searchParameters = new SearchParameters();
            searchParameters.setQuery(ologQueryManager.getQueries().get(0).getQuery());
            FXMLLoader loader = new FXMLLoader();
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            loader.setLocation(this.getClass().getResource("LogEntryCalenderView.fxml"));
            loader.setResources(resourceBundle);
            loader.setControllerFactory(clazz -> {
                try {
                    if(app.getClient() != null)
                    {
                        if(clazz.isAssignableFrom(LogEntryCalenderViewController.class)){
                            return clazz.getConstructor(LogClient.class, OlogQueryManager.class, SearchParameters.class)
                                    .newInstance(app.getClient(), ologQueryManager, searchParameters);
                        }
                        else if(clazz.isAssignableFrom(AdvancedSearchViewController.class)){
                            return clazz.getConstructor(LogClient.class, SearchParameters.class)
                                    .newInstance(app.getClient(), searchParameters);
                        }
                    }
                    else
                    {
                        // no logbook client available
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to open log viewer");
                        alert.setContentText("No logbook client found.");
                        alert.showAndWait();
                    }
                }
                catch (Exception e)
                {
                    ExceptionDetailsErrorDialog.openError("Error",
                            "Failed to open log calendar viewer: Logfactory could now create a logbook client", e);
                    Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.SEVERE, "Failed to construct controller for log calendar view", e);
                }
                return null;
            });
            loader.load();
            controller = loader.getController();
            if (this.app.getClient() != null) {
                controller.setClient(this.app.getClient());
            } else {
                log.log(Level.SEVERE, "Failed to acquire a valid logbook client");
            }
            tab = new DockItem(this, loader.getRoot());
            tab.setOnClosed(event -> {
                controller.shutdown();
            });
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e)
        {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot load UI", e);
        }
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return app;
    }

    @Override
    public void save(final Memento memento)
    {
        OlogQueryManager.getInstance().save();
    }
}
