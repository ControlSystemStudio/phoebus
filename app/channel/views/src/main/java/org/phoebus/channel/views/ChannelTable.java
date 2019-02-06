package org.phoebus.channel.views;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.channel.views.ui.ChannelTableController;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;

/**
 * An instance of the ChannelTable application to display cf queries.
 * @author Kunal Shroff
 *
 */
public class ChannelTable implements AppInstance {
    private final ChannelTableApp app;
    private ChannelTableController controller;
    private DockItem tab;

    ChannelTable(final ChannelTableApp app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("ui/ChannelTable.fxml"));
            tab = new DockItem(this, loader.load());
            controller = loader.getController();
            controller.setClient(app.getClient());
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
