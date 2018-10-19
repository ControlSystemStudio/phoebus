package org.phoebus.logbook.ui;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;

public class LogEntryTable implements AppInstance {
    final static Logger log = Logger.getLogger(LogEntryTable.class.getName());
    
    private final LogEntryTableApp app;
    private LogEntryTableViewController controller;
    private DockItem tab;

    LogEntryTable(final LogEntryTableApp app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("LogEntryTableView.fxml"));
            loader.load();
            controller = loader.getController();
            if (this.app.getClient() != null) {
                controller.setClient(this.app.getClient());
            } else {
                log.log(Level.SEVERE, "Failed to acquire a valid logbook client");
            }

            tab = new DockItem(this, loader.getRoot());
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot load UI", e);
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
}
