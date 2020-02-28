package org.phoebus.channel.views.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.Property;
import org.phoebus.channelfinder.Tag;

import java.util.Collection;
import java.util.List;

/**
 * Controller for ChannelInfoTree.fxml
 */
public class ChannelInfoTreeController {

    // Model
    List<Channel> channels;
    TreeItem<Attribute> root;

    @FXML
    TreeTableView<Attribute> treeTable;
    @FXML
    TreeTableColumn<Attribute, String> name;
    @FXML
    TreeTableColumn<Attribute, String> value;

    public List<Channel> getChannels()
    {
        return channels;
    }

    public synchronized void setChannels(List<Channel> channels)
    {
        this.channels = channels;
        refresh();
    }

    public synchronized void addChannels(Collection<Channel> channels) {
        getChannels().addAll(channels);
        refresh();
    }

    @FXML
    private void initialize()
    {
        name.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
        value.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
        treeTable.setShowRoot(false);
    }

    @FXML
    private void refresh()
    {
        updateModel();
        treeTable.setRoot(root);
    }

    private void updateModel() {
        root = new TreeItem<>();

        getChannels().forEach(channel -> {
            TreeItem<Attribute> channelNode = new TreeItem<>(new Attribute("channel:", channel.getName()));
            channelNode.getChildren().add(new TreeItem<>(new Attribute("owner:", channel.getOwner())));
            if(!channel.getProperties().isEmpty())
            {
                TreeItem<Attribute> properties = new TreeItem<>(new Attribute("properties:", " "));
                for (Property property : channel.getProperties())
                {
                    properties.getChildren().add(new TreeItem<>(new Attribute(property.getName(), property.getValue())));
                }
                channelNode.getChildren().add(properties);
            }
            if(!channel.getTags().isEmpty())
            {
                TreeItem<Attribute> tags = new TreeItem<>(new Attribute("tags:", " "));
                for (Tag tag : channel.getTags())
                {
                    tags.getChildren().add(new TreeItem<>(new Attribute(" ", tag.getName())));
                }
                channelNode.getChildren().add(tags);
            }
            root.getChildren().add(channelNode);
        });
    }


    /**
     * A class to simplify the representation of a {@link Channel} in a tree view
     */
    public static class Attribute
    {
        private final String name;
        private final String value;

        private Attribute(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        public String getName()
        {
            return name;
        }

        public String getValue()
        {
            return value;
        }
    }
}
