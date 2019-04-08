/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.ui.dialog.DialogHelper;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;

import javax.swing.tree.TreeNode;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class SaveAndRestoreController implements Initializable{

    private static Executor UI_EXECUTOR = Platform::runLater;

    @FXML
    private TreeView<Node> treeView;

    @FXML
    private TabPane tabPane;

    private SaveAndRestoreService service;
    private ContextMenu folderContextMenu;
    private ContextMenu saveSetContextMenu;
    private ContextMenu snapshotContextMenu;
    private ContextMenu rootFolderContextMenu;

    //private String nodeNameBeingEdited;

    public SaveAndRestoreController() {
        service = SaveAndRestoreService.getInstance();
        folderContextMenu = new ContextMenu();
        MenuItem newFolderMenuItem = new MenuItem("New Folder");
        newFolderMenuItem.setOnAction(ae -> {
            handleNewFolder(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem deleteFolderMenuItem = new MenuItem("Delete Folder");
        deleteFolderMenuItem.setOnAction(ae -> {
            deleteFolder(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem newSaveSetMenuItem = new MenuItem("New Save Set");
        newSaveSetMenuItem.setOnAction(ae -> {
            handleNewSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        folderContextMenu.getItems().addAll(newFolderMenuItem, deleteFolderMenuItem, newSaveSetMenuItem);

        rootFolderContextMenu = new ContextMenu();
        MenuItem newRootFolderMenuItem = new MenuItem("New Folder");
        newRootFolderMenuItem.setOnAction(ae -> {
            handleNewFolder(treeView.getSelectionModel().getSelectedItem());
        });
        rootFolderContextMenu.getItems().add(newRootFolderMenuItem);

        saveSetContextMenu = new ContextMenu();

        MenuItem deleteSaveSetMenuItem = new MenuItem("Delete");
        deleteSaveSetMenuItem.setOnAction(ae -> {
            handleDeleteSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem openSaveSetMenuItem = new MenuItem("Open");
        openSaveSetMenuItem.setOnAction(ae -> {
            handleOpenSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem editSaveSetMenuItem = new MenuItem("Edit");
        editSaveSetMenuItem.setOnAction(ae -> {
            nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
        });

        saveSetContextMenu.getItems().addAll(openSaveSetMenuItem, editSaveSetMenuItem, deleteSaveSetMenuItem);

        snapshotContextMenu = new ContextMenu();
        MenuItem deleteSnapshotMenuItem = new MenuItem("Delete Snapshot");
        deleteSnapshotMenuItem.setOnAction(ae -> {
            handleDeleteSnapshot(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem compareSaveSetMenuItem = new MenuItem("Compare Snapshots");
        compareSaveSetMenuItem.setOnAction(ae -> {
            handleCompareSnapshot(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem tagAsGolden = new MenuItem("Tag as Golden");
        tagAsGolden.setOnAction(ae -> {
            handleTagAsGolden(treeView.getSelectionModel().getSelectedItem());
        });

        snapshotContextMenu.getItems().addAll(deleteSnapshotMenuItem, compareSaveSetMenuItem, tagAsGolden);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        treeView.setEditable(true);

//        treeView.setOnMouseClicked(me -> {
//            TreeItem<Node> item = treeView.getSelectionModel().getSelectedItem();
//            if (item != null) {
//                if (me.getClickCount() == 2) {
//                    nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
//                }
//            }
//        });
//
        treeView.setOnEditCommit(event -> {
            handleNodeRenamed(event);
        });

        loadInitialTreeData();
    }

    /**
     * Loads the data for the tree root as provided (persisted) by the current
     * {@link DataProvider}.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void loadInitialTreeData() {

        try {

            Node rootNode = service.getRootNode();
            rootNode.setName(service.getServiceIdentifier());

            TreeItem<Node> rootItem = createNode(rootNode);

            treeView.setCellFactory(p -> new BrowserTreeCell(folderContextMenu, saveSetContextMenu, snapshotContextMenu, rootFolderContextMenu));

            rootItem.addEventHandler(TreeItem.branchExpandedEvent(), e -> {
                expandTreeNode(((TreeItem.TreeModificationEvent)e).getTreeItem());
            });

            UI_EXECUTOR.execute(() -> {
                treeView.setRoot(rootItem);
                rootItem.setExpanded(true);
            });
        } catch (Exception e) {
            showJMasarServiceUnabvailable();
        }
    }


    private void expandTreeNode(TreeItem<Node> targetItem) {

        targetItem.getChildren().clear();

        List<Node> childNodes = service.getChildNodes(targetItem.getValue());;

        Collections.sort(childNodes);
        UI_EXECUTOR.execute(() -> {
            targetItem.getChildren().addAll(childNodes.stream().map(n -> createNode(n)).collect(Collectors.toList()));
        });
    }

    private void handleDeleteSnapshot(TreeItem<Node> treeItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete snapshot?");
        alert.setContentText("Deletion is irreversible. Do you wish to continue?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            TreeItem<Node> parent = treeItem.getParent();
            try {
                service.deleteNode(treeItem.getValue().getUniqueId());
                UI_EXECUTOR.execute(() -> {
                    parent.getChildren().remove(treeItem);
                });
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void handleCompareSnapshot(TreeItem<Node> treeItem) {
        try {
            SnapshotTab currentTab = (SnapshotTab)tabPane.getSelectionModel().getSelectedItem();
            currentTab.addSnapshot(treeItem.getParent().getValue().getUniqueId(), treeItem.getValue());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void handleTagAsGolden(TreeItem<Node> treeItem) {

        try {
            service.tagSnapshotAsGolden(treeItem.getValue().getUniqueId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteSaveSet(TreeItem<Node> treeItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete save set?");
        alert.setHeaderText("All snapshots for this save set will be deleted!");
        alert.setContentText("Deletion is irreversible. Do you wish to continue?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            TreeItem<Node> parent = treeItem.getParent();
            try {
                service.deleteNode(treeItem.getValue().getUniqueId());
                UI_EXECUTOR.execute(() -> {
                    parent.getChildren().remove(treeItem);
                    for(Tab tab : tabPane.getTabs()) {
                        if(tab.getText().equals(treeItem.getValue().getName())) {
                            tabPane.getTabs().remove(tab);
                            break;
                        }
                    }
                });
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void handleOpenSaveSet(TreeItem<Node> treeItem) {
        SnapshotTab tab = new SnapshotTab(treeView.getSelectionModel().getSelectedItem().getValue());
        tab.loadSaveSet(treeView.getSelectionModel().getSelectedItem().getValue());

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }


    private void handleNewFolder(TreeItem<Node> parentTreeItem) {

        List<String> existingFolderNames =
                parentTreeItem.getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(NodeType.FOLDER))
                        .map(item -> item.getValue().getName())
                        .collect(Collectors.toList());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setContentText("Specify a folder name:");
        dialog.setHeaderText(null);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

        dialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            String value = newValue.trim();
            dialog.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(existingFolderNames.contains(value) || value.isEmpty());
        });

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            Node newFolderNode = Node.builder()
                    .name(result.get())
                    .build();
            try {
                Node newTreeNode = service
                        .createNode(treeView.getSelectionModel().getSelectedItem().getValue().getUniqueId(), newFolderNode);
                parentTreeItem.getChildren().add(createNode(newTreeNode));
                parentTreeItem.getChildren().sort((a, b) -> a.getValue().getName().compareTo(b.getValue().getName()));
                parentTreeItem.setExpanded(true);
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Action failed");
                alert.setHeaderText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void deleteFolder(TreeItem<Node> treeItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete folder?");
        alert.setHeaderText("All folders, save sets and snapshots in this folder and sub-folders will be deleted!");
        alert.setContentText("Deletion is irreversible. Do you wish to continue?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            TreeItem<Node> parent = treeItem.getParent();
            try {
                if(!service.deleteNode(treeItem.getValue().getUniqueId())) {
                    alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Delete failed");
                    alert.setHeaderText("Selected folder was not deleted on server");
                    alert.showAndWait();
                }
                else {
                    UI_EXECUTOR.execute(() -> {
                        parent.getChildren().remove(treeItem);
                    });
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    private void nodeDoubleClicked(TreeItem<Node> newValue) {

        // Disallow opening a tab multiple times for the same save set.
        for(Tab tab : tabPane.getTabs()) {
            if(tab.getId().equals(newValue.getValue().getUniqueId())) {
                return;
            }
        }

        Tab tab;

        switch (newValue.getValue().getNodeType()) {
            case CONFIGURATION:
                tab = new SaveSetTab(treeView.getSelectionModel().getSelectedItem().getValue());
                break;
            case SNAPSHOT:
                tab = new SnapshotTab(treeView.getSelectionModel().getSelectedItem().getValue());
                ((SnapshotTab) tab).loadSnapshot(treeView.getSelectionModel().getSelectedItem().getValue());
                break;
            case FOLDER:
            default:
                return;
        }

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

    }

    private void handleNewSaveSet(TreeItem<Node> parentTreeItem){

        List<String> existingFolderNames =
                parentTreeItem.getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(NodeType.CONFIGURATION))
                        .map(item -> item.getValue().getName())
                        .collect(Collectors.toList());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Save Set");
        dialog.setContentText("Specify a save set name:");
        dialog.setHeaderText(null);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

        dialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            String value = newValue.trim();
            dialog.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(existingFolderNames.contains(value) || value.isEmpty());
        });

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            Node newSateSetNode = Node.builder()
                    .nodeType(NodeType.CONFIGURATION)
                    .name(result.get())
                    .build();
            try {
                Node newTreeNode = service
                        .createNode(treeView.getSelectionModel().getSelectedItem().getValue().getUniqueId(), newSateSetNode);
                TreeItem<Node> newSaveSetNode = createNode(newTreeNode);
                parentTreeItem.getChildren().add(newSaveSetNode);
                parentTreeItem.getChildren().sort((a, b) -> a.getValue().getName().compareTo(b.getValue().getName()));
                parentTreeItem.setExpanded(true);
                nodeDoubleClicked(newSaveSetNode);
                treeView.getSelectionModel().select(treeView.getRow(newSaveSetNode));
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Action failed");
                alert.setHeaderText(e.getMessage());
                alert.showAndWait();
            }
        }
    }


    /**
     * Renames a node through the service and its underlying data provider.
     * If there is a problem in the call to the remote JMasar service,
     * the user is shown a suitable error dialog and the name of the node is restored.
     * If the node being renamed is a save set ({@link Node} and if it is opened
     * in the editor, the tab title is also updated with the new name.
     * to its old value.
     * @param event The event holding the data needed to find the new name.
     */
    private void handleNodeRenamed(TreeView.EditEvent<Node> event){
//        try {
//            service.rename(event.getSource().g);
//            if(event.getTreeItem().getValue().getNodeType().equals(NodeType.CONFIGURATION)){
//                for(Tab tab : tabPane.getTabs()){
//                    if(tab.getId().equals(Integer.toString(event.getTreeItem().getValue().getId()))){
//                        tab.setText(event.getNewValue().getName());
//                        break;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            event.getTreeItem().getValue().setName(nodeNameBeingEdited);
//            Alert alert = new Alert(AlertType.ERROR);
//            alert.setTitle("Rename failed");
//            alert.setHeaderText("Selected node not renamed on server");

//            alert.showAndWait();
//        }
    }


    private void showJMasarServiceUnabvailable(){
        Node node = Node.builder().name(service.getServiceIdentifier() + " - currently unavailable").build();
        treeView.setRoot(new TreeItem<>(node));
        treeView.setCellFactory(new Callback<TreeView<Node>, TreeCell<Node>>() {
            @Override
            public TreeCell<Node> call(TreeView<Node> p) {
                return new RemoteServiceUnavailableCell();
            }
        });
    }

    private class RemoteServiceUnavailableCell extends TreeCell<Node>{

        private HBox cellGraphic;

        public RemoteServiceUnavailableCell(){

            cellGraphic = new HBox();
            HBox.setMargin(cellGraphic, new Insets(100, 0, 0,0));
            Label label = new Label("JMasar Service unavailable");
            label.setMaxHeight(Double.MAX_VALUE);
            HBox.setMargin(label, new Insets(5, 10, 5,5));
            Button reconnect = new Button("Reconnect");
            reconnect.setOnAction(ae -> {
                reconnect();
            });

            cellGraphic.getChildren().addAll(label, reconnect);
        }

        public void reconnect(){
            try {
                loadInitialTreeData();
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Reconnection failed");
                alert.setHeaderText("Unable to connect to " + service.getServiceIdentifier());
                alert.showAndWait();
            }
        }

        @Override
        public void updateItem(Node node, boolean empty) {
            super.updateItem(node, empty);
            if(!empty){
                setGraphic(cellGraphic);
            }
        }
    }

    private TreeItem<Node> createNode(final Node node){
        return new TreeItem<>(node){
            @Override
            public boolean isLeaf(){
                return node.getNodeType().equals(NodeType.SNAPSHOT);
            }
        };
    }
}
