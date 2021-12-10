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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.NodeAddedListener;
import org.phoebus.applications.saveandrestore.data.NodeChangedListener;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVExporter;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVImporter;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotNewTagDialog;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagUtil;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main controller for the save & restore UI. In particular it handles the tree view and operations on it, e.g.
 * creating folders, save sets and launching the snapshot view.
 */
public class SaveAndRestoreController implements Initializable, NodeChangedListener, NodeAddedListener, ISaveAndRestoreController {

    @FXML
    protected TreeView<Node> treeView;

    @FXML
    protected TabPane tabPane;

    @FXML
    private Label jmasarServiceTitle;

    @FXML
    private Button reconnectButton;

    @FXML
    private Button searchButton;

    @FXML
    private Label emptyTreeInstruction;

    @FXML
    protected SplitPane splitPane;

    protected SaveAndRestoreService saveAndRestoreService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected ContextMenu folderContextMenu;
    protected ContextMenu saveSetContextMenu;
    protected ContextMenu snapshotContextMenu;
    protected ContextMenu rootFolderContextMenu;

    protected SimpleStringProperty toggleGoldenMenuItemText = new SimpleStringProperty();
    protected SimpleStringProperty jmasarServiceTitleProperty = new SimpleStringProperty();
    protected BooleanProperty treeViewEmpty = new SimpleBooleanProperty(false);
    protected SimpleObjectProperty<ImageView> toggleGoldenImageViewProperty = new SimpleObjectProperty<>();

    protected ImageView snapshotImageView = new ImageView(snapshotIcon);
    protected ImageView snapshotGoldenImageView = new ImageView(snapshotGoldenIcon);

    private static final String TREE_STATE = "tree_state";

    protected static final Logger LOG = Logger.getLogger(SaveAndRestoreService.class.getName());

    protected PreferencesReader preferencesReader;

    public static final Image folderIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/folder.png");
    public static final Image snapshotIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot.png");
    public static final Image snapshotGoldenIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-golden.png");

    protected Stage searchWindow;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        preferencesReader =
                new PreferencesReader(SaveAndRestoreApplication.class, "save_and_restore_preferences.properties");
        reconnectButton.setGraphic(ImageCache.getImageView(SaveAndRestoreApplication.class, "/icons/refresh.png"));
        reconnectButton.setTooltip(new Tooltip(Messages.buttonRefresh));

        ImageView searchButtonImageView = ImageCache.getImageView(SaveAndRestoreApplication.class, "/icons/sar-search.png");
        searchButtonImageView.setFitWidth(16);
        searchButtonImageView.setFitHeight(16);

        searchButton.setGraphic(searchButtonImageView);
        searchButton.setTooltip(new Tooltip(Messages.buttonSearch));

        emptyTreeInstruction.textProperty().setValue(Messages.labelCreateFolderEmptyTree);

        folderContextMenu = new ContextMenuFolder(this, preferencesReader.getBoolean("enableCSVIO"));
        saveSetContextMenu = new ContextMenuSaveSet(this, preferencesReader.getBoolean("enableCSVIO"));

        rootFolderContextMenu = new ContextMenu();
        MenuItem newRootFolderMenuItem = new MenuItem(Messages.contextMenuNewFolder, new ImageView(folderIcon));
        newRootFolderMenuItem.setOnAction(ae -> createNewFolder());
        rootFolderContextMenu.getItems().add(newRootFolderMenuItem);

        snapshotContextMenu = new ContextMenuSnapshot(this, preferencesReader.getBoolean("enableCSVIO"),
                toggleGoldenMenuItemText, toggleGoldenImageViewProperty);

        treeView.setEditable(true);

        treeView.setOnMouseClicked(me -> {
            TreeItem<Node> item = treeView.getSelectionModel().getSelectedItem();
            if (item == null) {
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

        jmasarServiceTitle.textProperty().bind(jmasarServiceTitleProperty);

        ContextMenu treeViewContextMenu = new ContextMenu();
        MenuItem newTopLevelFolderMenuItem = new MenuItem(Messages.contextMenuNewTopLevelFolder, new ImageView(folderIcon));
        newTopLevelFolderMenuItem.setOnAction(ae -> createNewFolder(treeView.getRoot()));

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    @FXML
    public void loadTreeData() {

        Task<TreeItem<Node>> loadRootNode = new Task<>() {
            @Override
            protected TreeItem<Node> call() {
                Node rootNode = saveAndRestoreService.getRootNode();
                TreeItem<Node> rootItem = createTreeItem(rootNode);
                List<String> savedTreeViewStructure = getSavedTreeStructure();
                // Check if there is a save tree structure. Also check that the first node id (=tree root)
                // has the same unique id as the actual root node retrieved from the remote service. This check
                // is needed to handle the case when the client connects to a different save-and-restore service.
                if (savedTreeViewStructure != null && savedTreeViewStructure.get(0).equals(rootNode.getUniqueId())) {
                    HashMap<String, List<TreeItem<Node>>> childNodesMap = new HashMap<>();
                    savedTreeViewStructure.forEach(s -> {
                        List<Node> childNodes = saveAndRestoreService.getChildNodes(Node.builder().uniqueId(s).build());
                        if (childNodes != null) { // This may be the case if the tree structure was modified outside of the UI
                            List<TreeItem<Node>> childItems = childNodes.stream().map(n -> createTreeItem(n)).collect(Collectors.toList());
                            childItems.sort(new TreeNodeComparator());
                            childNodesMap.put(s, childItems);
                        }
                    });
                    setChildItems(childNodesMap, rootItem);
                } else {
                    List<Node> childNodes = saveAndRestoreService.getChildNodes(rootItem.getValue());
                    List<TreeItem<Node>> childItems = childNodes.stream().map(n -> createTreeItem(n)).collect(Collectors.toList());
                    treeViewEmpty.setValue(childItems.isEmpty());
                    childItems.sort(new TreeNodeComparator());
                    rootItem.getChildren().addAll(childItems);
                }

                return rootItem;
            }

            @Override
            public void succeeded() {
                TreeItem<Node> rootItem = getValue();
                jmasarServiceTitleProperty.set(saveAndRestoreService.getServiceIdentifier());

                treeView.setRoot(rootItem);
                restoreTreeState();
            }

            @Override
            public void failed() {
                jmasarServiceTitleProperty.set(MessageFormat.format(Messages.jmasarServiceUnavailable, saveAndRestoreService.getServiceIdentifier()));
            }
        };

        new Thread(loadRootNode).start();
    }

    private List<String> getSavedTreeStructure() {
        String savedTreeState = PhoebusPreferenceService.userNodeForClass(SaveAndRestoreApplication.class).get(TREE_STATE, null);
        if (savedTreeState == null) {
            return null;
        }
        try {
            return objectMapper.readValue(savedTreeState, new TypeReference<>() {
            });
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Unable to obtain tree node data from service", e);
            return null;
        }
    }

    /**
     * Expands the specified node by clearing its list of child nodes and then fetching the
     * list of child nodes from the service. This is typically applied if one wishes to
     * refresh a node as a consequence of structural changes (e.g. node deleted).
     *
     * @param targetItem {@link TreeItem<Node>} on which the operation is performed.
     */
    private void expandTreeNode(TreeItem<Node> targetItem) {

        targetItem.getChildren().clear();

        List<Node> childNodes = saveAndRestoreService.getChildNodes(targetItem.getValue());
        Collections.sort(childNodes);
        targetItem.getChildren().addAll(childNodes.stream().map(n -> createTreeItem(n)).collect(Collectors.toList()));
        targetItem.getChildren().sort(new TreeNodeComparator());
    }

    /**
     * Deletion of tree nodes when multiple nodes are selected is allowed only if all of the
     * selected nodes have the same parent node. This method checks the parent node(s) of
     * the selected nodes accordingly.
     *
     * @param selectedItems The selected tree nodes.
     * @return <code>true</code> if all selected nodes have the same parent node, <code>false</code> otherwise.
     */
    private boolean isDeletionPossible(ObservableList<TreeItem<Node>> selectedItems) {
        Node parentNode = selectedItems.get(0).getParent().getValue();
        for (TreeItem<Node> treeItem : selectedItems) {
            if (!treeItem.getParent().getValue().getUniqueId().equals(parentNode.getUniqueId())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Action when user requests comparison between an opened snapshot and a snapshot item selected from
     * the tree view.
     */
    protected void comapreSnapshot() {
        compareSnapshot(treeView.getSelectionModel().getSelectedItem().getValue());
    }

    /**
     * Action when user requests comparison between an opened snapshot and the specifies snapshot {@link Node}
     * @param node The snapshot used in the comparison.
     */
    protected void compareSnapshot(Node node) {
        try {
            SnapshotTab currentTab = (SnapshotTab) tabPane.getSelectionModel().getSelectedItem();
            if (currentTab == null) {
                return;
            }
            currentTab.addSnapshot(node);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to compare snapshot", e);
        }
    }

    /**
     * Toggles the "golden" property of a snapshot as selected from the tree view.
     */
    protected void toggleGoldenProperty() {
        toggleGoldenProperty(treeView.getSelectionModel().getSelectedItem().getValue());
    }

    /**
     * Toggles the "golden" property of the specified snapshot {@link Node}
     * @param node The snapshot {@link Node} on which to toggle the "golden" property.
     */
    private void toggleGoldenProperty(Node node) {
        try {
            Node updatedNode = saveAndRestoreService.tagSnapshotAsGolden(node,
                    !Boolean.parseBoolean(node.getProperty("golden")));
            treeView.getSelectionModel().getSelectedItem().setValue(updatedNode);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to toggle golden property", e);
        }
    }

    /**
     * Deletes selected snapshots.
     */
    protected void deleteSnapshots(){
        deleteNodes(treeView.getSelectionModel().getSelectedItems());
    }

    /**
     * Deletes the tree items selected in the tree view.
     */
    protected void deleteNodes() {
        deleteNodes(treeView.getSelectionModel().getSelectedItems());
    }

    /**
     * Deletes the specified tree nodes from the tree view.
     * @param selectedItems List of nodes to delete.
     */
    private void deleteNodes(ObservableList<TreeItem<Node>> selectedItems) {
        if (!isDeletionPossible(selectedItems)) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(Messages.promptDeleteSelectedTitle);
            alert.setHeaderText(Messages.deletionNotAllowedHeader);
            alert.setContentText(Messages.deletionNotAllowed);
            alert.showAndWait();
            return;
        }

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
            selectedItems.forEach(this::deleteTreeItem);
        }
    }

    /**
     * Deletes the {@link Node} associated with the {@link TreeItem}, and
     * removes the {@link TreeItem} from the tree view. If {@link Node} is associated
     * with an open tab, that tab is cleaned up and closed.
     *
     * @param treeItem The item to be deleted from the tree view
     */
    private void deleteTreeItem(TreeItem<Node> treeItem) {
        TreeItem<Node> parent = treeItem.getParent();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                saveAndRestoreService.deleteNode(treeItem.getValue().getUniqueId());
                return null;
            }

            @Override
            public void succeeded() {
                parent.getChildren().remove(treeItem);
                List<Tab> tabsToRemove = new ArrayList<>();
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getId().equals(treeItem.getValue().getUniqueId())) {
                        tabsToRemove.add(tab);
                        tab.getOnCloseRequest().handle(null);
                    }
                }
                tabPane.getTabs().removeAll(tabsToRemove);
                treeView.getSelectionModel().clearSelection();
            }

            @Override
            public void failed() {
                expandTreeNode(treeItem.getParent());
                ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                        MessageFormat.format(Messages.errorDeleteNodeFailed, treeItem.getValue().getName()), null);
            }
        };

        new Thread(task).start();
    }

    /**
     * Opens a new snapshot view tab associated with the selected save set.
     */
    protected void openSaveSetForSnapshot() {
        TreeItem<Node> treeItem = treeView.getSelectionModel().getSelectedItem();
        SnapshotTab tab = new SnapshotTab(treeItem.getValue(), saveAndRestoreService);
        tab.loadSaveSet(treeItem.getValue());

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    /**
     * Creates a new folder {@link Node}.
     */
    protected void createNewFolder() {
        createNewFolder(treeView.getSelectionModel().getSelectedItem());
    }

    /**
     * Creates a new folder {@link Node} in the specified parent.
     * @param parentTreeItem The tree node to which a new folder is added.
     */
    protected void createNewFolder(TreeItem<Node> parentTreeItem) {
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
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    saveAndRestoreService
                            .createNode(parentTreeItem.getValue().getUniqueId(), newFolderNode);
                    return null;
                }

                @Override
                public void failed() {
                    expandTreeNode(parentTreeItem.getParent());
                    ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                            Messages.errorCreateFolderFailed, null);
                }
            };

            new Thread(task).start();
        }
    }

    /**
     * Handles double click on a tree node.
     */
    public void nodeDoubleClicked() {
        nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
    }

    /**
     * Handles double click on the specified tree node. Actual action depends on the {@link Node} type.
     * @param node The double click source
     */
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

    /**
     * Creates a new save set in the selected tree node.
     */
    protected void createNewSaveSet() {

        TreeItem<Node> parentTreeItem = treeView.getSelectionModel().getSelectedItem();
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
            Task<Node> task = new Task<>() {
                @Override
                protected Node call() throws Exception {
                    return saveAndRestoreService
                            .createNode(treeView.getSelectionModel().getSelectedItem().getValue().getUniqueId(), newSateSetNode);
                }

                @Override
                public void succeeded() {
                    TreeItem<Node> newSaveSetNode;
                    try {
                        newSaveSetNode = createTreeItem(get());
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Encountered error when creating save set", e);
                        return;
                    }
                    nodeDoubleClicked(newSaveSetNode);
                    treeView.getSelectionModel().clearSelection();
                    treeView.getSelectionModel().select(treeView.getRow(newSaveSetNode));
                }

                @Override
                public void failed() {
                    expandTreeNode(parentTreeItem.getParent());
                    ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                            Messages.errorCreateSaveSetFailed, null);
                }
            };

            new Thread(task).start();
        }
    }


    /**
     * Renames a node through the service and its underlying data provider.
     * If there is a problem in the call to the remote JMasar service,
     * the user is shown a suitable error dialog and the name of the node is restored.
     */
    protected void renameNode() {
        TreeItem<Node> node = treeView.getSelectionModel().getSelectedItem();
        List<String> existingSiblingNodes =
                node.getParent().getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(node.getValue().getNodeType()))
                        .map(item -> item.getValue().getName())
                        .collect(Collectors.toList());
        renameNode(node.getValue(), existingSiblingNodes);
    }

    /**
     * Renames the selected node. A check is made to ensure that user cannot specify a name
     * that is the same as any of its sibling nodes if they are of the same {@link Node} type.
     * @param node The node to rename
     * @param existingSiblingNodes List of sibling nodes
     */
    public void renameNode(Node node, List<String> existingSiblingNodes) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Messages.promptRenameNodeTitle);
        dialog.setContentText(Messages.promptRenameNodeContent);
        dialog.setHeaderText(null);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        dialog.getEditor().textProperty().setValue(node.getName());


        dialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            String value = newValue.trim();
            dialog.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(existingSiblingNodes.contains(value) || value.isEmpty());
        });

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            node.setName(result.get());
            try {
                saveAndRestoreService.updateNode(node);
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle(Messages.errorActionFailed);
                alert.setHeaderText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Utility method to create a {@link TreeItem<Node>} object.
     * @param node The {@link Node} object for the {@link TreeItem}.
     * @return The new {@link TreeItem}.
     */
    protected TreeItem<Node> createTreeItem(final Node node) {
        return new TreeItem<>(node) {
            @Override
            public boolean isLeaf() {
                return node.getNodeType().equals(NodeType.SNAPSHOT);
            }
        };
    }

    /**
     * Handles callback in order to update the tree view when a {@link Node} has changed.
     * @param node The updated node.
     */
    @Override
    public void nodeChanged(Node node) {
        // Find the node that has changed
        TreeItem<Node> nodeSubjectToUpdate = recursiveSearch(node.getUniqueId(), treeView.getRoot());
        if (nodeSubjectToUpdate == null) {
            // TODO: log this?
            return;
        }
        nodeSubjectToUpdate.setValue(node);
        nodeSubjectToUpdate.getParent().getChildren().sort(new TreeNodeComparator());
        treeView.getSelectionModel().clearSelection();
        treeView.getSelectionModel().select(nodeSubjectToUpdate);
    }

    /**
     * Handles callback in order to update the tree view when a {@link Node} has been added, e.g. when
     * a snapshot is saved.
     * @param parentNode Parent of the new {@link Node}
     * @param newNode The new {@link Node}
     */
    @Override
    public void nodeAdded(Node parentNode, Node newNode) {
        // Find the parent to which the new node is to be added
        TreeItem<Node> parentTreeItem = recursiveSearch(parentNode.getUniqueId(), treeView.getRoot());
        if (parentTreeItem == null) {
            return;
        }
        parentTreeItem.getChildren().add(createTreeItem(newNode));
        parentTreeItem.getChildren().sort(new TreeNodeComparator());
        parentTreeItem.expandedProperty().setValue(true);
    }

    /**
     * Utility method used to find the {@link TreeItem} corresponding to the specified {@link Node}'s id.
     * @param nodeIdToLocate The unique string identifying the {@link Node}
     * @param node The {@link TreeItem} from which to start the search
     * @return The located {@link TreeItem}, or <code>null</code> if not found.
     */
    protected TreeItem<Node> recursiveSearch(String nodeIdToLocate, TreeItem<Node> node) {
        if (node.getValue().getUniqueId().equals(nodeIdToLocate))
            return node;
        List<TreeItem<Node>> childNodes = node.getChildren();
        TreeItem<Node> result = null;
        for (int i = 0; result == null && i < childNodes.size(); i++) {
            result = recursiveSearch(nodeIdToLocate, childNodes.get(i));
        }
        return result;
    }

    /**
     * Saves the tree state.
     * @param memento The {@link Memento} in which to save the state.
     */
    @Override
    public void save(final Memento memento) {
        saveTreeState();
        memento.setNumber("POS", splitPane.getDividers().get(0).getPosition());
    }

    /**
     * Restores the divider position from {@link Memento}, if found.
     * @param memento The persisted (or empty) {@link Memento}.
     */
    @Override
    public void restore(final Memento memento) {
        memento.getNumber("POS").ifPresent(pos -> splitPane.setDividerPositions(pos.doubleValue()));
    }

    @Override
    public void locateNode(Stack<Node> nodeStack) {
        TreeItem<Node> parentTreeItem = treeView.getRoot();

        while (nodeStack.size() > 0) {
            Node currentNode = nodeStack.pop();
            TreeItem<Node> currentTreeItem = recursiveSearch(currentNode.getUniqueId(), parentTreeItem);
            currentTreeItem.setExpanded(true);
            parentTreeItem = currentTreeItem;
        }

        treeView.getSelectionModel().clearSelection();
        treeView.getSelectionModel().select(parentTreeItem);
        treeView.scrollTo(treeView.getSelectionModel().getSelectedIndex());
    }

    /**
     * Persists the tree view state
     */
    private void saveTreeState() {
        if (treeView.getRoot() == null) {
            return;
        }
        List<String> expandedNodes = new ArrayList<>();
        findExpandedNodes(expandedNodes, treeView.getRoot());
        if (expandedNodes.isEmpty()) {
            return;
        }
        try {
            PhoebusPreferenceService.userNodeForClass(SaveAndRestoreApplication.class).put(TREE_STATE, objectMapper.writeValueAsString(expandedNodes));
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, "Failed to persist tree state");
        }
    }

    private void findExpandedNodes(List<String> expandedNodes, TreeItem<Node> treeItem) {
        if (treeItem.expandedProperty().get() && !treeItem.getChildren().isEmpty()) {
            expandedNodes.add(treeItem.getValue().getUniqueId());
            treeItem.getChildren().forEach(ti -> findExpandedNodes(expandedNodes, ti));
        }
    }

    /**
     * Loops through the the tree view model and expands all nodes that have a non-empty children
     * list. The tree view at this point has already been updated with data from the backend.
     */
    protected void restoreTreeState() {
        expandNodes(treeView.getRoot());

        // Must be added here, after nodes have been expanded. Adding the event handler
        // before expansion of nodes will break the expected behavior when restoring the tree state.
        treeView.getRoot().addEventHandler(TreeItem.<Node>branchExpandedEvent(), e -> expandTreeNode(e.getTreeItem()));

        // This is needed in the rare event that all top level folders have been deleted.
        treeView.getRoot().addEventHandler(TreeItem.treeNotificationEvent(),
                e -> treeViewEmpty.set(treeView.getRoot().getChildren().isEmpty()));
    }

    private void setChildItems(HashMap<String, List<TreeItem<Node>>> allItems, TreeItem<Node> parentItem) {
        if (allItems.containsKey(parentItem.getValue().getUniqueId())) {
            List<TreeItem<Node>> childItems = allItems.get(parentItem.getValue().getUniqueId());
            parentItem.getChildren().setAll(childItems);
            parentItem.getChildren().sort(new TreeNodeComparator());
            childItems.stream().forEach(ci -> setChildItems(allItems, ci));
        }
    }

    /**
     * Expands all nodes recursively starting from the specified node. Note that this
     * method operates only on the items already present in the {@link TreeView}, i.e.
     * no data is retrieved from the service.
     *
     * @param parentNode {@link TreeItem<Node>} from which to start the operation.
     */
    protected void expandNodes(TreeItem<Node> parentNode) {
        if (!parentNode.getChildren().isEmpty()) {
            parentNode.setExpanded(true);
            parentNode.getChildren().forEach(this::expandNodes);
        }
    }

    /**
     * Utility class for the purpose of sorting {@link TreeItem}s. For snapshot {@link Node}s the created date
     * is used for comparison, while folder and save set {@link Node}s are compared by name.
     * See {@link Node#compareTo(Node)}.
     */
    protected class TreeNodeComparator implements Comparator<TreeItem<Node>> {
        @Override
        public int compare(TreeItem<Node> t1, TreeItem<Node> t2) {
            if (t1.getValue().getNodeType().equals(NodeType.SNAPSHOT) && t2.getValue().getNodeType().equals(NodeType.SNAPSHOT)) {
                return (preferencesReader.getBoolean("sortSnapshotsTimeReversed") ? -1 : 1) * t1.getValue().getCreated().compareTo(t2.getValue().getCreated());
            }

            return t1.getValue().compareTo(t2.getValue());
        }
    }

    /**
     * Tag comparator using the tags' created date.
     */
    protected static class TagComparator implements Comparator<Tag> {
        @Override
        public int compare(Tag tag1, Tag tag2) {
            return -tag1.getCreated().compareTo(tag2.getCreated());
        }
    }

    /**
     * Self explanatory
     */
    @FXML
    protected void openSearchWindow() {
        try {
            if (searchWindow == null) {
                final ResourceBundle bundle = NLS.getMessages(SaveAndRestoreApplication.class);

                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(SaveAndRestoreController.class.getResource("SearchWindow.fxml"));
                loader.setResources(bundle);
                searchWindow = new Stage();
                searchWindow.getIcons().add(ImageCache.getImage(ImageCache.class, "/icons/logo.png"));
                searchWindow.setTitle(Messages.searchWindowLabel);
                searchWindow.initModality(Modality.WINDOW_MODAL);
                searchWindow.setScene(new Scene(loader.load()));
                ((SearchController) loader.getController()).setCallerController(this);
                searchWindow.setOnCloseRequest(action -> searchWindow = null);
                searchWindow.show();
            } else {
                searchWindow.requestFocus();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load fxml for search window", e);
        }
    }

    /**
     * Self explanatory
     */
    public void closeTagSearchWindow() {
        if (searchWindow != null) {
            searchWindow.close();
        }
    }

    /**
     * Imports a save set to a folder {@link Node} from file.
     */
    protected void importSaveSet() {
        Node node = treeView.getSelectionModel().getSelectedItem().getValue();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Messages.importSaveSetLabel);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported file formats (CSV, SNP)", "*.csv", "*.bms"));
            File file = fileChooser.showOpenDialog(splitPane.getScene().getWindow());
            if (file != null) {
                CSVImporter.importFile(node, file);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "CSV import failed", e);
        }
    }

    /**
     * Exports a save set to file.
     */
    protected void exportSaveSet() {
        Node node = treeView.getSelectionModel().getSelectedItem().getValue();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Messages.exportSaveSetLabel);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (BMS compatible)", "*.csv"));
            fileChooser.setInitialFileName(treeView.getSelectionModel().getSelectedItem().getValue().getName());
            File file = fileChooser.showSaveDialog(splitPane.getScene().getWindow());
            if (file != null) {
                if (!file.getAbsolutePath().toLowerCase().endsWith("csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }

                CSVExporter.export(node, file.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Save set export failed", e);
        }
    }

    /**
     * Imports a snapshot from file to a save set. Contets must match the PV list of the selected save set.
     */
    protected void importSnapshot() {
        Node node = treeView.getSelectionModel().getSelectedItem().getValue();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Messages.importSnapshotLabel);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported file formats (CSV, SNP)", "*.csv", "*.snp"));
            File file = fileChooser.showOpenDialog(splitPane.getScene().getWindow());
            if (file != null) {
                CSVImporter.importFile(node, file);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Snapshot import failed", e);
        }
    }

    /**
     * Exports a snapshot to file from selected snapshot node.
     */
    protected void exportSnapshot() {
        exportSnapshot(treeView.getSelectionModel().getSelectedItem().getValue());
    }

    /**
     * Exports the specified snapshot node
     * @param node The snapshot {@link Node} to export.
     */
    public void exportSnapshot(Node node) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Messages.exportSnapshotLabel);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (SNP compatible)", "*.csv"));
            fileChooser.setInitialFileName(treeView.getSelectionModel().getSelectedItem().getValue().getName());
            File file = fileChooser.showSaveDialog(splitPane.getScene().getWindow());
            if (file != null) {
                if (!file.getAbsolutePath().toLowerCase().endsWith("csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }
                CSVExporter.export(node, file.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to export snapshot", e);
        }
    }

    /**
     * Adds a tag to the selected snapshot {@link Node}
     */
    protected void addTagToSnapshot() {
        addTagToSnapshot(treeView.getSelectionModel().getSelectedItem().getValue());
    }

    /**
     * Adds a tag to the specified snapshot {@link Node}
     * @param node The snapshot to which tag is added.
     */
    public void addTagToSnapshot(Node node) {
        SnapshotNewTagDialog snapshotNewTagDialog = new SnapshotNewTagDialog(node.getTags());
        snapshotNewTagDialog.initModality(Modality.APPLICATION_MODAL);

        String locationString = DirectoryUtilities.CreateLocationString(node, true);
        snapshotNewTagDialog.getDialogPane().setHeader(TagUtil.CreateAddHeader(locationString, node.getName()));

        Optional<Pair<String, String>> result = snapshotNewTagDialog.showAndWait();
        result.ifPresent(items -> {
            Tag aNewTag = Tag.builder()
                    .snapshotId(node.getUniqueId())
                    .name(items.getKey())
                    .comment(items.getValue())
                    .userName(System.getProperty("user.name"))
                    .build();

            try {
                saveAndRestoreService.addTagToSnapshot(node, aNewTag);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to add tag to snapshot");
            }
        });
    }

    /**
     * Adds a tag with comment to the selected snapshot node.
     * @param tagList A list of existing tags, if any.
     */
    protected void tagWithComment(ObservableList<MenuItem> tagList) {
        Node node = treeView.getSelectionModel().getSelectedItem().getValue();
        tagWithComment(node, tagList);
    }

    /**
     * Adds a tag with comment to the specified snapshot node.
     * @param node The {@link Node} to enrich with a commented tag.
     * @param tagList List of existing tags, if any.
     */
    public void tagWithComment(Node node, ObservableList<MenuItem> tagList) {
        while (tagList.size() > 2) {
            tagList.remove(tagList.size() - 1);
        }

        if (node.getTags().isEmpty()) {
            CustomMenuItem noTags = TagWidget.NoTagMenuItem();
            noTags.setDisable(true);
            tagList.add(noTags);
        } else {
            node.getTags().sort(new TagComparator());
            node.getTags().stream().forEach(tag -> {
                CustomMenuItem tagItem = TagWidget.TagWithCommentMenuItem(tag);

                tagItem.setOnAction(actionEvent -> {
                    Alert confirmation = new Alert(AlertType.CONFIRMATION);
                    confirmation.setTitle(Messages.tagRemoveConfirmationTitle);
                    String locationString = DirectoryUtilities.CreateLocationString(node, true);
                    javafx.scene.Node headerNode = TagUtil.CreateRemoveHeader(locationString, node.getName(), tag);
                    confirmation.getDialogPane().setHeader(headerNode);
                    confirmation.setContentText(Messages.tagRemoveConfirmationContent);

                    Optional<ButtonType> result = confirmation.showAndWait();
                    result.ifPresent(buttonType -> {
                        if (buttonType == ButtonType.OK) {
                            try {
                                saveAndRestoreService.removeTagFromSnapshot(node, tag);
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "Failed to remove tag from snapshot", e);
                            }
                        }
                    });
                });
                tagList.add(tagItem);
            });
        }
    }
}
