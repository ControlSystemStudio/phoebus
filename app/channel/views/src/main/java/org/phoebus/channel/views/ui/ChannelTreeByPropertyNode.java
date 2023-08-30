package org.phoebus.channel.views.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.VType;
import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelUtil;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

import static org.phoebus.channel.views.ui.ChannelFinderController.logger;

/**
 * A representation of a node in the channelfinder tree
 * @author Kunal Shroff
 */
public class ChannelTreeByPropertyNode {

    // The model that contains the node, used to access all data
    // common to all nodes
    private ChannelTreeByPropertyModel model;

    // Channels represented by this node and down
    private List<Channel> nodeChannels;

    // 0 for root, 1 for children, 2 for grandchildren...
    private final int depth;
    // null for root,
    // first property value for children, second property value for grandchildren,....
    // channelName (pv value) for leaf
    private final String displayName;
    // Next property value for root and descendents, names of channels for first
    // to last node,
    // null for leaf
    private final List<String> childrenNames;
    // Parent of the node, or null if root
    private final ChannelTreeByPropertyNode parentNode;

    /**
     * Create the node in the channel tree ordered by a set of properties
     * @param model
     * @param parentNode
     * @param displayName
     * @param connect
     */
    public ChannelTreeByPropertyNode(ChannelTreeByPropertyModel model, ChannelTreeByPropertyNode parentNode, String displayName) {
        this.model = model;
        this.parentNode = parentNode;

        // Calculate depth
        if (parentNode == null) {
            depth = 0;
        } else {
            depth = parentNode.depth + 1;
        }

        this.displayName = displayName;

        // Construct the Channel list
        if (parentNode == null) {
            // Node is root, get all channels
            nodeChannels = model.allChannels;
        } else if (getPropertyName() == null) {
            // leaf node, channels that match the name
            nodeChannels = new ArrayList<Channel>();
            for (Channel channel : parentNode.nodeChannels) {
                if (this.displayName.equals(channel.getName())) {
                    nodeChannels.add(channel);
                    if(this.model.isConnect())
                    {
                        try {
                            final PV pv = PVPool.getPV(channel.getName());
                            pv.onValueEvent().throttleLatest(100, TimeUnit.MILLISECONDS).subscribe(value -> {
                                this.model.nodePVValues.put(pv.getName(), value);
                            });
                            this.model.nodePVs.add(pv);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "failed to create an active pv connection for pv: " + channel.getName(), e);
                        }
                    }
                }
            }
        } else {
            // Filter the channels that match the property name
            nodeChannels = new ArrayList<Channel>();
            for (Channel channel : parentNode.nodeChannels) {
                if (this.displayName.equals(channel.getProperty(getPropertyName()).getValue())) {
                    nodeChannels.add(channel);
                }
            }
        }

        if (depth < model.properties.size()) {
            // Children will be property values
            childrenNames = new ArrayList<String>(ChannelUtil.getPropValues(nodeChannels, model.properties.get(depth)));
            Collections.sort(childrenNames);
        } else if (depth == model.properties.size()) {
            // Children will be channels
            if (model.isShowChannelNames()) {
                childrenNames = new ArrayList<String>(ChannelUtil.getChannelNames(nodeChannels));
                Collections.sort(childrenNames);
            } else {
                childrenNames = null;
            }
        } else {
            childrenNames = null;
        }
    }

    /**
     * The property name at this level
     *
     * @return property name or null if leaf node
     */
    public String getPropertyName()
    {
        // Root node does not have any property associated with it
        if (depth == 0)
            return null;

        int index = depth - 1;
        // We are at the channel level
        if (index >= model.properties.size())
            return null;

        return model.properties.get(index);
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getDisplayValue()
    {
        if (model.nodePVValues.containsKey(displayName))
        {
            return formatVType(model.nodePVValues.get(displayName));
        }
        return null;
    }

    private String formatVType(VType value )
    {
        Alarm alarm = Alarm.alarmOf(value);
        StringBuffer sb = new StringBuffer();
        sb.append(FormatOptionHandler.format(value, FormatOption.DEFAULT, -1, true));
        if (!alarm.getSeverity().equals(AlarmSeverity.NONE)) 
        {
            sb.append(alarm.getSeverity().toString());
        }
        if (!alarm.getStatus().equals(AlarmStatus.NONE))
        {
            sb.append(" - ");
            sb.append(alarm.getStatus().toString());
        }
        return sb.toString();
    }

    public List<String> getChildrenNames() {
        return childrenNames;
    }

    public ChannelTreeByPropertyNode getChild(int index) {
        return new ChannelTreeByPropertyNode(model, this, childrenNames.get(index));
    }

    public List<Channel> getNodeChannels() {
        return Collections.unmodifiableList(nodeChannels);
    }

    public ChannelTreeByPropertyNode getParentNode() {
        return parentNode;
    }

    /**
     * True if the node represents a sub-query and not a single channel.
     * 
     * @return
     */
    public boolean isSubQuery() {
        int index = depth - 1;
        // We are at the channel level
        if (index >= model.properties.size())
            return false;

        return true;
    }

    public String getSubQuery() {
        // If it's not a sub-query, return the channel name (i.e. the display
        // name)
        if (!isSubQuery()) {
            return getDisplayName();
        }

        if (parentNode == null) {
            return model.query;
        }

        return parentNode.getSubQuery() + " " + getPropertyName() + "=" + getDisplayName();
    }

    private void includePropertyAndValue(Map<String, String> map) {
        if (getPropertyName() != null) {
            map.put(getPropertyName(), getDisplayName());
        }
        if (parentNode != null) {
            parentNode.includePropertyAndValue(map);
        }
    }

    public Map<String, String> getPropertiesAndValues() {
        Map<String, String> map = new HashMap<String, String>();
        includePropertyAndValue(map);
        return map;
    }

}
