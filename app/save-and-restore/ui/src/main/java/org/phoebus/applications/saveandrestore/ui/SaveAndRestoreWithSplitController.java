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
 *
 * <a target="_blank" href="https://icons8.com/icons/set/tags">Tags icon</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>
 * <a target="_blank" href="https://icons8.com/icons/set/export">Export icon</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>
 * <a target="_blank" href="https://icons8.com/icons/set/import">Import icon</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Pair;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
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
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.ui.javafx.ImageCache;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SaveAndRestoreWithSplitController extends SaveAndRestoreController {

    private static Executor UI_EXECUTOR = Platform::runLater;

    @FXML
    private TreeView<Node> treeView;

    @FXML
    private TabPane tabPane;

    @FXML
    private Label jmasarServiceTitle;

    @FXML
    private Button reconnectButton;

    @FXML
    private Button searchButton;

    @FXML
    private Label emptyTreeInstruction;

    @FXML
    private SplitPane splitPane;

    @FXML
    private ListView<Node> listView;

    private ContextMenu folderContextMenu;
    private ContextMenu saveSetContextMenu;
    private ContextMenu snapshotContextMenu;
    private ContextMenu rootFolderContextMenu;

    private SimpleStringProperty toggleGoldenMenuItemText = new SimpleStringProperty();
    private SimpleStringProperty jmasarServiceTitleProperty = new SimpleStringProperty();
    private BooleanProperty treeViewEmpty = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<ImageView> toggleGoldenImageViewProperty = new SimpleObjectProperty<>();

    private ImageView snapshotImageView = new ImageView(snapshotIcon);
    private ImageView snapshotGoldenImageView = new ImageView(snapshotGoldenIcon);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveAndRestoreService = SaveAndRestoreService.getInstance();
        preferencesReader = new PreferencesReader(SaveAndRestoreApplication.class, "/save_and_restore_preferences.properties");

        reconnectButton.setGraphic(ImageCache.getImageView(SaveAndRestoreApplication.class, "/icons/refresh.png"));
        reconnectButton.setTooltip(new Tooltip(Messages.buttonRefresh));

        ImageView searchButtonImageView = ImageCache.getImageView(SaveAndRestoreApplication.class, "/icons/sar-search.png");
        searchButtonImageView.setFitWidth(16);
        searchButtonImageView.setFitHeight(16);

        searchButton.setGraphic(searchButtonImageView);
        searchButton.setTooltip(new Tooltip(Messages.buttonSearch));

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
            deleteNodes(treeView.getSelectionModel().getSelectedItems());
        });

        MenuItem newSaveSetMenuItem = new MenuItem(Messages.contextMenuNewSaveSet, new ImageView(saveSetIcon));
        newSaveSetMenuItem.setOnAction(ae -> {
            handleNewSaveSet(treeView.getSelectionModel().getSelectedItem());
        });

        ImageView importSaveSetIconImageView = new ImageView(csvImportIcon);
        importSaveSetIconImageView.setFitWidth(18);
        importSaveSetIconImageView.setFitHeight(18);

        MenuItem importSaveSetMenuItem = new MenuItem(Messages.importSaveSetLabel, importSaveSetIconImageView);
        importSaveSetMenuItem.setOnAction(ae -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle(Messages.importSaveSetLabel);
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported file formats (CSV, SNP)", "*.csv", "*.bms"));
                File file = fileChooser.showOpenDialog(splitPane.getScene().getWindow());
                if (file != null) {
                    CSVImporter.importFile(treeView.getSelectionModel().getSelectedItem().getValue(), file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        folderContextMenu.getItems().addAll(newFolderMenuItem, renameFolderMenuItem, deleteFolderMenuItem, newSaveSetMenuItem);
        if (preferencesReader.getBoolean("enableCSVIO")) {
            folderContextMenu.getItems().add(importSaveSetMenuItem);
        }

        rootFolderContextMenu = new ContextMenu();
        MenuItem newRootFolderMenuItem = new MenuItem(Messages.contextMenuNewFolder, new ImageView(folderIcon));
        newRootFolderMenuItem.setOnAction(ae -> {
            createNewFolder(treeView.getSelectionModel().getSelectedItem());
        });
        rootFolderContextMenu.getItems().add(newRootFolderMenuItem);

        saveSetContextMenu = new ContextMenu();

        MenuItem deleteSaveSetMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(deleteIcon));
        deleteSaveSetMenuItem.setOnAction(ae -> {
            deleteNodes(treeView.getSelectionModel().getSelectedItems());
        });

        MenuItem renameSaveSetMenuItem = new MenuItem(Messages.contextMenuRename, new ImageView(renameIcon));
        renameSaveSetMenuItem.setOnAction(ae -> {
            renameNode(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem openSaveSetMenuItem = new MenuItem(Messages.contextMenuCreateSnapshot, new ImageView(saveSetIcon));
        openSaveSetMenuItem.setOnAction(ae -> {
            openSaveSetForSnapshot(treeView.getSelectionModel().getSelectedItem());
        });

        MenuItem editSaveSetMenuItem = new MenuItem(Messages.contextMenuEdit, new ImageView(editSaveSetIcon));
        editSaveSetMenuItem.setOnAction(ae -> {
            nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
        });

        ImageView exportSaveSetIconImageView = new ImageView(csvExportIcon);
        exportSaveSetIconImageView.setFitWidth(18);
        exportSaveSetIconImageView.setFitHeight(18);

        MenuItem exportSaveSetMenuItem = new MenuItem(Messages.exportSaveSetLabel, exportSaveSetIconImageView);
        exportSaveSetMenuItem.setOnAction(ae -> {
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

                    CSVExporter.export(treeView.getSelectionModel().getSelectedItem().getValue(), file.getAbsolutePath());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ImageView importSnapshotIconImageView = new ImageView(csvImportIcon);
        importSnapshotIconImageView.setFitWidth(18);
        importSnapshotIconImageView.setFitHeight(18);

        MenuItem importSnapshotMenuItem = new MenuItem(Messages.importSnapshotLabel, importSnapshotIconImageView);
        importSnapshotMenuItem.setOnAction(ae -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle(Messages.importSnapshotLabel);
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported file formats (CSV, SNP)", "*.csv", "*.snp"));
                File file = fileChooser.showOpenDialog(splitPane.getScene().getWindow());
                if (file != null) {
                    CSVImporter.importFile(treeView.getSelectionModel().getSelectedItem().getValue(), file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        saveSetContextMenu.getItems().addAll(openSaveSetMenuItem, editSaveSetMenuItem, renameSaveSetMenuItem, deleteSaveSetMenuItem);
        if (preferencesReader.getBoolean("enableCSVIO")) {
            saveSetContextMenu.getItems().addAll(exportSaveSetMenuItem, importSnapshotMenuItem);
        }

        snapshotContextMenu = new ContextMenu();

        MenuItem deleteSnapshotMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(deleteIcon));
        deleteSnapshotMenuItem.setOnAction(ae -> {
            deleteSnapshots(listView.getSelectionModel().getSelectedItems());
        });

        MenuItem renameSnapshotItem = new MenuItem(Messages.contextMenuRename, new ImageView(renameIcon));
        renameSnapshotItem.setOnAction(ae -> {
            renameSnapshot(listView.getSelectionModel().getSelectedItem());
        });

        MenuItem compareSaveSetMenuItem = new MenuItem(Messages.contextMenuCompareSnapshots, new ImageView(compareSnapshotIcon));
        compareSaveSetMenuItem.setOnAction(ae -> {
            comapreSnapshot(listView.getSelectionModel().getSelectedItem());
        });

        MenuItem tagAsGolden = new MenuItem(Messages.contextMenuTagAsGolden, new ImageView(snapshotGoldenIcon));
        tagAsGolden.textProperty().bind(toggleGoldenMenuItemText);
        tagAsGolden.graphicProperty().bind(toggleGoldenImageViewProperty);
        tagAsGolden.setOnAction(ae -> {
            Node node = toggleGoldenProperty(listView.getSelectionModel().getSelectedItem());
            nodeChanged(node);
        });

        ImageView snapshotTagsWithCommentIconImage = new ImageView(snapshotTagsWithCommentIcon);
        snapshotTagsWithCommentIconImage.setFitHeight(22);
        snapshotTagsWithCommentIconImage.setFitWidth(22);
        Menu tagWithComment = new Menu(Messages.contextMenuTagsWithComment, snapshotTagsWithCommentIconImage);
        tagWithComment.setOnShowing(event -> {
            Node node = listView.getSelectionModel().getSelectedItem();

            ObservableList<MenuItem> tagList = tagWithComment.getItems();

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

                                }
                            }
                        });
                    });
                    tagList.add(tagItem);
                });
            }
        });

        CustomMenuItem addTagWithCommentMenuItem = TagWidget.AddTagWithCommentMenuItem();
        addTagWithCommentMenuItem.setOnAction(action -> {
            Node selectedNode = listView.getSelectionModel().getSelectedItem();
            SnapshotNewTagDialog snapshotNewTagDialog = new SnapshotNewTagDialog(selectedNode.getTags());
            snapshotNewTagDialog.initModality(Modality.APPLICATION_MODAL);

            String locationString = DirectoryUtilities.CreateLocationString(selectedNode, true);
            snapshotNewTagDialog.getDialogPane().setHeader(TagUtil.CreateAddHeader(locationString, selectedNode.getName()));

            Optional<Pair<String, String>> result = snapshotNewTagDialog.showAndWait();
            result.ifPresent(items -> {
                Tag aNewTag = Tag.builder()
                        .snapshotId(selectedNode.getUniqueId())
                        .name(items.getKey())
                        .comment(items.getValue())
                        .userName(System.getProperty("user.name"))
                        .build();

                try {
                    saveAndRestoreService.addTagToSnapshot(selectedNode, aNewTag);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to add tag to snapshot", e);
                }
            });
        });

        tagWithComment.getItems().addAll(addTagWithCommentMenuItem, new SeparatorMenuItem());

        ImageView exportSnapshotIconImageView = new ImageView(csvExportIcon);
        exportSnapshotIconImageView.setFitWidth(18);
        exportSnapshotIconImageView.setFitHeight(18);

        MenuItem exportSnapshotMenuItem = new MenuItem(Messages.exportSnapshotLabel, exportSnapshotIconImageView);
        exportSnapshotMenuItem.setOnAction(ae -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle(Messages.exportSnapshotLabel);
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (SNP compatible)", "*.csv"));
                fileChooser.setInitialFileName(listView.getSelectionModel().getSelectedItem().getName());
                File file = fileChooser.showSaveDialog(splitPane.getScene().getWindow());
                if (file != null) {
                    if (!file.getAbsolutePath().toLowerCase().endsWith("csv")) {
                        file = new File(file.getAbsolutePath() + ".csv");
                    }

                    CSVExporter.export(listView.getSelectionModel().getSelectedItem(), file.getAbsolutePath());
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to export", e);
            }
        });

        snapshotContextMenu.getItems().addAll(renameSnapshotItem, deleteSnapshotMenuItem, compareSaveSetMenuItem, tagAsGolden, tagWithComment);
        if (preferencesReader.getBoolean("enableCSVIO")) {
            snapshotContextMenu.getItems().add(exportSnapshotMenuItem);
        }

        treeView.setEditable(true);

        treeView.setOnMouseClicked(me -> {
            TreeItem<Node> item = treeView.getSelectionModel().getSelectedItem();
            if (item == null) {
                return;
            }
            if (me.getClickCount() == 2) {
                nodeDoubleClicked(item);
            }
        });

        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

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
                saveSetContextMenu, null, rootFolderContextMenu));

        treeView.getSelectionModel().selectedItemProperty().addListener((observableValue, nodeTreeItem, selectedTreeItem) -> {
            listView.getItems().clear();

            if (selectedTreeItem == null) {
                return;
            }

            Node selectedNode = selectedTreeItem.getValue();
            if (selectedNode.getNodeType() != NodeType.CONFIGURATION) {
                return;
            }

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
                nodeDoubleClicked(new TreeItem<Node>(node));
            }
        });

        loadTreeData();
    }

    private void deleteSnapshots(ObservableList<Node> selectedItems) {
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
            selectedItems.stream().forEach(item -> deleteListItem(item));
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
                UI_EXECUTOR.execute(() -> {
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
                });
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

    private void renameSnapshot(Node node) {
        List<String> existingSiblingNodes =
                listView.getItems().stream()
                        .map(item -> item.getName())
                        .collect(Collectors.toList());

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

    @Override
    protected TreeItem<Node> createNode(final Node node) {
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
                // TODO: log this?
                return;
            }
            nodeSubjectToUpdate.setValue(node);
            nodeSubjectToUpdate.getParent().getChildren().sort(new TreeNodeComparator());
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().select(nodeSubjectToUpdate);
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
    public void nodeAdded(Node parentNode, Node newNode) {
        if (newNode.getNodeType() != NodeType.SNAPSHOT) {
            // Find the parent to which the new node is to be added
            TreeItem<Node> parentTreeItem = recursiveSearch(parentNode.getUniqueId(), treeView.getRoot());
            if (parentTreeItem == null) {
                // TODO: log this?
                return;
            }
            parentTreeItem.getChildren().add(createNode(newNode));
            parentTreeItem.getChildren().sort(new TreeNodeComparator());
            parentTreeItem.expandedProperty().setValue(true);
        } else {
            if (treeView.getSelectionModel().getSelectedItem().getValue().getUniqueId().equals(parentNode.getUniqueId())) {
                listView.getItems().add(newNode);
                listView.getItems().sort(new NodeComparator());
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

        treeView.getSelectionModel().clearSelection();
        treeView.getSelectionModel().select(parentTreeItem);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    @FXML
    public void loadTreeData() {

        Task<TreeItem<Node>> loadRootNode = new Task<>() {
            @Override
            protected TreeItem<Node> call() throws Exception {
                Node rootNode = saveAndRestoreService.getRootNode();
                TreeItem<Node> rootItem = createNode(rootNode);
                List<String> savedTreeViewStructure = getSavedTreeStructure();
                // Check if there is a save tree structure. Also check that the first node id (=tree root)
                // has the same unique id as the actual root node retrieved from the remote service. This check
                // is needed to handle the case when the client connects to a different save-and-restore service.
                if (savedTreeViewStructure != null && savedTreeViewStructure.get(0).equals(rootNode.getUniqueId())) {
                    HashMap<String, List<TreeItem<Node>>> childNodesMap = new HashMap<>();
                    savedTreeViewStructure.stream().forEach(s -> {
                        List<Node> childNodes = saveAndRestoreService.getChildNodes(Node.builder().uniqueId(s).build());
                        if (childNodes != null) { // This may be the case if the tree structure was modified outside of the UI
                            List<TreeItem<Node>> childItems = childNodes.stream().map(n -> createNode(n)).collect(Collectors.toList());
                            childItems.sort(new TreeNodeComparator());
                            childNodesMap.put(s, childItems);
                        }
                    });
                    setChildItems(childNodesMap, rootItem);
                } else {
                    List<Node> childNodes = saveAndRestoreService.getChildNodes(rootItem.getValue());
                    List<TreeItem<Node>> childItems = childNodes.stream().map(n -> createNode(n)).collect(Collectors.toList());
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
}
