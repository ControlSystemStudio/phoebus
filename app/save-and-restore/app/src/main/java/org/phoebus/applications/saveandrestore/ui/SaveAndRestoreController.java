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
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Callback;
import javafx.util.Pair;
import javafx.util.StringConverter;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVExporter;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVImporter;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.ui.configuration.ConfigurationTab;
import org.phoebus.applications.saveandrestore.ui.search.SearchAndFilterTab;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;
import org.phoebus.applications.saveandrestore.ui.snapshot.CompositeSnapshotTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotNewTagDialog;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagUtil;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.framework.autocomplete.ProposalService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.autocomplete.AutocompleteMenu;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main controller for the save & restore UI.
 */
public class SaveAndRestoreController implements Initializable, NodeChangedListener, NodeAddedListener {

    @FXML
    protected TreeView<Node> treeView;

    @FXML
    protected TabPane tabPane;

    @FXML
    protected SplitPane splitPane;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private ComboBox<Filter> filtersComboBox;

    @FXML
    private Button searchButton;

    @FXML
    private CheckBox enableFilterCheckBox;

    protected SaveAndRestoreService saveAndRestoreService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected ContextMenu folderContextMenu;
    protected ContextMenu configurationContextMenu;
    protected ContextMenu snapshotContextMenu;
    protected ContextMenu rootFolderContextMenu;
    protected ContextMenu compositeSnapshotContextMenu;

    protected SimpleStringProperty toggleGoldenMenuItemText = new SimpleStringProperty();
    protected SimpleObjectProperty<ImageView> toggleGoldenImageViewProperty = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty multipleItemsSelected = new SimpleBooleanProperty(false);
    protected MultipleSelectionModel<TreeItem<Node>> browserSelectionModel;

    protected ImageView snapshotImageView = new ImageView(ImageRepository.SNAPSHOT);
    protected ImageView snapshotGoldenImageView = new ImageView(ImageRepository.GOLDEN_SNAPSHOT);

    private static final String TREE_STATE = "tree_state";

    private static final String FILTER_NAME = "filter_name";

    protected static final Logger LOG = Logger.getLogger(SaveAndRestoreService.class.getName());

    protected Comparator<TreeItem<Node>> treeNodeComparator;

    protected SimpleBooleanProperty disabledUi = new SimpleBooleanProperty(false);

    private SimpleBooleanProperty filterEnabledProperty = new SimpleBooleanProperty(false);

    private final URI uri;

    @FXML
    private Tooltip filterToolTip;

    @SuppressWarnings("unused")
    @FXML
    private VBox errorPane;

    private final ObservableList<Node> searchResultNodes = FXCollections.observableArrayList();

    private final ObservableList<Filter> filtersList = FXCollections.observableArrayList();

    /**
     * @param uri If non-null, this is used to load a configuration or snapshot into the view.
     */
    public SaveAndRestoreController(URI uri) {
        this.uri = uri;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Tree items are first compared on type, then on name (case insensitive).
        treeNodeComparator = Comparator.comparing(TreeItem::getValue);

        saveAndRestoreService = SaveAndRestoreService.getInstance();
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        browserSelectionModel = treeView.getSelectionModel();
        browserSelectionModel.selectedItemProperty().addListener(new ChangeListener<TreeItem<Node>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<Node>> observable, TreeItem<Node> oldValue, TreeItem<Node> newValue) {
                if (newValue != null) {

                }
            }
        });

        ImageView searchButtonImageView = ImageCache.getImageView(SaveAndRestoreApplication.class, "/icons/sar-search.png");
        searchButtonImageView.setFitWidth(20);
        searchButtonImageView.setFitHeight(20);
        searchButton.setGraphic(searchButtonImageView);

        enableFilterCheckBox.selectedProperty().bindBidirectional(filterEnabledProperty);
        filtersComboBox.disableProperty().bind(filterEnabledProperty.not());
        filterEnabledProperty.addListener((observable, oldValue, newValue) -> {
            filterEnabledChanged(newValue);
        });


        folderContextMenu = new ContextMenuFolder(this, Preferences.enableCSVIO, multipleItemsSelected);
        folderContextMenu.setOnShowing(event -> multipleItemsSelected.set(browserSelectionModel.getSelectedItems().size() > 1));
        configurationContextMenu = new ContextMenuConfiguration(this, Preferences.enableCSVIO, multipleItemsSelected);
        configurationContextMenu.setOnShowing(event -> multipleItemsSelected.set(browserSelectionModel.getSelectedItems().size() > 1));

        rootFolderContextMenu = new ContextMenu();
        MenuItem newRootFolderMenuItem = new MenuItem(Messages.contextMenuNewFolder, new ImageView(ImageRepository.FOLDER));
        newRootFolderMenuItem.setOnAction(ae -> createNewFolder());
        rootFolderContextMenu.getItems().add(newRootFolderMenuItem);

        snapshotContextMenu = new ContextMenuSnapshot(this, Preferences.enableCSVIO,
                toggleGoldenMenuItemText, toggleGoldenImageViewProperty, multipleItemsSelected);

        compositeSnapshotContextMenu = new ContextMenuCompositeSnapshot(this, multipleItemsSelected);

        treeView.setEditable(true);

        treeView.setOnMouseClicked(me -> {
            TreeItem<Node> item = browserSelectionModel.getSelectedItem();
            if (item == null) {
                return;
            }
            if (item.getValue().getNodeType().equals(NodeType.SNAPSHOT)) {
                toggleGoldenMenuItemText.set(item.getValue().hasTag(Tag.GOLDEN) ? Messages.contextMenuRemoveGoldenTag : Messages.contextMenuTagAsGolden);
                toggleGoldenImageViewProperty.set(item.getValue().hasTag(Tag.GOLDEN) ? snapshotImageView : snapshotGoldenImageView);
            }
            // Check if a tab has already been opened for this node.
            boolean highlighted = highlightTab(item.getValue().getUniqueId());
            if (!highlighted && me.getClickCount() == 2) {
                nodeDoubleClicked(item.getValue());
            }
        });

        treeView.setShowRoot(true);

        saveAndRestoreService.addNodeChangeListener(this);
        saveAndRestoreService.addNodeAddedListener(this);

        treeView.setCellFactory(p -> new BrowserTreeCell(folderContextMenu,
                configurationContextMenu, snapshotContextMenu, rootFolderContextMenu, compositeSnapshotContextMenu,
                this));

        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> treeView.setDisable(newValue));

        filtersComboBox.setCellFactory(new Callback<>() {
            @Override
            public ListCell<org.phoebus.applications.saveandrestore.model.search.Filter> call(ListView<org.phoebus.applications.saveandrestore.model.search.Filter> param) {
                return new ListCell<>() {
                    @Override
                    public void updateItem(org.phoebus.applications.saveandrestore.model.search.Filter item,
                                           boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty && item != null) {
                            setText(item.getName());
                        }
                    }
                };
            }
        });

        filtersComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Filter filter) {
                        if (filter == null) {
                            return "";
                        } else {
                            return filter.getName();
                        }
                    }

                    @Override
                    public Filter fromString(String s) {
                        return null;
                    }
                });

        filtersComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                filterToolTip.textProperty().set(newValue.getQueryString());
                if (!newValue.equals(oldValue)) {
                    applyFilter(newValue);
                }
            }
        });

        filtersComboBox.itemsProperty().bind(new SimpleObjectProperty<>(filtersList));

        enableFilterCheckBox.disableProperty().bind(Bindings.createBooleanBinding(() -> filtersList.isEmpty(), filtersList));

        loadTreeData();
    }

    /**
     * Loads the data for the tree root as provided (persisted) by the current
     * {@link org.phoebus.applications.saveandrestore.SaveAndRestoreClient}.
     */
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
                            List<TreeItem<Node>> childItems = childNodes.stream().map(n -> createTreeItem(n)).sorted(treeNodeComparator).collect(Collectors.toList());
                            childNodesMap.put(s, childItems);
                        }
                    });
                    setChildItems(childNodesMap, rootItem);
                } else {
                    List<Node> childNodes = saveAndRestoreService.getChildNodes(rootItem.getValue());
                    List<TreeItem<Node>> childItems = childNodes.stream().map(n -> createTreeItem(n)).sorted(treeNodeComparator).collect(Collectors.toList());
                    rootItem.getChildren().addAll(childItems);
                }

                return rootItem;
            }

            /**
             * Performs additional configuration/initialization when data has been loaded from
             * the service.
             */
            @Override
            public void succeeded() {
                TreeItem<Node> rootItem = getValue();
                treeView.setRoot(rootItem);
                expandNodes(treeView.getRoot());
                // Open a resource (e.g. a snapshot node) if one is specified.
                openResource(uri);
                // Event handler for expanding nodes
                treeView.getRoot().addEventHandler(TreeItem.<Node>branchExpandedEvent(), e -> expandTreeNode(e.getTreeItem()));
                // Load all filters from service
                loadFilters();
                // Get saved filter and apply it if non-null, otherwise select "no filter"
                String savedFilterName = getSavedFilterName();
                if (savedFilterName != null) {
                    Optional<Filter> f = filtersComboBox.getItems().stream().filter(filter -> filter.getName().equals(savedFilterName)).findFirst();
                    if (f.isPresent()) {
                        filtersComboBox.getSelectionModel().select(f.get());
                    }
                }
            }

            @Override
            public void failed() {
                errorPane.visibleProperty().set(true);
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
            LOG.log(Level.WARNING, "Unable to parse saved tree state", e);
            return null;
        }
    }

    private String getSavedFilterName() {
        String savedFilterName = PhoebusPreferenceService.userNodeForClass(SaveAndRestoreApplication.class).get(FILTER_NAME, null);
        if (savedFilterName == null) {
            return null;
        }
        try {
            return objectMapper.readValue(savedFilterName, String.class);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Unable to parse saved filter name", e);
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
    protected void expandTreeNode(TreeItem<Node> targetItem) {
        targetItem.getChildren().clear();
        List<Node> childNodes = saveAndRestoreService.getChildNodes(targetItem.getValue());
        Collections.sort(childNodes);
        targetItem.getChildren().addAll(childNodes.stream().map(this::createTreeItem).collect(Collectors.toList()));
        targetItem.getChildren().sort(treeNodeComparator);
        targetItem.setExpanded(true);
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
        compareSnapshot(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    /**
     * Action when user requests comparison between an opened snapshot and the specifies snapshot {@link Node}
     *
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
        toggleGoldenProperty(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    /**
     * Toggles the "golden" property of the specified snapshot {@link Node}
     *
     * @param node The snapshot {@link Node} on which to toggle the "golden" property.
     */
    protected void toggleGoldenProperty(Node node) {
        try {
            Node updatedNode = saveAndRestoreService.tagSnapshotAsGolden(node,
                    !node.hasTag(Tag.GOLDEN));
            browserSelectionModel.getSelectedItems().get(0).setValue(updatedNode);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to toggle golden property", e);
        }
    }

    /**
     * Deletes selected snapshots.
     */
    protected void deleteSnapshots() {
        deleteNodes(browserSelectionModel.getSelectedItems());
    }

    /**
     * Deletes the tree items selected in the tree view.
     */
    protected void deleteNodes() {
        deleteNodes(browserSelectionModel.getSelectedItems());
    }

    /**
     * Deletes the specified tree nodes from the tree view.
     *
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
            deleteTreeItems(selectedItems);
        }
    }

    private void deleteTreeItems(ObservableList<TreeItem<Node>> items) {
        TreeItem<Node> parent = items.get(0).getParent();
        disabledUi.set(true);
        List<String> nodeIds =
                items.stream().map(item -> item.getValue().getUniqueId()).collect(Collectors.toList());
        JobManager.schedule("Delete nodes", monitor -> {
            try {
                saveAndRestoreService.deleteNodes(nodeIds);
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                        MessageFormat.format(Messages.errorDeleteNodeFailed, items.get(0).getValue().getName()),
                        e);
                disabledUi.set(false);
                return;
            }

            Platform.runLater(() -> {
                List<Tab> tabsToRemove = new ArrayList<>();
                List<Tab> visibleTabs = tabPane.getTabs();
                for (Tab tab : visibleTabs) {
                    for (TreeItem<Node> treeItem : items) {
                        if (tab.getId().equals(treeItem.getValue().getUniqueId())) {
                            tabsToRemove.add(tab);
                            tab.getOnCloseRequest().handle(null);
                        }
                    }
                }
                disabledUi.set(false);
                tabPane.getTabs().removeAll(tabsToRemove);
                parent.getChildren().removeAll(items);
            });
        });
    }

    /**
     * Opens a new snapshot view tab associated with the selected configuration.
     */
    protected void openConfigurationForSnapshot() {
        TreeItem<Node> treeItem = browserSelectionModel.getSelectedItems().get(0);
        SnapshotTab tab = new SnapshotTab(treeItem.getValue(), saveAndRestoreService);
        tab.newSnapshot(treeItem.getValue());

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    protected void openCompositeSnapshotForRestore() {
        TreeItem<Node> treeItem = browserSelectionModel.getSelectedItems().get(0);
        SnapshotTab tab = new SnapshotTab(treeItem.getValue(), saveAndRestoreService);
        tab.loadSnapshot(treeItem.getValue());

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    public void openSearchWindow() {
        Optional<Tab> searchTabOptional = tabPane.getTabs().stream().filter(t -> t.getId() != null &&
                t.getId().equals(SearchAndFilterTab.SEARCH_AND_FILTER_TAB_ID)).findFirst();
        if (searchTabOptional.isPresent()) {
            tabPane.getSelectionModel().select(searchTabOptional.get());
        } else {
            SearchAndFilterTab searchAndFilterTab = new SearchAndFilterTab(this);
            tabPane.getTabs().add(0, searchAndFilterTab);
            tabPane.getSelectionModel().select(searchAndFilterTab);
        }
    }

    /**
     * Creates a new folder {@link Node}.
     */
    protected void createNewFolder() {
        createNewFolder(browserSelectionModel.getSelectedItems().get(0));
    }

    /**
     * Creates a new folder {@link Node} in the specified parent.
     *
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

    public void nodeDoubleClicked() {
        nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem().getValue());
    }

    /**
     * Handles double click on the specified tree node. Actual action depends on the {@link Node} type.
     *
     * @param node The double click source
     */
    public void nodeDoubleClicked(Node node) {

        Tab tab;

        switch (node.getNodeType()) {
            case CONFIGURATION:
                tab = new ConfigurationTab();
                ((ConfigurationTab) tab).editConfiguration(node);
                break;
            case SNAPSHOT:
                tab = new SnapshotTab(node, saveAndRestoreService);
                ((SnapshotTab) tab).loadSnapshot(node);
                break;
            case COMPOSITE_SNAPSHOT:
                tab = new CompositeSnapshotTab(this);
                ((CompositeSnapshotTab) tab).editCompositeSnapshot(node);
                break;
            case FOLDER:
            default:
                return;
        }

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void launchTabForNewConfiguration(Node parentNode) {
        ConfigurationTab tab = new ConfigurationTab();
        tab.configureForNewConfiguration(parentNode);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void launchTabForNewCompositeSnapshot(Node parentNode) {
        CompositeSnapshotTab tab = new CompositeSnapshotTab(this);
        tab.configureForNewCompositeSnapshot(parentNode);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private boolean highlightTab(String id) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getId() != null && tab.getId().equals(id)) {
                tabPane.getSelectionModel().select(tab);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new configuration in the selected tree node.
     */
    protected void createNewConfiguration() {
        launchTabForNewConfiguration(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    protected void createNewCompositeSnapshot() {
        launchTabForNewCompositeSnapshot(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    /**
     * Renames a node through the service and its underlying data provider.
     * If there is a problem in the call to the remote JMasar service,
     * the user is shown a suitable error dialog and the name of the node is restored.
     */
    protected void renameNode() {
        TreeItem<Node> node = browserSelectionModel.getSelectedItems().get(0);
        List<String> existingSiblingNodes =
                node.getParent().getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(node.getValue().getNodeType()))
                        .map(item -> item.getValue().getName())
                        .collect(Collectors.toList());
        renameNode(node.getValue(), existingSiblingNodes);
    }

    protected void copyUniqueNodeIdToClipboard() {
        Node node = browserSelectionModel.getSelectedItem().getValue();
        ClipboardContent content = new ClipboardContent();
        content.putString(node.getUniqueId());
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Renames the selected node. A check is made to ensure that user cannot specify a name
     * that is the same as any of its sibling nodes if they are of the same {@link Node} type.
     *
     * @param node                 The node to rename
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
     *
     * @param node The {@link Node} object for the {@link TreeItem}.
     * @return The new {@link TreeItem}.
     */
    protected TreeItem<Node> createTreeItem(final Node node) {
        return new TreeItem<>(node) {
            @Override
            public boolean isLeaf() {
                return node.getNodeType().equals(NodeType.SNAPSHOT) || node.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT);
            }
        };
    }

    /**
     * Handles callback in order to update the tree view when a {@link Node} has changed.
     *
     * @param node The updated node.
     */
    @Override
    public void nodeChanged(Node node) {
        // Find the node that has changed
        TreeItem<Node> nodeSubjectToUpdate = recursiveSearch(node.getUniqueId(), treeView.getRoot());
        if (nodeSubjectToUpdate == null) {
            return;
        }
        nodeSubjectToUpdate.setValue(node);
        // Folder and configuration node changes may include structure changes, so expand to force update.
        if (nodeSubjectToUpdate.getValue().getNodeType().equals(NodeType.FOLDER) ||
                nodeSubjectToUpdate.getValue().getNodeType().equals(NodeType.CONFIGURATION)) {
            if (nodeSubjectToUpdate.getParent() != null) { // null means root folder as it has no parent
                nodeSubjectToUpdate.getParent().getChildren().sort(treeNodeComparator);
            }
            expandTreeNode(nodeSubjectToUpdate);
        }
    }

    /**
     * Handles callback in order to update the tree view when a {@link Node} has been added, e.g. when
     * a snapshot is saved.
     *
     * @param parentNode Parent of the new {@link Node}
     * @param newNodes   The list of new {@link Node}s
     */
    @Override
    public void nodesAdded(Node parentNode, List<Node> newNodes) {
        // Find the parent to which the new node is to be added
        TreeItem<Node> parentTreeItem = recursiveSearch(parentNode.getUniqueId(), treeView.getRoot());
        if (parentTreeItem == null) {
            return;
        }
        expandTreeNode(parentTreeItem);
    }

    /**
     * Utility method used to find the {@link TreeItem} corresponding to the specified {@link Node}'s id.
     *
     * @param nodeIdToLocate The unique string identifying the {@link Node}
     * @param node           The {@link TreeItem} from which to start the search
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

    public void locateNode(Stack<Node> nodeStack) {
        TreeItem<Node> parentTreeItem = treeView.getRoot();

        while (nodeStack.size() > 0) {
            Node currentNode = nodeStack.pop();
            TreeItem<Node> currentTreeItem = recursiveSearch(currentNode.getUniqueId(), parentTreeItem);
            currentTreeItem.setExpanded(true);
            parentTreeItem = currentTreeItem;
        }

        browserSelectionModel.clearSelection();
        browserSelectionModel.select(parentTreeItem);
        treeView.scrollTo(browserSelectionModel.getSelectedIndex());
    }

    /**
     * Locates expanded nodes recursively and adds them to <code>expandedNodes</code>
     *
     * @param expandedNodes The {@link List} holding expanded nodes.
     * @param treeItem      The {@link TreeItem} in which to look for expanded {@link TreeItem}s (nodes).
     */
    private void findExpandedNodes(List<String> expandedNodes, TreeItem<Node> treeItem) {
        if (treeItem.expandedProperty().get() && !treeItem.getChildren().isEmpty()) {
            expandedNodes.add(treeItem.getValue().getUniqueId());
            treeItem.getChildren().forEach(ti -> findExpandedNodes(expandedNodes, ti));
        }
    }

    private void setChildItems(HashMap<String, List<TreeItem<Node>>> allItems, TreeItem<Node> parentItem) {
        if (allItems.containsKey(parentItem.getValue().getUniqueId())) {
            List<TreeItem<Node>> childItems = allItems.get(parentItem.getValue().getUniqueId());
            parentItem.getChildren().setAll(childItems);
            parentItem.getChildren().sort(treeNodeComparator);
            childItems.forEach(ci -> setChildItems(allItems, ci));
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
    public void saveLocalState() {
        // If root item is null, then there is no data in the TreeView
        if (treeView.getRoot() == null) {
            return;
        }
        List<String> expandedNodes = new ArrayList<>();
        findExpandedNodes(expandedNodes, treeView.getRoot());
        try {
            PhoebusPreferenceService.userNodeForClass(SaveAndRestoreApplication.class).put(TREE_STATE, objectMapper.writeValueAsString(expandedNodes));
            if (filterEnabledProperty.get()) {
                PhoebusPreferenceService.userNodeForClass(SaveAndRestoreApplication.class).put(FILTER_NAME, objectMapper.writeValueAsString(filtersComboBox.getSelectionModel().getSelectedItem().getName()));
            }
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, "Failed to persist tree state");
        }
    }

    /**
     * Imports a configuration to a folder {@link Node} from file.
     */
    protected void importConfiguration() {
        Node node = browserSelectionModel.getSelectedItems().get(0).getValue();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Messages.importConfigurationLabel);
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
     * Exports a configuration to file.
     */
    protected void exportConfiguration() {
        Node node = browserSelectionModel.getSelectedItems().get(0).getValue();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Messages.exportConfigurationLabel);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (BMS compatible)", "*.csv"));
            fileChooser.setInitialFileName(browserSelectionModel.getSelectedItems().get(0).getValue().getName());
            File file = fileChooser.showSaveDialog(splitPane.getScene().getWindow());
            if (file != null) {
                if (!file.getAbsolutePath().toLowerCase().endsWith("csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }

                CSVExporter.export(node, file.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Configuration export failed", e);
        }
    }

    /**
     * Imports a snapshot from file to a configuration. Contents must match the PV list of the selected configuration.
     */
    protected void importSnapshot() {
        Node node = browserSelectionModel.getSelectedItems().get(0).getValue();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Messages.importSnapshotLabel);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported file formats (CSV, SNP)", "*.csv", "*.snp"));
            File file = fileChooser.showOpenDialog(splitPane.getScene().getWindow());
            if (file != null) {
                CSVImporter.importFile(node, file);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "SnapshotData import failed", e);
        }
    }

    /**
     * Exports a snapshot to file from selected snapshot node.
     */
    protected void exportSnapshot() {
        exportSnapshot(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    /**
     * Exports the specified snapshot node
     *
     * @param node The snapshot {@link Node} to export.
     */
    public void exportSnapshot(Node node) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(Messages.exportSnapshotLabel);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (SNP compatible)", "*.csv"));
            fileChooser.setInitialFileName(browserSelectionModel.getSelectedItems().get(0).getValue().getName());
            File file = fileChooser.showSaveDialog(splitPane.getScene().getWindow());
            if (file != null) {
                if (!file.getAbsolutePath().toLowerCase().endsWith("csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }
                CSVExporter.export(node, file.getAbsolutePath());
            }
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                    Messages.errorActionFailed,
                    e);
            LOG.log(Level.WARNING, Messages.errorActionFailed, e);
        }
    }

    /**
     * Adds a tag to the selected snapshot {@link Node}
     */
    protected void addTagToSnapshot() {
        addTagToSnapshot(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    /**
     * Adds a tag to the specified snapshot {@link Node}
     *
     * @param node The snapshot to which tag is added.
     */
    public void addTagToSnapshot(Node node) {
        SnapshotNewTagDialog snapshotNewTagDialog = new SnapshotNewTagDialog(node);
        snapshotNewTagDialog.initModality(Modality.APPLICATION_MODAL);

        String locationString = DirectoryUtilities.CreateLocationString(node, true);
        snapshotNewTagDialog.getDialogPane().setHeader(TagUtil.CreateAddHeader(locationString, node.getName()));

        ProposalService proposalService = new ProposalService(new TagProposalProvider(saveAndRestoreService));
        AutocompleteMenu autocompleteMenu = new AutocompleteMenu(proposalService);
        snapshotNewTagDialog.configureAutocompleteMenu(autocompleteMenu);

        Optional<Pair<String, String>> result = snapshotNewTagDialog.showAndWait();
        result.ifPresent(items -> {
            Tag aNewTag = Tag.builder()
                    .name(items.getKey())
                    .comment(items.getValue())
                    .created(new Date())
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
     *
     * @param tagList A list of existing tags, if any.
     */
    protected void tagWithComment(ObservableList<MenuItem> tagList) {
        Node node = browserSelectionModel.getSelectedItems().get(0).getValue();
        tagWithComment(node, tagList);
    }

    /**
     * Adds a tag with comment to the specified snapshot node.
     *
     * @param node    The {@link Node} to enrich with a commented tag.
     * @param tagList List of existing tags, if any.
     */
    public void tagWithComment(Node node, ObservableList<MenuItem> tagList) {
        while (tagList.size() > 2) {
            tagList.remove(tagList.size() - 1);
        }

        if (node.getTags() == null || node.getTags().isEmpty()) {
            CustomMenuItem noTags = TagWidget.NoTagMenuItem();
            noTags.setDisable(true);
            tagList.add(noTags);
        } else {
            node.getTags().sort(new TagComparator());
            node.getTags().forEach(tag -> {
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

    /**
     * Performs check of multiple selection to determine if it fulfills the criteria:
     * <ul>
     *     <li>All selected nodes must be of same type.</li>
     *     <li>All selected nodes must have same parent node.</li>
     * </ul>
     *
     * @return <code>true</code> if criteria are met, otherwise <code>false</code>
     */
    protected boolean checkMultipleSelection() {
        ObservableList<TreeItem<Node>> selectedItems = browserSelectionModel.getSelectedItems();
        if (selectedItems.size() < 2) {
            return true;
        }
        TreeItem<Node> parent = selectedItems.get(0).getParent();
        NodeType nodeType = selectedItems.get(0).getValue().getNodeType();
        return selectedItems.stream().filter(item -> !item.getParent().equals(parent) || !item.getValue().getNodeType().equals(nodeType)).findFirst().isEmpty();
    }

    /**
     * Performs the copy or move operation. When successful, the tree view is updated to reflect the changes.
     *
     * @param sourceNodes  List of {@link Node}s to be copied or moved.
     * @param targetNode   Target {@link Node}, which must be a folder.
     * @param transferMode Must be {@link TransferMode#MOVE} or {@link TransferMode#COPY}.
     */
    protected void performCopyOrMove(List<Node> sourceNodes, Node targetNode, TransferMode transferMode) {
        disabledUi.set(true);
        JobManager.schedule("Copy Or Move save&restore node(s)", monitor -> {
            TreeItem<Node> rootTreeItem = treeView.getRoot();
            TreeItem<Node> targetTreeItem = recursiveSearch(targetNode.getUniqueId(), rootTreeItem);
            try {
                TreeItem<Node> sourceTreeItem = recursiveSearch(sourceNodes.get(0).getUniqueId(), rootTreeItem);
                TreeItem<Node> sourceParentTreeItem = sourceTreeItem.getParent();
                if (transferMode.equals(TransferMode.MOVE)) {
                    saveAndRestoreService.moveNodes(sourceNodes, targetNode);
                    Platform.runLater(() -> {
                        removeMovedNodes(sourceParentTreeItem, sourceNodes);
                        addMovedNodes(targetTreeItem, sourceNodes);
                    });
                } else if (transferMode.equals(TransferMode.COPY)) {
                    saveAndRestoreService.copyNode(sourceNodes, targetNode);
                    List<Node> childNodes = saveAndRestoreService.getChildNodes(targetNode);
                    Platform.runLater(() -> {
                        List<TreeItem<Node>> existingChildItems = targetTreeItem.getChildren();
                        List<Node> existingChildNodes = existingChildItems.stream().map(TreeItem::getValue).collect(Collectors.toList());
                        childNodes.forEach(childNode -> {
                            if (!existingChildNodes.contains(childNode)) {
                                targetTreeItem.getChildren().add(createTreeItem(childNode));
                            }
                        });
                        targetTreeItem.getChildren().sort(treeNodeComparator);
                        targetTreeItem.setExpanded(true);
                    });
                }
            } catch (Exception exception) {
                Logger.getLogger(SaveAndRestoreController.class.getName())
                        .log(Level.SEVERE, "Failed to move or copy");
                ExceptionDetailsErrorDialog.openError(splitPane, Messages.copyOrMoveNotAllowedHeader, Messages.copyOrMoveNotAllowedBody, exception);
            } finally {
                disabledUi.set(false);
            }

        });
    }

    /**
     * Updates the tree view such that moved items are shown in the drop target.
     *
     * @param parentTreeItem The drop target
     * @param nodes          List of {@link Node}s that were moved.
     */
    private void addMovedNodes(TreeItem<Node> parentTreeItem, List<Node> nodes) {
        parentTreeItem.getChildren().addAll(nodes.stream().map(this::createTreeItem).collect(Collectors.toList()));
        parentTreeItem.getChildren().sort(treeNodeComparator);
        TreeItem<Node> nextItemToExpand = parentTreeItem;
        while (nextItemToExpand != null) {
            nextItemToExpand.setExpanded(true);
            nextItemToExpand = nextItemToExpand.getParent();
        }

    }

    /**
     * Updates the tree view such that moved items are removed from source nodes' parent.
     *
     * @param parentTreeItem The parent of the {@link Node}s before the move.
     * @param nodes          List of {@link Node}s that were moved.
     */
    private void removeMovedNodes(TreeItem<Node> parentTreeItem, List<Node> nodes) {
        List<TreeItem<Node>> childItems = parentTreeItem.getChildren();
        List<TreeItem<Node>> treeItemsToRemove = new ArrayList<>();
        childItems.forEach(childItem -> {
            if (nodes.contains(childItem.getValue())) {
                treeItemsToRemove.add(childItem);
            }
        });
        parentTreeItem.getChildren().removeAll(treeItemsToRemove);
        TreeItem<Node> nextItemToExpand = parentTreeItem;
        while (nextItemToExpand != null) {
            nextItemToExpand.setExpanded(true);
            nextItemToExpand = nextItemToExpand.getParent();
        }
    }

    /**
     * Launches the save & restore app and highlights/loads the "resource" (configuration or snapshot) identified
     * by the {@link URI}. If the configuration/snapshot in question cannot be found, an error dialog is shown.
     *
     * @param uri An {@link URI} on the form file:/unique-id?app=saveandrestore, where unique-id is the
     *            unique id of a configuration or snapshot.
     */
    public void openResource(URI uri) {
        if (uri == null) {
            return;
        }
        String nodeId = uri.getPath().substring(1);
        Node node = saveAndRestoreService.getNode(nodeId);
        if (node == null) {
            // Show error dialog.
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(Messages.openResourceFailedTitle);
            alert.setHeaderText(MessageFormat.format(Messages.openResourceFailedHeader, nodeId));
            DialogHelper.positionDialog(alert, treeView, -200, -200);
            alert.show();
            return;
        }
        Stack<Node> copiedStack = new Stack<>();
        DirectoryUtilities.CreateLocationStringAndNodeStack(node, false).getValue().forEach(copiedStack::push);
        locateNode(copiedStack);
        nodeDoubleClicked(node);
    }

    public void findSnapshotReferences() {
        // TODO: implement this as a search request and use search result UI to display result.
    }

    /**
     * @param node A {@link Node} to be checked
     * @return <code>true</code> if a {@link Filter} is enabled/selected and if the {@link Node} is
     * contained in the search result associated with that {@link Filter}.
     */
    public boolean matchesFilter(Node node) {
        if (!filterEnabledProperty.get()) {
            return false;
        }
        TreeItem<Node> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return searchResultNodes.contains(node);
        } else {
            return searchResultNodes.contains(node) &&
                    !selectedItem.getValue().getUniqueId().equals(node.getUniqueId());
        }
    }

    /**
     * Retrieves all {@link Filter}s from service and populates the filter combo box.
     */
    private void loadFilters() {
        try {
            List<Filter> filters = saveAndRestoreService.getAllFilters();
            Platform.runLater(() -> filtersList.setAll(filters));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to load filters", e);
        }
    }

    private void applyFilter(Filter filter) {
        treeView.getSelectionModel().clearSelection();
        Map<String, String> searchParams =
                SearchQueryUtil.parseHumanReadableQueryString(filter.getQueryString());
        // In this case we want to hit all matching, i.e. no pagination.
        searchParams.put(Keys.FROM.getName(), "0");
        searchParams.put(Keys.SIZE.getName(), "10000");
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        searchParams.forEach(map::add);
        JobManager.schedule("Apply Filter", monitor -> {
            try {
                SearchResult searchResult = saveAndRestoreService.search(map);
                searchResultNodes.setAll(searchResult.getNodes());
                Platform.runLater(() -> treeView.refresh());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to perform search when applying filter", e);
            }
        });
    }

    /**
     * Takes action to update the view when a {@link Filter} has been deleted, i.e. remove
     * from drop-down list and clear highlighted nodes.
     *
     * @param filter The deleted {@link Filter}.
     */
    public void filterDeleted(Filter filter) {
        filtersList.remove(filter);
        searchResultNodes.clear();
        treeView.refresh();
        if(filtersList.isEmpty()){
            filterEnabledProperty.set(false);
        }
    }

    public void filterAddedOrUpdated(Filter filter) {
        Filter selectedFilter =  filtersComboBox.getSelectionModel().getSelectedItem();
        boolean selectFilterAfterRefresh =
                selectedFilter != null &&
                selectedFilter.getName().equals(filter.getName());
        loadFilters();
        if(selectFilterAfterRefresh){
            filtersComboBox.getSelectionModel().select(filter);
        }
    }

    private void filterEnabledChanged(boolean enabled) {
        if (!enabled) {
            searchResultNodes.clear();
            treeView.refresh();
        } else {
            Filter filter = filtersComboBox.getSelectionModel().getSelectedItem();
            if (filter != null) {
                applyFilter(filter);
            }
        }
    }
}
