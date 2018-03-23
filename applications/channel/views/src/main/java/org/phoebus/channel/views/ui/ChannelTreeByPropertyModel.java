package org.phoebus.channel.views.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelUtil;

class ChannelTreeByPropertyModel {

    private ChannelTreeController widget;

    List<Channel> allChannels;
    List<String> properties;
    private ChannelTreeByPropertyNode root;
    final String query;
    private final boolean showChannelNames;

    public ChannelTreeByPropertyModel(String query, Collection<Channel> allChannels, List<String> properties,
            ChannelTreeController channelTreeController, boolean showChannelNames) {
        if (allChannels == null) {
            allChannels = Collections.emptyList();
        }

        // Filter the channels that would not show up as leaf because they don't
        // have a value for all properties
        this.allChannels = new ArrayList<Channel>(ChannelUtil.filterbyProperties(allChannels, properties));
        this.properties = properties;
        this.query = query;
        this.widget = channelTreeController;
        this.showChannelNames = showChannelNames;
        this.root = new ChannelTreeByPropertyNode(this, null, query);
    }

    public ChannelTreeByPropertyNode getRoot() {
        return root;
    }

    public ChannelTreeController getWidget() {
        return widget;
    }

    public boolean isShowChannelNames() {
        return showChannelNames;
    }
}
