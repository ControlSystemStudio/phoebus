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

public class LogEntryList implements AppInstance {
    private final LogEntryListApp app;
    private LogEntryListController controller;
    private DockItem tab;

    LogEntryList(final LogEntryListApp app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("LogEntryList.fxml"));
            tab = new DockItem(this, loader.load());
            controller = loader.getController();
            controller.setClient(this.app.getClient());
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
        // TODO URI parsing might be improved.
        String parsedQuery = Arrays.asList(query.split("&")).stream().filter(s->{
            return s.startsWith("query");
        }).map(s->{return s.split("=")[1];}).collect(Collectors.joining(" "));
        controller.setQuery(parsedQuery);
    }
}
