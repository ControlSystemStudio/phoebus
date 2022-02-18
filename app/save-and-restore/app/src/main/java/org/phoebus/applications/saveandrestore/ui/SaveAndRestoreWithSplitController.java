/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.stream.Collectors;

public class SaveAndRestoreWithSplitController extends SaveAndRestoreController {

    @FXML
    private ListView<Node> listView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);

        treeView.setCellFactory(p -> new BrowserTreeCell(folderContextMenu,
                saveSetContextMenu, null, rootFolderContextMenu,
                this));

        browserSelectionModel.selectedItemProperty().addListener((observableValue, nodeTreeItem, selectedTreeItem) -> {
            if (selectedTreeItem == null) {
                return;
            }

            Node selectedNode = selectedTreeItem.getValue();
            if (selectedNode.getNodeType() != NodeType.CONFIGURATION) {
                return;
            }

            listView.getItems().clear();

            List<Node> snapshots = saveAndRestoreService.getChildNodes(selectedNode);
            if (!snapshots.isEmpty()) {
                listView.getItems().addAll(snapshots);
                listView.getItems().sort(new NodeComparator());
            }
        });

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(p -> new BrowserListCell(snapshotContextMenu));

        listView.setOnMouseClicked(action -> {
            Node node = listView.getSelectionModel().getSelectedItem();

            if (node == null) {
                return;
            }

            if (node.getNodeType().equals(NodeType.SNAPSHOT)) {
                toggleGoldenMenuItemText.set(Boolean.parseBoolean(node.getProperty("golden")) ? Messages.contextMenuRemoveGoldenTag : Messages.contextMenuTagAsGolden);
                toggleGoldenImageViewProperty.set(Boolean.parseBoolean(node.getProperty("golden")) ? snapshotImageView : snapshotGoldenImageView);
            }

            if (action.getClickCount() == 2) {
                nodeDoubleClicked(new TreeItem<>(node));
            }
        });

        loadTreeData();
    }

    @Override
    protected void deleteSnapshots() {
        ObservableList<Node> selectedItems = listView.getSelectionModel().getSelectedItems();
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(Messages.promptDeleteSelectedTitle);
        alert.setHeaderText(Messages.promptDeleteSelectedHeader);
        alert.setContentText(Messages.promptDeleteSelectedContent);
        alert.getDialogPane().addEventFilter(KeyEvent.ANY, event -> {
            if (event.getCode().equals(KeyCode.ENTER) || event.getCode().equals(KeyCode.SPACE)) {
                event.consume();
            }
        });
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            selectedItems.forEach(this::deleteListItem);
        }
    }

    private void deleteListItem(Node listItem) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                saveAndRestoreService.deleteNode(listItem.getUniqueId());
                return null;
            }

            @Override
            public void succeeded() {
                listView.getItems().remove(listItem);
                List<Tab> tabsToRemove = new ArrayList<>();
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getId().equals(listItem.getUniqueId())) {
                        tabsToRemove.add(tab);
                        tab.getOnCloseRequest().handle(null);
                    }
                }
                tabPane.getTabs().removeAll(tabsToRemove);
                listView.getSelectionModel().select(null);
            }

            @Override
            public void failed() {
                Node node = saveAndRestoreService.getNode(listItem.getUniqueId());
                if (node == null) {
                    listView.getItems().remove(listItem);
                }
                ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                        MessageFormat.format(Messages.errorDeleteNodeFailed, listItem.getName()), null);
            }
        };

        new Thread(task).start();
    }

    private void nodeDoubleClicked(TreeItem<Node> node) {

        // Disallow opening a tab multiple times for the same save set.
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getId() != null && tab.getId().equals(node.getValue().getUniqueId())) {
                return;
            }
        }

        Tab tab;

        switch (node.getValue().getNodeType()) {
            case CONFIGURATION:
                tab = new SaveSetTab(node.getValue(), saveAndRestoreService);
                break;
            case SNAPSHOT:
                tab = new SnapshotTab(node.getValue(), saveAndRestoreService);
                ((SnapshotTab) tab).loadSnapshot(node.getValue());
                break;
            case FOLDER:
            default:
                return;
        }

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    @Override
    protected void renameNode() {
        Node node = treeView.getSelectionModel().getSelectedItem().getValue();
        List<String> existingSiblingNodes =
                listView.getItems().stream()
                        .map(Node::getName)
                        .collect(Collectors.toList());

        renameNode(node, existingSiblingNodes);
    }

    @Override
    protected TreeItem<Node> createTreeItem(final Node node) {
        return new TreeItem<>(node) {
            @Override
            public boolean isLeaf() {
                return node.getNodeType().equals(NodeType.CONFIGURATION);
            }
        };
    }

    @Override
    public void nodeChanged(Node node) {
        // Find the node that has changed
        if (node.getNodeType() != NodeType.SNAPSHOT) {
            TreeItem<Node> nodeSubjectToUpdate = recursiveSearch(node.getUniqueId(), treeView.getRoot());
            if (nodeSubjectToUpdate == null) {
                return;
            }
            nodeSubjectToUpdate.setValue(node);
            TreeItem<Node> parent = nodeSubjectToUpdate.getParent();
            // parent is null if nodeSubjectToUpdate is root
            if(parent == null){
                return;
            }
            parent.getChildren().sort(treeNodeComparator);
            browserSelectionModel.clearSelection();
            browserSelectionModel.select(nodeSubjectToUpdate);
            // Folder node changes may include structure changes, so expand to force update.
            if (nodeSubjectToUpdate.getValue().getNodeType().equals(NodeType.FOLDER)) {
                if (nodeSubjectToUpdate.getParent() != null) { // null means root folder as it has no parent
                    nodeSubjectToUpdate.getParent().getChildren().sort(treeNodeComparator);
                }
                expandTreeNode(nodeSubjectToUpdate);
            }
        } else {
            listView.getItems().stream()
                    .filter(item -> item.getUniqueId().equals(node.getUniqueId()))
                    .findFirst()
                    .ifPresent(item -> {
                        listView.getItems().remove(item);
                        listView.getItems().add(node);
                        listView.getItems().sort(new NodeComparator());
                        listView.getSelectionModel().clearSelection();
                        listView.getSelectionModel().select(node);
                    });
        }
    }

    @Override
    public void nodesAdded(Node parentNode, List<Node> newNodes) {
        for (Node newNode : newNodes) {
            if (newNode.getNodeType() != NodeType.SNAPSHOT) {
                // Find the parent to which the new node is to be added
                TreeItem<Node> parentTreeItem = recursiveSearch(parentNode.getUniqueId(), treeView.getRoot());
                if (parentTreeItem == null) {
                    return;
                }
                parentTreeItem.getChildren().add(createTreeItem(newNode));
                parentTreeItem.getChildren().sort(treeNodeComparator);
                parentTreeItem.expandedProperty().setValue(true);
            } else {
                if (treeView.getSelectionModel().getSelectedItem().getValue().getUniqueId().equals(parentNode.getUniqueId())) {
                    listView.getItems().add(newNode);
                    listView.getItems().sort(new NodeComparator());
                }
            }
        }
    }

    @Override
    public void locateNode(Stack<Node> nodeStack) {
        TreeItem<Node> parentTreeItem = treeView.getRoot();

        while (nodeStack.size() > 1) {
            Node currentNode = nodeStack.pop();
            TreeItem<Node> currentTreeItem = recursiveSearch(currentNode.getUniqueId(), parentTreeItem);
            currentTreeItem.setExpanded(true);
            parentTreeItem = currentTreeItem;
        }

        browserSelectionModel.clearSelection();
        browserSelectionModel.select(parentTreeItem);
        treeView.scrollTo(treeView.getSelectionModel().getSelectedIndex());

        Node currentNode = nodeStack.pop();
        listView.getSelectionModel().clearSelection();
        listView.getSelectionModel().select(currentNode);
        listView.scrollTo(listView.getSelectionModel().getSelectedIndex());
    }

    private class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(Node node1, Node node2) {
            return (preferencesReader.getBoolean("sortSnapshotsTimeReversed") ? -1 : 1) * node1.getCreated().compareTo(node2.getCreated());
        }
    }

    @Override
    public void comapreSnapshot() {
        compareSnapshot(listView.getSelectionModel().getSelectedItem());
    }

    @Override
    public void exportSnapshot() {
        exportSnapshot(listView.getSelectionModel().getSelectedItem());
    }

    @Override
    protected void addTagToSnapshot() {
        addTagToSnapshot(listView.getSelectionModel().getSelectedItem());
    }

    @Override
    protected void tagWithComment(ObservableList<MenuItem> tagList) {
        Node node = listView.getSelectionModel().getSelectedItem();
        tagWithComment(node, tagList);
    }
}
