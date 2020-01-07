package org.phoebus.channel.views.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.epics.vtype.VType;
import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelUtil;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

/**
 * Model for the Channel Tree built using properties
 * @author Kunal Shroff
 */
class ChannelTreeByPropertyModel {

    private ChannelTreeByPropertyNode root;
    final String query;
    private final boolean showChannelNames;
    // Flag instructing the model to connect to the leaf pv's
    private final boolean connect;

    List<Channel> allChannels;
    List<String> properties;
    
    // PVs represented by this three
    List<PV> nodePVs;
    Map<String, VType> nodePVValues;

    public ChannelTreeByPropertyModel(String query, Collection<Channel> allChannels, List<String> properties, boolean showChannelNames, boolean connect) {
        if (allChannels == null) {
            allChannels = Collections.emptyList();
        }

        // Filter the channels that would not show up as leaf because they don't
        // have a value for all properties
        this.allChannels = new ArrayList<Channel>(ChannelUtil.filterbyProperties(allChannels, properties));
        this.properties = properties;

        this.nodePVs = new ArrayList<PV>();
        this.nodePVValues = new HashMap<String, VType>();

        this.query = query;
        this.showChannelNames = showChannelNames;
        this.connect = connect;
        this.root = new ChannelTreeByPropertyNode(this, null, query);
    }

    public ChannelTreeByPropertyNode getRoot() {
        return root;
    }

    public boolean isShowChannelNames() {
        return showChannelNames;
    }

    public boolean isConnect() {
        return connect;
    }

    public void dispose() {
        nodePVs.forEach(pv -> PVPool.releasePV(pv));
        nodePVs.clear();
    }
}
