package org.phoebus.channel.views.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.phoebus.channelfinder.Channel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * Controller for the file browser app
 * 
 * @author Kunal Shroff
 *
 */
public class ChannelTreeController extends ChannelFinderController {

    @FXML
    TextField query;
    @FXML
    Button search;
    @FXML
    TreeView<ChannelTreeByPropertyNode> treeView;

    private List<String> properties = Arrays.asList();
    private ChannelTreeByPropertyModel model;

    @FXML
    public void initialize() {
        treeView.setCellFactory(f -> new ChannelTreeCell());
    }

    public void setQuery(String string) {
        query.setText(string);
        search();
    }

    @FXML
    public void search() {
        super.search(query.getText());
    }

    @Override
    public void setChannels(Collection<Channel> channels) {
        model = new ChannelTreeByPropertyModel(query.getText(), channels, properties, this, true);
        ChannelTreeItem root = new ChannelTreeItem(model.getRoot());
        treeView.setRoot(root);
    }

    private final class ChannelTreeItem extends TreeItem<ChannelTreeByPropertyNode> {

        private boolean isFirstTimeLeaf = true;
        private boolean isFirstTimeChildren = true;
        private boolean isLeaf;

        public ChannelTreeItem(ChannelTreeByPropertyNode node) {
            super(node);
        }

        @Override
        public ObservableList<TreeItem<ChannelTreeByPropertyNode>> getChildren() {
            if (isFirstTimeChildren) {
                isFirstTimeChildren = false;
                super.getChildren().setAll(buildChildren(this));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            if (isFirstTimeLeaf) {
                isFirstTimeLeaf = false;
                if (getValue().getParentNode() == null) {
                    isLeaf = false;
                } else {
                    isLeaf = getValue().getPropertyName() == null;
                }
            }
            return isLeaf;
        }

        private ObservableList<TreeItem<ChannelTreeByPropertyNode>> buildChildren(
                TreeItem<ChannelTreeByPropertyNode> treeItem) {
            ChannelTreeByPropertyNode item = treeItem.getValue();
            ObservableList<TreeItem<ChannelTreeByPropertyNode>> children = FXCollections.observableArrayList();
            for (String child : item.getChildrenNames()) {
                children.add(new ChannelTreeItem(new ChannelTreeByPropertyNode(model, item, child)));
            }
            return children;
        }
    }

    private final class ChannelTreeCell extends TreeCell<ChannelTreeByPropertyNode> {

        @Override
        protected void updateItem(ChannelTreeByPropertyNode node, boolean empty) {
            super.updateItem(node, empty);
            if (node != null) {
                setText(node.getDisplayName());
            } else {
                setText(null);
                setTooltip(null);
                setContextMenu(null);
                setGraphic(null);
            }
        }
    }

}
