package org.phoebus.logbook.ui;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.Node;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.ui.write.AttachmentsViewController;
import org.phoebus.logbook.ui.write.FieldsViewController;
import org.phoebus.logbook.ui.write.LogEntryCompletionHandler;
import org.phoebus.logbook.ui.write.LogEntryEditorController;
import org.phoebus.logbook.ui.write.LogEntryEditorStage;
import org.phoebus.logbook.ui.write.LogEntryModel;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;

public class LogEntryTable implements AppInstance {
    final static Logger log = Logger.getLogger(LogEntryTable.class.getName());
    
    private final LogEntryTableApp app;
    private LogEntryTableViewController controller;

    LogEntryTable(final LogEntryTableApp app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("LogEntryTableView.fxml"));
            loader.setControllerFactory(clazz -> {
                try {
                    if(clazz.isAssignableFrom(LogEntryTableViewController.class)){
                        return clazz.getConstructor(LogClient.class)
                                .newInstance(app.getClient());
                    }
                    else if(clazz.isAssignableFrom(AdvancedSearchViewController.class)){
                        return clazz.getConstructor(LogClient.class)
                                .newInstance(app.getClient());
                    }
                } catch (Exception e) {
                    Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.SEVERE, "Failed to construct controller for log table view", e);
                }
                return null;
            });
            loader.load();
            controller = loader.getController();
            DockItem tab = new DockItem(this, loader.getRoot());
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
