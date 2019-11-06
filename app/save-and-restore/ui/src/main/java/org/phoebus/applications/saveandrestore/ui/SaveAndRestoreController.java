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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.NodeAddedListener;
import org.phoebus.applications.saveandrestore.data.NodeChangedListener;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.ui.javafx.ImageCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class SaveAndRestoreController implements Initializable, NodeChangedListener, NodeAddedListener {

    private static Executor UI_EXECUTOR = Platform::runLater;

    public static final Image folderIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/folder.png");
    public static final Image saveSetIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/saveset.png");
    public static final Image editSaveSetIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/edit_saveset.png");
    public static final Image deleteIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/delete.png");
    public static final Image renameIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/rename_col.png");
    public static final Image snapshotIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot.png");
    public static final Image snapshotGoldenIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-golden.png");
    public static final Image compareSnapshotIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/compare.png");

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

    @Autowired
    private Preferences preferences;

    @Autowired
    private ObjectMapper objectMapper;

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

    private static final String TREE_STATE = "tree_state";

    private static final Logger LOG = LoggerFactory.getLogger(SaveAndRestoreService.class.getName());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        reconnectButton.textProperty().setValue(Messages.buttonReconnect);
        emptyTreeInstruction.textProperty().setValue(Messages.labelCreateFolderEmptyTree);

        folderContextMenu = new ContextMenu();
        MenuItem newFolderMenuItem = new MenuItem(Messages.contextMenuNewFolder, new ImageView(folderIcon));
        newFolderMenuItem.setOnAction(ae -> {
            createNewFolder(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem renameFolderMenuItem = new MenuItem(Messages.contextMenuRename, new ImageView(renameIcon));
        renameFolderMenuItem.setOnAction(ae -> {
            renameNode(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem deleteFolderMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(deleteIcon));
        deleteFolderMenuItem.setOnAction(ae -> {
            deleteFolder(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem newSaveSetMenuItem = new MenuItem(Messages.contextMenuNewSaveSet, new ImageView(saveSetIcon));
        newSaveSetMenuItem.setOnAction(ae -> {
            handleNewSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        folderContextMenu.getItems().addAll(newFolderMenuItem, renameFolderMenuItem, deleteFolderMenuItem, newSaveSetMenuItem);

        rootFolderContextMenu = new ContextMenu();
        MenuItem newRootFolderMenuItem = new MenuItem(Messages.contextMenuNewFolder, new ImageView(folderIcon));
        newRootFolderMenuItem.setOnAction(ae -> {
            createNewFolder(treeView.getSelectionModel().getSelectedItem());
        });
        rootFolderContextMenu.getItems().add(newRootFolderMenuItem);

        saveSetContextMenu = new ContextMenu();

        MenuItem deleteSaveSetMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(deleteIcon));
        deleteSaveSetMenuItem.setOnAction(ae -> {
            deleteSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem renameSaveSetMenuItem = new MenuItem(Messages.contextMenuRename, new ImageView(renameIcon));
        renameSaveSetMenuItem.setOnAction(ae -> {
            renameNode(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem openSaveSetMenuItem = new MenuItem(Messages.contextMenuOpen, new ImageView(saveSetIcon));
        openSaveSetMenuItem.setOnAction(ae -> {
            openSaveSetForSnapshot(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem editSaveSetMenuItem = new MenuItem(Messages.contextMenuEdit, new ImageView(editSaveSetIcon));
        editSaveSetMenuItem.setOnAction(ae -> {
            nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
        });

        saveSetContextMenu.getItems().addAll(openSaveSetMenuItem, editSaveSetMenuItem, renameSaveSetMenuItem, deleteSaveSetMenuItem);

        snapshotContextMenu = new ContextMenu();
        MenuItem deleteSnapshotMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(deleteIcon));
        deleteSnapshotMenuItem.setOnAction(ae -> {
            deleteSnapshot(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem renameSnapshotItem = new MenuItem(Messages.contextMenuRename, new ImageView(renameIcon));
        renameSnapshotItem.setOnAction(ae -> {
            renameNode(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem compareSaveSetMenuItem = new MenuItem(Messages.contextMenuCompareSnapshots, new ImageView(compareSnapshotIcon));
        compareSaveSetMenuItem.setOnAction(ae -> {
            if(treeView.getSelectionModel().getSelectedItems().size() != 1){
                System.out.println("AA");
            }
            comapreSnapshot(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem tagAsGolden = new MenuItem(Messages.contextMenuTagAsGolden, new ImageView(snapshotGoldenIcon));
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
            if(item == null){
                return;
            }
            if (item.getValue().getNodeType().equals(NodeType.SNAPSHOT)) {
                toggleGoldenMenuItemText.set(Boolean.parseBoolean(item.getValue().getProperty("golden")) ? Messages.contextMenuRemoveGoldenTag : Messages.contextMenuTagAsGolden);
                toggleGoldenImageViewProperty.set(Boolean.parseBoolean(item.getValue().getProperty("golden")) ? snapshotImageView : snapshotGoldenImageView);
            }
            if (me.getClickCount() == 2) {
                nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
            }
        });

        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        reconnectButton.visibleProperty().bind(remoteServiceUnavailable);
        jmasarServiceTitle.textProperty().bind(jmasarServiceTitleProperty);

        ContextMenu treeViewContextMenu = new ContextMenu();
        MenuItem newTopLevelFolderMenuItem = new MenuItem(Messages.contextMenuNewTopLevelFolder, new ImageView(folderIcon));
        newTopLevelFolderMenuItem.setOnAction(ae -> {
            createNewFolder(treeView.getRoot());
        });

        treeViewContextMenu.getItems().addAll(newTopLevelFolderMenuItem);

        treeView.setContextMenu(treeViewContextMenu);

        emptyTreeInstruction.visibleProperty().bindBidirectional(treeViewEmpty);

        saveAndRestoreService.addNodeChangeListener(this);
        saveAndRestoreService.addNodeAddedListener(this);

        treeView.setCellFactory(p -> new BrowserTreeCell(folderContextMenu,
                saveSetContextMenu, snapshotContextMenu, rootFolderContextMenu));

        loadTreeData();
    }

    /**
     * Loads the data for the tree root as provided (persisted) by the current
     * {@link DataProvider}.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    @FXML
    public void loadTreeData() {

        Task<TreeItem<Node>> loadRootNode = new Task<>() {
            @Override
            protected TreeItem<Node> call() throws Exception {
                Node rootNode = saveAndRestoreService.getRootNode();
                TreeItem<Node> rootItem = createNode(rootNode);
                String savedTreeState = preferences.get(TREE_STATE, null);
                if(savedTreeState != null){
                    try {
                        List<String> savedTreeViewStructure =
                                objectMapper.readValue(savedTreeState, new TypeReference<List<String>>() {});
                        HashMap<String, List<TreeItem<Node>>> childNodesMap = new HashMap<>();

                        savedTreeViewStructure.stream().forEach(s -> {
                            List<Node> childNodes = saveAndRestoreService.getChildNodes(Node.builder().uniqueId(s).build());
                            if(childNodes != null) { // This may be the case if the tree structure was modified outside of the UI
                                List<TreeItem<Node>> childItems = childNodes.stream().map(n -> createNode(n)).collect(Collectors.toList());
                                childNodesMap.put(s, childItems);
                            }
                        });
                        setChildItems(childNodesMap, rootItem);

                    }
                    catch(IOException e){
                        // Failed to load tree structure.
                        LOG.error("Unable to obtain tree node data from service", e);
                    }
                }
                else{
                    List<Node> childNodes = saveAndRestoreService.getChildNodes(rootItem.getValue());
                    List<TreeItem<Node>> childItems = childNodes.stream().map(n -> createNode(n)).collect(Collectors.toList());
                    treeViewEmpty.setValue(childItems.isEmpty());
                    rootItem.getChildren().addAll(childItems);
                }

                return rootItem;
            }

            @Override
            public void succeeded(){
                TreeItem<Node> rootItem = getValue();

                //if(rootItem != null){
                    remoteServiceUnavailable.set(false);
                    jmasarServiceTitleProperty.set(saveAndRestoreService.getServiceIdentifier());

                    treeView.setRoot(rootItem);
                    restoreTreeState();
                    /*
                }
                else{
                    jmasarServiceTitleProperty.set(MessageFormat.format(Messages.jmasarServiceUnavailable, saveAndRestoreService.getServiceIdentifier()));
                    remoteServiceUnavailable.set(true);
                }

                     */
            }

            @Override
            public void failed(){
                jmasarServiceTitleProperty.set(MessageFormat.format(Messages.jmasarServiceUnavailable, saveAndRestoreService.getServiceIdentifier()));
                remoteServiceUnavailable.set(true);
            }
        };

        new Thread(loadRootNode).start();
    }


    private void expandTreeNode(TreeItem<Node> targetItem) {

        targetItem.getChildren().clear();

        List<Node> childNodes = saveAndRestoreService.getChildNodes(targetItem.getValue());;
        Collections.sort(childNodes);
        UI_EXECUTOR.execute(() -> {
            targetItem.getChildren().addAll(childNodes.stream().map(n -> createNode(n)).collect(Collectors.toList()));
        });

    }

    private void deleteSnapshot(TreeItem<Node> treeItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Messages.promptDeleteSnapshotTitle);
        alert.setContentText(Messages.promptDeleteSnapshotContent);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            deleteTreeItem(treeItem);
        }
    }

    private void comapreSnapshot(TreeItem<Node> treeItem) {

        try {
            SnapshotTab currentTab = (SnapshotTab)tabPane.getSelectionModel().getSelectedItem();
            if(currentTab == null){
                return;
            }
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

    private void deleteSaveSet(TreeItem<Node> treeItem) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(Messages.promptDeleteSaveSetTitle);
        alert.setHeaderText(Messages.promptDeleteSaveSetHeader);
        alert.setContentText(Messages.promptDeleteSaveSetContent);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            deleteTreeItem(treeItem);
        }
    }

    /**
     * Deletes the {@link Node} associated with the {@link TreeItem}, and
     * removes the {@link TreeItem} from the tree view. If {@link Node} is associated
     * with an open tab, that tab is cleaned up and closed.
     * @param treeItem
     */
    private void deleteTreeItem(TreeItem<Node> treeItem){
        TreeItem<Node> parent = treeItem.getParent();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                saveAndRestoreService.deleteNode(treeItem.getValue().getUniqueId());
                return null;
            }

            @Override
            public void succeeded(){
                UI_EXECUTOR.execute(() -> {
                    parent.getChildren().remove(treeItem);
                    List<Tab> tabsToRemove = new ArrayList<>();
                    for(Tab tab : tabPane.getTabs()) {
                        if(tab.getId().equals(treeItem.getValue().getUniqueId())) {
                           tabsToRemove.add(tab);
                           tab.getOnCloseRequest().handle(null);
                        }
                    }
                    tabPane.getTabs().removeAll(tabsToRemove);
                    treeView.getSelectionModel().select(null);
                });
            }
        };

        new Thread(task).start();
    }

    private void openSaveSetForSnapshot(TreeItem<Node> treeItem) {
        SnapshotTab tab = new SnapshotTab(treeItem.getValue(), saveAndRestoreService);
        tab.loadSaveSet(treeItem.getValue());

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void createNewFolder(TreeItem<Node> parentTreeItem) {

        List<String> existingFolderNames =
                parentTreeItem.getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(NodeType.FOLDER))
                        .map(item -> item.getValue().getName())
                        .collect(Collectors.toList());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Messages.contextMenuNewFolder);
        dialog.setContentText(Messages.promptNewFolder);
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
        alert.setTitle(Messages.promptDeleteFolderTitle);
        alert.setHeaderText(Messages.promptDeleteFolderHeader);
        alert.setContentText(Messages.promptDeleteFolderContent);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            TreeItem<Node> parent = treeItem.getParent();
            try {
                if(!saveAndRestoreService.deleteNode(treeItem.getValue().getUniqueId())) {
                    alert = new Alert(AlertType.ERROR);
                    alert.setTitle(Messages.errorDeleteFolderTitle);
                    alert.setHeaderText(Messages.errorDeleteFolderContent);
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
        dialog.setTitle(Messages.promptNewSaveSetTitle);
        dialog.setContentText(Messages.promptNewSaveSetContent);
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
    private void renameNode(TreeItem<Node> node){

        List<String> existingSiblingNodes =
                node.getParent().getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(node.getValue().getNodeType()))
                        .map(item -> item.getValue().getName())
                        .collect(Collectors.toList());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Messages.promptRenameNodeTitle);
        dialog.setContentText(Messages.promptRenameNodeContent);
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
                alert.setTitle(Messages.errorActionFailed);
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
            // TODO: log this?
            return;
        }
        nodeSubjectToUpdate.setValue(node);
        nodeSubjectToUpdate.getParent().getChildren().sort(new TreeNodeComparator());
        treeView.getSelectionModel().select(nodeSubjectToUpdate);
    }

    @Override
    public void nodeAdded(Node parentNode, Node newNode){
        // Find the parent to which the new node is to be added
        TreeItem<Node> parentTreeItem = recursiveSearch(parentNode.getUniqueId(), treeView.getRoot());
        if(parentTreeItem == null){
            // TODO: log this?
            return;
        }
        parentTreeItem.getChildren().add(createNode(newNode));
        parentTreeItem.getChildren().sort(new TreeNodeComparator());
        parentTreeItem.expandedProperty().setValue(true);
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

    public void cleanUp(){
        saveAndRestoreService.removeNodeChangeListener(this);
        saveAndRestoreService.removeNodeAddedListener(this);
        saveTreeState();
    }


    private void saveTreeState(){
        if(treeView.getRoot() == null){
            return;
        }
        List<String> expandedNodes = new ArrayList<>();
        findExpandedNodes(expandedNodes, treeView.getRoot());
        if(expandedNodes.isEmpty()){
            return;
        }
        try {
            preferences.put(TREE_STATE, objectMapper.writeValueAsString(expandedNodes));
        } catch (JsonProcessingException e){
            e.printStackTrace();
        }
    }

    private void findExpandedNodes(List<String> expandedNodes, TreeItem<Node> treeItem){
        if(treeItem.expandedProperty().get() && !treeItem.getChildren().isEmpty()){
            expandedNodes.add(treeItem.getValue().getUniqueId());
            treeItem.getChildren().stream().forEach(ti -> findExpandedNodes(expandedNodes, ti));
        }
    }

    /**
     * Loops through the the tree view model and expands all nodes that have a non-empty children
     * list. The tree view at this point has already been updated with data from the backend.
     */
    private void restoreTreeState(){
        UI_EXECUTOR.execute(() -> {
            expandNodes(treeView.getRoot());

            // Must be added here, after nodes have been expanded. Adding the event handler
            // before expansion of nodes will break the expected behavior when restoring the tree state.
            treeView.getRoot().addEventHandler(TreeItem.branchExpandedEvent(), e -> {
                expandTreeNode(((TreeItem.TreeModificationEvent)e).getTreeItem());
            });

            // This is needed in the rare event that all top level folders have been deleted.
            treeView.getRoot().addEventHandler(TreeItem.treeNotificationEvent(), e -> {
                treeViewEmpty.set(treeView.getRoot().getChildren().isEmpty());
            });
        });
    }

    private void setChildItems(HashMap<String, List<TreeItem<Node>>> allItems, TreeItem<Node> parentItem){
        if(allItems.containsKey(parentItem.getValue().getUniqueId())){
            List<TreeItem<Node>> childItems = allItems.get(parentItem.getValue().getUniqueId());
            parentItem.getChildren().setAll(childItems);
            childItems.stream().forEach(ci -> setChildItems(allItems, ci));
        }
    }

    private void expandNodes(TreeItem<Node> parentNode){
        if(!parentNode.getChildren().isEmpty()){
            parentNode.setExpanded(true);
            for(TreeItem<Node> childNode : parentNode.getChildren()){
                expandNodes(childNode);
            }
        }
    }

    private class TreeNodeComparator implements Comparator<TreeItem<Node>>{
        @Override
        public int compare(TreeItem<Node> t1, TreeItem<Node> t2){
            return t1.getValue().compareTo(t2.getValue());
        }
    }
}
