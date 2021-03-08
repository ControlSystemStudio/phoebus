package org.phoebus.logbook.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.ui.write.LogEntryEditorStage;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;

public class LogEntryCalender implements AppInstance {

    final static Logger log = Logger.getLogger(LogEntryCalender.class.getName());
    private static final String LOG_CALENDER_QUERY = "log_calender_query";

    private final LogEntryCalenderApp app;
    private DockItem tab;

    private LogEntryCalenderViewController controller;


    LogEntryCalender(final LogEntryCalenderApp app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("LogEntryCalenderView.fxml"));
            loader.setControllerFactory(clazz -> {
                try {
                    if(clazz.isAssignableFrom(LogEntryCalenderViewController.class)){
                        return clazz.getConstructor(LogClient.class)
                                .newInstance(app.getClient());
                    }
                    else if(clazz.isAssignableFrom(AdvancedSearchViewController.class)){
                        return clazz.getConstructor(LogClient.class)
                                .newInstance(app.getClient());
                    }
                } catch (Exception e) {
                    Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.SEVERE, "Failed to construct controller for log calendar view", e);
                }
                return null;
            });
            loader.load();
            controller = loader.getController();
            controller.setQuery(LogbookUiPreferences.default_logbook_query);
            if (this.app.getClient() != null) {
                controller.setClient(this.app.getClient());
            } else {
                log.log(Level.SEVERE, "Failed to acquire a valid logbook client");
            }

            tab = new DockItem(this, loader.getRoot());
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e)
        {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot load UI", e);
        }
        tab.setOnClosed(event -> {
            // dispose();
        });
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return app;
    }

    @Override
    public void restore(final Memento memento)
    {
        if (memento.getString(LOG_CALENDER_QUERY).isPresent()) {
            controller.setQuery(memento.getString(LOG_CALENDER_QUERY).get());
        } else {
            controller.setQuery(LogbookUiPreferences.default_logbook_query);
        }
    }

    @Override
    public void save(final Memento memento)
    {
        if(!controller.getQuery().isBlank()) {
            memento.setString(LOG_CALENDER_QUERY, controller.getQuery().trim());
        }
    }
}
