package org.phoebus.channel.views.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.management.Query;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelUtil;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.WindowEvent;

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
    Button configure;
    @FXML
    TreeView<ChannelTreeByPropertyNode> treeView;

    private List<String> orderedProperties = Collections.emptyList();
    private Collection<Channel> channels = Collections.emptyList();;
    private ChannelTreeByPropertyModel model;

    @FXML
    public void initialize() {

        treeView.setCellFactory(f -> new ChannelTreeCell());
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                SelectionService.getInstance().setSelection(treeView, treeView.getSelectionModel().getSelectedItems());
            }
        });
    }

    public void setQuery(String string) {
        query.setText(string);
        search();
    }

    public void setOrderedProperties(List<String> orderedProperties) {
        this.orderedProperties = orderedProperties;
        reconstructTree();
    }

    @Override
    public void setChannels(Collection<Channel> channels) {
        this.channels = channels;
        reconstructTree();
    }

    private void reconstructTree() {
        model = new ChannelTreeByPropertyModel(query.getText(), channels, orderedProperties, true);
        ChannelTreeItem root = new ChannelTreeItem(model.getRoot());
        treeView.setRoot(root);
    }

    /**
     * Reorder the properties used to create the table
     */
    @FXML
    public void configure() {
        if (model != null) {
            List<String> allProperties = ChannelUtil.getPropertyNames(model.getRoot().getNodeChannels()).stream().sorted().collect(Collectors.toList());
            ListMultiOrderedPickerDialog dialog = new ListMultiOrderedPickerDialog(allProperties, orderedProperties);
            Optional<List<String>> result = dialog.showAndWait();
            result.ifPresent(r -> {
                setOrderedProperties(r);
            });
        }
    }

    /**
     * Search channelfinder for the channels satisfying the query set
     */
    @FXML
    public void search() {
        super.search(query.getText());
    }

    @FXML
    public void createContextMenu() {

        final ContextMenu contextMenu = new ContextMenu();

        List<ContextMenuEntry> contextEntries = ContextMenuService.getInstance().listSupportedContextMenuEntries();
        contextEntries.forEach(entry -> {
            MenuItem item = new MenuItem(entry.getName());
            item.setOnAction(e -> {
                try {
                    //final Stage stage = (Stage) listView.getScene().getWindow();
                    entry.callWithSelection(SelectionService.getInstance().getSelection());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
            contextMenu.getItems().add(item);
        });

        treeView.setContextMenu(contextMenu);
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
