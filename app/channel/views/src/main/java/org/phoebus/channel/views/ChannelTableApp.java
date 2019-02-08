package org.phoebus.channel.views;

import java.net.URI;
import java.util.logging.Logger;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.ChannelFinderService;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;

public class ChannelTableApp implements AppResourceDescriptor {

    public static final Logger logger = Logger.getLogger(ChannelTableApp.class.getName());
    static final Image icon = ImageCache.getImage(ChannelTableApp.class, "/icons/channel-viewer-16.png");
    public static final String NAME = "channel_table";
    public static final String DISPLAYNAME = "Channel Table";

    private static final String SUPPORTED_SCHEMA = "cf";

    private ChannelFinderClient client;

    @Override
    public void start() {
        client = ChannelFinderService.getInstance().getClient();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAYNAME;
    }

    @Override
    public AppInstance create() {
        return new ChannelTable(this);
    }

    @Override
    public boolean canOpenResource(String resource) {
        return URI.create(resource).getScheme().equals(SUPPORTED_SCHEMA);
    }

    /**
     * Support the launching of channeltable using resource cf://?<search_string>
     * e.g.
     * -resource cf://?query=SR*
     */
    @Override
    public AppInstance create(URI resource) {
        ChannelTable channelTable = new ChannelTable(this);
        channelTable.setResource(resource);
        return channelTable;
    }
    
    /**
     * Get the default {@link ChannelFinderClient} initialized during the creation of this application descriptor
     * @return {@link ChannelFinderClient} default client
     */
    public ChannelFinderClient getClient() {
        return client;
    }
}
