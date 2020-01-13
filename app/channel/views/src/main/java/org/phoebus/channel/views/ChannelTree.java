package org.phoebus.channel.views;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.channel.views.ui.ChannelTreeController;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;

/**
 * An instance of the ChannelTree application to display cf queries.
 * @author Kunal Shroff
 *
 */
public class ChannelTree implements AppInstance {
    private final ChannelTreeApp app;
    private ChannelTreeController controller;
    private DockItem tab;

    ChannelTree(final ChannelTreeApp app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("ui/ChannelTree.fxml"));
            tab = new DockItem(this, loader.load());
            controller = loader.getController();
            controller.setClient(app.getClient());
            tab.addClosedNotification(() -> {
                controller.dispose();
            });
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
        // TODO URI parsing might be imporved.
        String parsedQuery = Arrays.asList(query.split("&")).stream().filter(s->{
            return s.startsWith("query");
        }).map(s->{return s.split("=")[1];}).collect(Collectors.joining(" "));
        controller.setQuery(parsedQuery);
    }
}
