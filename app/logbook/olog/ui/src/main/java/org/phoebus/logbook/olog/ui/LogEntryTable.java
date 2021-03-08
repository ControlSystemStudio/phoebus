package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.LogClient;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogEntryTable implements AppInstance {
    final static Logger log = Logger.getLogger(LogEntryTable.class.getName());
    private static final String LOG_TABLE_QUERY = "log_table_query";
    
    private final LogEntryTableApp app;
    private LogEntryTableViewController controller;

    public LogEntryTable(final LogEntryTableApp app)
    {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("LogEntryTableView.fxml"));

            loader.setControllerFactory(clazz -> {
                try {
                    if(app.getClient() != null)
                    {
                        if(clazz.isAssignableFrom(LogEntryTableViewController.class))
                        {
                            return clazz.getConstructor(LogClient.class).newInstance(app.getClient());
                        }
                        else if(clazz.isAssignableFrom(AdvancedSearchViewController.class))
                        {
                            return clazz.getConstructor(LogClient.class).newInstance(app.getClient());
                        }
                        else if(clazz.isAssignableFrom(LogEntryDisplayController.class))
                        {
                            return clazz.getConstructor(LogClient.class).newInstance(app.getClient());
                        }
                        else if(clazz.isAssignableFrom(LogPropertiesController.class))
                        {
                            return clazz.getConstructor().newInstance();
                        }
                        else if(clazz.isAssignableFrom(LogAttachmentsController.class))
                        {
                            return clazz.getConstructor().newInstance();
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
                } catch (Exception e) {
                    ExceptionDetailsErrorDialog.openError("Error",
                            "Failed to open log table viewer: Logfactory could now create a logbook client", e);
                    log.log(Level.SEVERE, "Failed to construct controller for log table view", e);
                }
                return null;
            });
            loader.load();
            controller = loader.getController();
            controller.setQuery(LogbookUIPreferences.default_logbook_query);
            DockItem tab = new DockItem(this, loader.getRoot());
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e)
        {
            log.log(Level.WARNING, "Cannot load UI", e);
        }
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    public void setResource(URI resource)
    {
        String query = resource.getQuery();
        controller.setQuery(query);
    }

    @Override
    public void restore(final Memento memento)
    {
        if (memento.getString(LOG_TABLE_QUERY).isPresent()) {
            controller.setQuery(memento.getString(LOG_TABLE_QUERY).get());
        }
        else
        {
            controller.setQuery(LogbookUIPreferences.default_logbook_query);
        }
    }

    @Override
    public void save(final Memento memento)
    {
        if(!controller.getQuery().isBlank())
        {
            memento.setString(LOG_TABLE_QUERY, controller.getQuery().trim());
        }
    }
}
