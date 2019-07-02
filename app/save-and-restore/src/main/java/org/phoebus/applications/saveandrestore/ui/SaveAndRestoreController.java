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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.NodeChangeListener;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.ui.javafx.ImageCache;
import org.springframework.beans.factory.annotation.Autowired;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class SaveAndRestoreController implements Initializable, NodeChangeListener {

    private static Executor UI_EXECUTOR = Platform::runLater;

    public static final Image folderIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/small/Folder@.png");
    public static final Image saveSetIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/small/Save-set@.png");
    public static final Image deleteIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/delete.png");
    public static final Image renameIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/rename_col.png");
    public static final Image snapshotIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/small/Snap-shot@.png");
    public static final Image snapshotGoldenIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/small/Snap-shot-golden@.png");

    @FXML
    private TreeView<Node> treeView;

    @FXML
    private TabPane tabPane;

    @FXML
    private Label jmasarServiceTitle;

    @FXML
    private Button reconnectButton;

    @FXML
    private Label emptyTreeInstruction;

    @Autowired
    private SaveAndRestoreService saveAndRestoreService;

    private ContextMenu folderContextMenu;
    private ContextMenu saveSetContextMenu;
    private ContextMenu snapshotContextMenu;
    private ContextMenu rootFolderContextMenu;

    private SimpleStringProperty toggleGoldenMenuItemText = new SimpleStringProperty();
    private SimpleStringProperty jmasarServiceTitleProperty = new SimpleStringProperty();
    private BooleanProperty remoteServiceUnavailable = new SimpleBooleanProperty(false);
    private BooleanProperty treeViewEmpty = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<ImageView> toggleGoldenImageViewProperty = new SimpleObjectProperty<>();

    private ImageView snapshotImageView = new ImageView(snapshotIcon);
    private ImageView snapshotGoldenImageView = new ImageView(snapshotGoldenIcon);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        folderContextMenu = new ContextMenu();
        MenuItem newFolderMenuItem = new MenuItem("New Folder", new ImageView(folderIcon));
        newFolderMenuItem.setOnAction(ae -> {
            handleNewFolder(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem renameFolderMenuItem = new MenuItem("Rename", new ImageView(renameIcon));
        renameFolderMenuItem.setOnAction(ae -> {
            handleRenameNode(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem deleteFolderMenuItem = new MenuItem("Delete", new ImageView(deleteIcon));
        deleteFolderMenuItem.setOnAction(ae -> {
            deleteFolder(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem newSaveSetMenuItem = new MenuItem("New Save Set", new ImageView(saveSetIcon));
        newSaveSetMenuItem.setOnAction(ae -> {
            handleNewSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        folderContextMenu.getItems().addAll(newFolderMenuItem, renameFolderMenuItem, deleteFolderMenuItem, newSaveSetMenuItem);

        rootFolderContextMenu = new ContextMenu();
        MenuItem newRootFolderMenuItem = new MenuItem("New Folder", new ImageView(folderIcon));
        newRootFolderMenuItem.setOnAction(ae -> {
            handleNewFolder(treeView.getSelectionModel().getSelectedItem());
        });
        rootFolderContextMenu.getItems().add(newRootFolderMenuItem);

        saveSetContextMenu = new ContextMenu();

        MenuItem deleteSaveSetMenuItem = new MenuItem("Delete", new ImageView(deleteIcon));
        deleteSaveSetMenuItem.setOnAction(ae -> {
            handleDeleteSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem renameSaveSetMenuItem = new MenuItem("Rename", new ImageView(renameIcon));
        renameSaveSetMenuItem.setOnAction(ae -> {
            handleRenameNode(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem openSaveSetMenuItem = new MenuItem("Open", new ImageView(saveSetIcon));
        openSaveSetMenuItem.setOnAction(ae -> {
            handleOpenSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem editSaveSetMenuItem = new MenuItem("Edit");
        editSaveSetMenuItem.setOnAction(ae -> {
            nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
        });

        saveSetContextMenu.getItems().addAll(openSaveSetMenuItem, editSaveSetMenuItem, renameSaveSetMenuItem, deleteSaveSetMenuItem);

        snapshotContextMenu = new ContextMenu();
        MenuItem deleteSnapshotMenuItem = new MenuItem("Delete", new ImageView(deleteIcon));
        deleteSnapshotMenuItem.setOnAction(ae -> {
            handleDeleteSnapshot(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem renameSnapshotItem = new MenuItem("Rename", new ImageView(renameIcon));
        renameSnapshotItem.setOnAction(ae -> {
            handleRenameNode(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem compareSaveSetMenuItem = new MenuItem("Compare Snapshots");
        compareSaveSetMenuItem.setOnAction(ae -> {
            handleCompareSnapshot(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem tagAsGolden = new MenuItem("Tag as Golden", new ImageView(snapshotGoldenIcon));
        tagAsGolden.textProperty().bind(toggleGoldenMenuItemText);
        tagAsGolden.graphicProperty().bind(toggleGoldenImageViewProperty);
        tagAsGolden.setOnAction(ae -> {
            Node node = toggleGoldenProperty(treeView.getSelectionModel().getSelectedItem());
            treeView.getSelectionModel().getSelectedItem().setValue(node);
        });

        snapshotContextMenu.getItems().addAll(renameSnapshotItem, deleteSnapshotMenuItem, compareSaveSetMenuItem, tagAsGolden);

        treeView.setEditable(true);

        treeView.setOnMouseClicked(me -> {
            TreeItem<Node> item = treeView.getSelectionModel().getSelectedItem();
            if (item != null && item.getValue().getNodeType().equals(NodeType.SNAPSHOT)) {
                toggleGoldenMenuItemText.set(Boolean.parseBoolean(item.getValue().getProperty("golden")) ? "Remove Golden tag" : "Tag as Golden");
                toggleGoldenImageViewProperty.set(Boolean.parseBoolean(item.getValue().getProperty("golden")) ? snapshotImageView : snapshotGoldenImageView);
                if (me.getClickCount() == 2) {
                    nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
                }
            }
        });

        treeView.setShowRoot(false);

        reconnectButton.visibleProperty().bind(remoteServiceUnavailable);
        jmasarServiceTitle.textProperty().bind(jmasarServiceTitleProperty);

        ContextMenu treeViewContextMenu = new ContextMenu();
        MenuItem newTopLevelFolderMenuItem = new MenuItem("New Folder", new ImageView(folderIcon));
        newTopLevelFolderMenuItem.setOnAction(ae -> {
            handleNewFolder(treeView.getRoot());
        });

        treeViewContextMenu.getItems().addAll(newTopLevelFolderMenuItem);

        treeView.setContextMenu(treeViewContextMenu);

        emptyTreeInstruction.visibleProperty().bindBidirectional(treeViewEmpty);

        loadInitialTreeData();

        saveAndRestoreService.addNodeChangeListener(this);
    }

    @FXML
    public void reconnect(){
        if(!loadInitialTreeData()){
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setHeaderText(null);
            alert.setContentText("Unable to reconnect to JMasar service.");
            alert.showAndWait();
        }
    }

    /**
     * Loads the data for the tree root as provided (persisted) by the current
     * {@link DataProvider}.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    private boolean loadInitialTreeData() {
        Node rootNode = saveAndRestoreService.getRootNode();
        if(rootNode == null){
            jmasarServiceTitleProperty.set("JMasar serivce @ " +  saveAndRestoreService.getServiceIdentifier() + " unavailable!");
            remoteServiceUnavailable.set(true);
            return false;
        }

        remoteServiceUnavailable.set(false);
        jmasarServiceTitleProperty.set(saveAndRestoreService.getServiceIdentifier());

        TreeItem<Node> rootItem = createNode(rootNode);

        treeView.setCellFactory(p -> new BrowserTreeCell(folderContextMenu,
                saveSetContextMenu, snapshotContextMenu, rootFolderContextMenu));

        rootItem.addEventHandler(TreeItem.branchExpandedEvent(), e -> {
            expandTreeNode(((TreeItem.TreeModificationEvent)e).getTreeItem());
        });

        rootItem.addEventHandler(TreeItem.treeNotificationEvent(), e -> {
            treeViewEmpty.set(treeView.getRoot().getChildren().isEmpty());
        });

        UI_EXECUTOR.execute(() -> {
            treeView.setRoot(rootItem);
            rootItem.setExpanded(true);
        });

        return true;
    }


    private void expandTreeNode(TreeItem<Node> targetItem) {

        targetItem.getChildren().clear();

        List<Node> childNodes = saveAndRestoreService.getChildNodes(targetItem.getValue());;

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
            UI_EXECUTOR.execute(() -> {
                try {
                    saveAndRestoreService.deleteNode(treeItem.getValue().getUniqueId());
                    parent.getChildren().remove(treeItem);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleCompareSnapshot(TreeItem<Node> treeItem) {
        try {
            SnapshotTab currentTab = (SnapshotTab)tabPane.getSelectionModel().getSelectedItem();
            currentTab.addSnapshot(treeItem.getValue());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Node toggleGoldenProperty(TreeItem<Node> treeItem) {

        try {
            return saveAndRestoreService.tagSnapshotAsGolden(treeItem.getValue(),
                    !Boolean.parseBoolean(treeItem.getValue().getProperty("golden")));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
                saveAndRestoreService.deleteNode(treeItem.getValue().getUniqueId());
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
        SnapshotTab tab = new SnapshotTab(treeView.getSelectionModel().getSelectedItem().getValue(), saveAndRestoreService);
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
        dialog.setContentText("Specify a folder name (case sensitive):");
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
                Node newTreeNode = saveAndRestoreService
                        .createNode(parentTreeItem.getValue().getUniqueId(), newFolderNode);
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
                if(!saveAndRestoreService.deleteNode(treeItem.getValue().getUniqueId())) {
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


    private void nodeDoubleClicked(TreeItem<Node> node) {

        // Disallow opening a tab multiple times for the same save set.
        for(Tab tab : tabPane.getTabs()) {
            if(tab.getId().equals(node.getValue().getUniqueId())) {
                return;
            }
        }

        Tab tab;

        switch (node.getValue().getNodeType()) {
            case CONFIGURATION:
                tab = new SaveSetTab(node.getValue(), saveAndRestoreService);
                break;
            case SNAPSHOT:
                tab = new SnapshotTab(treeView.getSelectionModel().getSelectedItem().getValue(), saveAndRestoreService);
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
        dialog.setContentText("Specify a save set name (case sensitive):");
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
                Node newTreeNode = saveAndRestoreService
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
     * @param node The node being renamed
     */
    private void handleRenameNode(TreeItem<Node> node){

        List<String> existingSiblingNodes =
                node.getParent().getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(node.getValue().getNodeType()))
                        .map(item -> item.getValue().getName())
                        .collect(Collectors.toList());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rename node");
        dialog.setContentText("Specify a new name (case sensitive):");
        dialog.setHeaderText(null);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        dialog.getEditor().textProperty().setValue(node.getValue().getName());


        dialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            String value = newValue.trim();
            dialog.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(existingSiblingNodes.contains(value) || value.isEmpty());
        });

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            node.getValue().setName(result.get());
            try {
                saveAndRestoreService.updateNode(node.getValue());
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Action failed");
                alert.setHeaderText(e.getMessage());
                alert.showAndWait();
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

    @Override
    public void nodeChanged(Node node){
        // Find the node that has changed
        TreeItem<Node> nodeSubjectToUpdate = recursiveSearch(node.getUniqueId(), treeView.getRoot());
        if(nodeSubjectToUpdate == null){
            // TODO: log this
            return;
        }
        nodeSubjectToUpdate.setValue(node);
    }

    private TreeItem<Node> recursiveSearch(String nodeIdToLocate, TreeItem<Node> node){
        if (node.getValue().getUniqueId().equals(nodeIdToLocate))
            return node;
        List<TreeItem<Node>> childNodes = node.getChildren();
        TreeItem<Node> result = null;
        for (int i = 0; result == null && i < childNodes.size(); i++) {
            result = recursiveSearch(nodeIdToLocate, childNodes.get(i));
        }
        return result;
    }
}
