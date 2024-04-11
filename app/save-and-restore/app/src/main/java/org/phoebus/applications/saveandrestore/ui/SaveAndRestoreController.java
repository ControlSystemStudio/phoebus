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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVExporter;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVImporter;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.ui.configuration.ConfigurationTab;
import org.phoebus.applications.saveandrestore.ui.search.SearchAndFilterTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.CompositeSnapshotTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagUtil;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main controller for the save and restore UI.
 */
public class SaveAndRestoreController extends SaveAndRestoreBaseController
        implements Initializable, NodeChangedListener, NodeAddedListener, FilterChangeListener {

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

    protected MultipleSelectionModel<TreeItem<Node>> browserSelectionModel;

    private static final String TREE_STATE = "tree_state";

    private static final String FILTER_NAME = "filter_name";

    protected static final Logger LOG = Logger.getLogger(SaveAndRestoreService.class.getName());

    protected Comparator<TreeItem<Node>> treeNodeComparator;

    protected SimpleBooleanProperty disabledUi = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty filterEnabledProperty = new SimpleBooleanProperty(false);

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
        treeView.getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());
        browserSelectionModel = treeView.getSelectionModel();

        ImageView searchButtonImageView = ImageCache.getImageView(SaveAndRestoreApplication.class, "/icons/sar-search.png");
        searchButtonImageView.setFitWidth(20);
        searchButtonImageView.setFitHeight(20);
        searchButton.setGraphic(searchButtonImageView);

        enableFilterCheckBox.selectedProperty().bindBidirectional(filterEnabledProperty);
        filtersComboBox.disableProperty().bind(filterEnabledProperty.not());
        filterEnabledProperty.addListener((observable, oldValue, newValue) -> filterEnabledChanged(newValue));

        treeView.setEditable(true);

        treeView.setOnMouseClicked(me -> {
            TreeItem<Node> item = browserSelectionModel.getSelectedItem();
            if (item == null) {
                return;
            }
            if (me.getClickCount() == 2) {
                nodeDoubleClicked(item.getValue());
            }
        });

        treeView.setShowRoot(true);

        saveAndRestoreService.addNodeChangeListener(this);
        saveAndRestoreService.addNodeAddedListener(this);
        saveAndRestoreService.addFilterChangeListener(this);

        treeView.setCellFactory(p -> new BrowserTreeCell(this));

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

        enableFilterCheckBox.disableProperty().bind(Bindings.createBooleanBinding(filtersList::isEmpty, filtersList));

        // Clear clipboard to make sure that only custom data format is
        // considered in paste actions.
        Clipboard.getSystemClipboard().clear();

        userIdentity.addListener((a, b, c) -> {
            String name = c == null ? "Root folder" :
                    "Root folder (" + userIdentity.get() + ")";
            treeView.getRoot().setValue(Node.builder().uniqueId(Node.ROOT_FOLDER_UNIQUE_ID).name(name).build());
        });

        loadTreeData();
    }

    /**
     * Loads the data for the tree root as provided (persisted) by the current
     * {@link org.phoebus.applications.saveandrestore.client.SaveAndRestoreClient}.
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
                if (savedTreeViewStructure != null && !savedTreeViewStructure.isEmpty() && savedTreeViewStructure.get(0).equals(rootNode.getUniqueId())) {
                    HashMap<String, List<TreeItem<Node>>> childNodesMap = new HashMap<>();
                    savedTreeViewStructure.forEach(s -> {
                        List<Node> childNodes = saveAndRestoreService.getChildNodes(Node.builder().uniqueId(s).build());
                        if (childNodes != null) { // This may be the case if the tree structure was modified externally
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
                    f.ifPresent(filter -> filtersComboBox.getSelectionModel().select(filter));
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
     * Expands the specified {@link Node}. In order to maintain the list of child {@link Node}s between repeated
     * expand/collapse actions, this method will query the service for the current list of child {@link Node}s and
     * then update the tree view accordingly, i.e. add {@link Node}s that are not yet present, and remove those that
     * have been removed.
     *
     * @param targetItem {@link TreeItem<Node>} on which the operation is performed.
     */
    protected void expandTreeNode(TreeItem<Node> targetItem) {
        List<Node> childNodes = saveAndRestoreService.getChildNodes(targetItem.getValue());
        List<String> childNodeIds = childNodes.stream().map(Node::getUniqueId).collect(Collectors.toList());
        List<String> existingNodeIds =
                targetItem.getChildren().stream().map(item -> item.getValue().getUniqueId()).collect(Collectors.toList());
        List<TreeItem<Node>> itemsToAdd = new ArrayList<>();
        childNodes.forEach(n -> {
            if (!existingNodeIds.contains(n.getUniqueId())) {
                itemsToAdd.add(createTreeItem(n));
            }
        });
        List<TreeItem<Node>> itemsToRemove = new ArrayList<>();
        targetItem.getChildren().forEach(item -> {
            if (!childNodeIds.contains(item.getValue().getUniqueId())) {
                itemsToRemove.add(item);
            }
        });

        targetItem.getChildren().addAll(itemsToAdd);
        targetItem.getChildren().removeAll(itemsToRemove);
        targetItem.getChildren().sort(treeNodeComparator);
        targetItem.setExpanded(true);
    }

    /**
     * Action when user requests comparison between an opened snapshot and a snapshot item selected from
     * the tree view.
     */
    protected void compareSnapshot() {
        compareSnapshot(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    /**
     * Action when user requests comparison between an opened snapshot and the specifies snapshot {@link Node}
     *
     * @param node The snapshot used in the comparison.
     */
    protected void compareSnapshot(Node node) {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) {
            return;
        }
        if (tab instanceof SnapshotTab) {
            try {
                SnapshotTab currentTab = (SnapshotTab) tab;
                currentTab.addSnapshot(node);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to compare snapshot", e);
            }
        }
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
                    if (parentTreeItem.getParent() != null) { // Parent is null for root folder
                        expandTreeNode(parentTreeItem.getParent());
                    }
                    ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                            Messages.errorCreateFolderFailed, new Exception(getException()));
                }
            };

            new Thread(task).start();
        }
    }

    /**
     * Handles double click on the specified tree node. Actual action depends on the {@link Node} type.
     *
     * @param node The double click source
     */
    public void nodeDoubleClicked(Node node) {
        if (getTab(node.getUniqueId()) != null) {
            return;
        }
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
                TreeItem<Node> treeItem = browserSelectionModel.getSelectedItems().get(0);
                tab = new SnapshotTab(treeItem.getValue(), saveAndRestoreService);
                ((SnapshotTab) tab).loadSnapshot(treeItem.getValue());
                break;
            case FOLDER:
            default:
                return;
        }

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    /**
     * Launches the composite snapshot editor view. Note that a tab showing this view uses the "edit_" prefix
     * for the id since it would otherwise clash with a restore view of a composite snapshot.
     */
    public void editCompositeSnapshot() {
        Node compositeSnapshotNode = browserSelectionModel.getSelectedItem().getValue();
        editCompositeSnapshot(compositeSnapshotNode, Collections.emptyList());
    }

    /**
     * Launches the composite snapshot editor view for the purpose of editing an existing
     * composite snapshot.
     *
     * @param compositeSnapshotNode A non-null {@link Node} of type {@link NodeType#COMPOSITE_SNAPSHOT}
     * @param snapshotNodes         A potentially empty (but non-null) list of snapshot nodes to include in
     *                              a new or existing composite snapshot.
     */
    public void editCompositeSnapshot(Node compositeSnapshotNode, List<Node> snapshotNodes) {
        CompositeSnapshotTab compositeSnapshotTab;
        Tab tab = getTab("edit_" + compositeSnapshotNode.getUniqueId());
        if (tab != null) {
            compositeSnapshotTab = (CompositeSnapshotTab) tab;
            compositeSnapshotTab.addToCompositeSnapshot(snapshotNodes);
        } else {
            compositeSnapshotTab = new CompositeSnapshotTab(this);
            compositeSnapshotTab.editCompositeSnapshot(compositeSnapshotNode, snapshotNodes);
            tabPane.getTabs().add(compositeSnapshotTab);
        }
        tabPane.getSelectionModel().select(compositeSnapshotTab);
    }

    /**
     * Launches a {@link Tab} for the purpose of creating a new configuration.
     *
     * @param parentNode A non-null parent {@link Node} that must be of type
     *                   {@link NodeType#FOLDER}.
     */
    private void launchTabForNewConfiguration(Node parentNode) {
        ConfigurationTab tab = new ConfigurationTab();
        tab.configureForNewConfiguration(parentNode);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    /**
     * Launches a {@link Tab} for the purpose of creating a new composite snapshot.
     *
     * @param parentNode    A non-null parent {@link Node} that must be of type
     *                      {@link NodeType#FOLDER}.
     * @param snapshotNodes A potentially empty list of snapshot nodes
     *                      added to the composite snapshot.
     */
    public void launchTabForNewCompositeSnapshot(Node parentNode, List<Node> snapshotNodes) {
        CompositeSnapshotTab tab = new CompositeSnapshotTab(this);
        tab.configureForNewCompositeSnapshot(parentNode, snapshotNodes);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    /**
     * Locates a {@link Tab} in the {@link TabPane }and returns it if it exists.
     *
     * @param id Unique id of a {@link Tab}, which is based on the id of the {@link Node} it
     *           is associated with.
     * @return A non-null {@link Tab} if one is found, otherwise <code>null</code>.
     */
    private Tab getTab(String id) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getId() != null && tab.getId().equals(id)) {
                tabPane.getSelectionModel().select(tab);
                return tab;
            }
        }
        return null;
    }

    /**
     * Creates a new configuration in the selected tree node.
     */
    protected void createNewConfiguration() {
        launchTabForNewConfiguration(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    protected void createNewCompositeSnapshot() {
        launchTabForNewCompositeSnapshot(browserSelectionModel.getSelectedItems().get(0).getValue(),
                Collections.emptyList());
    }

    /**
     * Renames a node through the service and its underlying data provider.
     * If there is a problem in the call to the remote service,
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
                // Since a changed node name may push the node to a different location in the tree view,
                // we need to locate it to keep it selected. The tree view will otherwise "select" the node
                // at the previous position of the renamed node. This is standard JavaFX TreeView behavior
                // where TreeItems are "recycled", and updated by the cell renderer.
                Stack<Node> copiedStack = new Stack<>();
                DirectoryUtilities.CreateLocationStringAndNodeStack(node, false).getValue().forEach(copiedStack::push);
                locateNode(copiedStack);
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
        if(nodeSubjectToUpdate.isExpanded() && (nodeSubjectToUpdate.getValue().getNodeType().equals(NodeType.FOLDER) ||
                    nodeSubjectToUpdate.getValue().getNodeType().equals(NodeType.CONFIGURATION))){
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
        if (node.getValue().getUniqueId().equals(nodeIdToLocate)) {
            return node;
        }
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
            expandTreeNode(currentTreeItem);
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
            parentItem.setExpanded(true);
            Platform.runLater(() -> childItems.forEach(ci -> setChildItems(allItems, ci)));
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
    public static class TagComparator implements Comparator<Tag> {
        @Override
        public int compare(Tag tag1, Tag tag2) {
            return -tag1.getCreated().compareTo(tag2.getCreated());
        }
    }

    /**
     * Self-explanatory
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
            if (filterEnabledProperty.get() && filtersComboBox.getSelectionModel().getSelectedItem() != null) {
                PhoebusPreferenceService.userNodeForClass(SaveAndRestoreApplication.class).put(FILTER_NAME,
                        objectMapper.writeValueAsString(filtersComboBox.getSelectionModel().getSelectedItem().getName()));
            }
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, "Failed to persist tree state");
        }
    }

    public void handleTabClosed() {
        saveLocalState();
        saveAndRestoreService.removeNodeChangeListener(this);
        saveAndRestoreService.removeNodeAddedListener(this);
        saveAndRestoreService.removeFilterChangeListener(this);
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
    protected void addTagToSnapshots() {
        ObservableList<TreeItem<Node>> selectedItems = browserSelectionModel.getSelectedItems();
        List<Node> selectedNodes = selectedItems.stream().map(TreeItem::getValue).collect(Collectors.toList());
        List<Node> updatedNodes = TagUtil.addTag(selectedNodes);
        updatedNodes.forEach(this::nodeChanged);
    }

    /**
     * Configures the "tag with comment" sub-menu. Items are added based on existing {@link Tag}s on the
     * selected {@link Node}s
     *
     * @param tagWithCommentMenu The {@link Menu} subject to configuration.
     */
    public void tagWithComment(final Menu tagWithCommentMenu) {

        List<Node> selectedNodes =
                browserSelectionModel.getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList());

        TagUtil.tagWithComment(tagWithCommentMenu, selectedNodes, updatedNodes -> updatedNodes.forEach(this::nodeChanged));
    }

    /**
     * Configures the "tag as golden" menu item. Depending on the occurrence of the golden {@link Tag} on the
     * selected {@link Node}s, the logic goes as:
     * <ul>
     *     <li>If all selected {@link Node}s have been tagged as golden, the item will offer possibility to remove
     *     the tag on all {@link Node}s.</li>
     *     <li>If none of the selected {@link Node}s have been tagged as golden, the item will offer possibility
     *     to add the tag on all {@link Node}s.</li>
     *     <li>If some - but not all - of the selected {@link Node}s have been tagged as golden, the item is disabled.</li>
     * </ul>
     *
     * @param menuItem The {@link MenuItem} subject to configuration.
     * @return <code>false</code> if the menu item should be disabled.
     */
    public boolean configureGoldenItem(MenuItem menuItem) {
        List<Node> selectedNodes =
                browserSelectionModel.getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList());
        return TagUtil.configureGoldenItem(selectedNodes, menuItem);
    }


    /**
     * Performs check of selection to determine if all selected nodes are of same type.
     *
     * @return <code>true</code> if criteria are met, otherwise <code>false</code>
     */
    public boolean selectedNodesOfSameType() {
        ObservableList<TreeItem<Node>> selectedItems = browserSelectionModel.getSelectedItems();
        if (selectedItems.size() < 2) {
            return true;
        }
        NodeType nodeType = selectedItems.get(0).getValue().getNodeType();
        for (int i = 1; i < selectedItems.size(); i++) {
            if (!selectedItems.get(i).getValue().getNodeType().equals(nodeType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Performs the copy or move operation. When successful, the tree view is updated to reflect the changes.
     *
     * @param sourceNodes  List of {@link Node}s to be copied or moved.
     * @param targetNode   Target {@link Node}, which must be a folder.
     * @param transferMode Must be {@link TransferMode#MOVE} or {@link TransferMode#COPY}.
     */
    protected void moveNodes(List<Node> sourceNodes, Node targetNode, TransferMode transferMode) {
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
                } // TransferMode.COPY not supported
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
     * Launches the save and restore app and highlights/loads the "resource" (configuration or snapshot) identified
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
     * contained in the search result associated with that {@link Filter}. If on the other hand filtering is
     * disabled, then all items match as we have a "no filter".
     */
    public boolean matchesFilter(Node node) {
        if (!filterEnabledProperty.get()) {
            return true;
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

    /**
     * Applies a {@link Filter} selected by user. The service will be queries for {@link Node}s matching
     * the {@link Filter}, then the {@link TreeView} is updated based on the search result.
     *
     * @param filter {@link Filter} selected by user.
     */
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

    /**
     * @return An array of two elements: the configuration {@link Node} anf the snapshot {@link Node} of
     * an active {@link SnapshotTab}.
     */
    public Node[] getConfigAndSnapshotForActiveSnapshotTab() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab instanceof SnapshotTab) {
            SnapshotTab snapshotTab = (SnapshotTab) selectedTab;
            return new Node[]{snapshotTab.getConfigNode(), snapshotTab.getSnapshotNode()};
        }
        return null;
    }

    @Override
    public void filterAddedOrUpdated(Filter filter) {
        if (!filtersList.contains(filter)) {
            filtersList.add(filter);
        } else {
            final int index = filtersList.indexOf(filter);
            Platform.runLater(() -> {
                filtersList.set(index, filter);
                filtersComboBox.valueProperty().set(filter);
                // If this is the active filter, update the tree view
                if (filter.equals(filtersComboBox.getSelectionModel().getSelectedItem())) {
                    applyFilter(filter);
                }
            });
        }
    }

    @Override
    public void filterRemoved(Filter filter) {
        if (filtersList.contains(filter)) {
            filtersList.remove(filter);
            // If this is the active filter, de-select filter completely
            filterEnabledProperty.set(false);
            filtersComboBox.getSelectionModel().select(null);
            // And refresh tree view
            Platform.runLater(() -> treeView.refresh());
        }
    }

    public void copySelectionToClipboard() {
        List<TreeItem<Node>> selectedItems = browserSelectionModel.getSelectedItems();
        List<Node> selectedNodes = selectedItems.stream().map(TreeItem::getValue).collect(Collectors.toList());
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.put(SaveAndRestoreApplication.NODE_SELECTION_FORMAT, selectedNodes);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        clipboard.setContent(clipboardContent);
    }

    /**
     * Checks if tree selection is valid for a copy operation:
     * <ul>
     *     <li>All selected nodes must be of same type.</li>
     *     <li>All selected nodes must have same parent.</li>
     * </ul>
     *
     * @return <code>true</code> if selection may be copied to clipboard, otherwise <code>false</code>.
     */
    public boolean mayCopy() {
        if (userIdentity.isNull().get()) {
            return false;
        }
        List<Node> selectedNodes = browserSelectionModel.getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList());
        if (selectedNodes.size() == 1) {
            return true;
        }
        NodeType nodeTypeOfFirst = selectedNodes.get(0).getNodeType();
        if (selectedNodes.stream().filter(n -> !n.getNodeType().equals(nodeTypeOfFirst)).findFirst().isPresent()) {
            return false;
        }
        TreeItem<Node> parentOfFirst = browserSelectionModel.getSelectedItems().get(0).getParent();
        return browserSelectionModel.getSelectedItems().stream().filter(t -> !t.getParent().equals(parentOfFirst)).findFirst().isEmpty();
    }

    /**
     * Checks if the clipboard content may be pasted onto a target node:
     * <ul>
     *     <li>Clipboard content must be of {@link SaveAndRestoreApplication#NODE_SELECTION_FORMAT}.</li>
     *     <li>Selected node for paste (target) must be single node.</li>
     *     <li>Configurations and composite snapshots may be pasted only onto folder.</li>
     *     <li>Snapshot may be pasted only onto configuration.</li>
     * </ul>
     *
     * @return <code>true</code> if selection may be pasted, otherwise <code>false</code>.
     */
    public boolean mayPaste() {
        if (userIdentity.isNull().get()) {
            return false;
        }
        Object clipBoardContent = Clipboard.getSystemClipboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
        if (clipBoardContent == null || browserSelectionModel.getSelectedItems().size() != 1) {
            return false;
        }
        // Check is made if target node is of supported type for the clipboard content.
        List<Node> selectedNodes = (List<Node>) clipBoardContent;
        NodeType nodeTypeOfFirst = selectedNodes.get(0).getNodeType();
        NodeType nodeTypeOfTarget = browserSelectionModel.getSelectedItem().getValue().getNodeType();
        if ((nodeTypeOfFirst.equals(NodeType.COMPOSITE_SNAPSHOT) ||
                nodeTypeOfFirst.equals(NodeType.CONFIGURATION)) && !nodeTypeOfTarget.equals(NodeType.FOLDER)) {
            return false;
        } else if (nodeTypeOfFirst.equals(NodeType.SNAPSHOT) && !nodeTypeOfTarget.equals(NodeType.CONFIGURATION)) {
            return false;
        }
        return true;
    }

    public void pasteFromClipboard() {
        disabledUi.set(true);
        Object selectedNodes = Clipboard.getSystemClipboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
        if (selectedNodes == null || browserSelectionModel.getSelectedItems().size() != 1) {
            return;
        }
        List<String> selectedNodeIds =
                ((List<Node>) selectedNodes).stream().map(Node::getUniqueId).collect(Collectors.toList());
        JobManager.schedule("copy nodes", monitor -> {
            try {
                saveAndRestoreService.copyNodes(selectedNodeIds, browserSelectionModel.getSelectedItem().getValue().getUniqueId());
                disabledUi.set(false);
            } catch (Exception e) {
                disabledUi.set(false);
                ExceptionDetailsErrorDialog.openError(Messages.errorGeneric, Messages.failedToPasteObjects, e);
                LOG.log(Level.WARNING, "Failed to paste nodes into target " + browserSelectionModel.getSelectedItem().getValue().getName());
                return;
            }
            Platform.runLater(() -> {
                expandTreeNode(browserSelectionModel.getSelectedItem());
                treeView.refresh();
            });
        });
    }

    /**
     * Used to determine if nodes selected in the tree view have the same parent node. Most menu items
     * do not make sense unless the selected nodes have same the parent node.
     *
     * @return <code>true</code> if all selected nodes have the same parent node, <code>false</code> otherwise.
     */
    public boolean hasSameParent() {
        ObservableList<TreeItem<Node>> selectedItems = browserSelectionModel.getSelectedItems();
        if (selectedItems.size() == 1) {
            return true;
        }
        Node parentNodeOfFirst = selectedItems.get(0).getParent().getValue();
        for (int i = 1; i < selectedItems.size(); i++) {
            TreeItem<Node> treeItem = selectedItems.get(i);
            if (!treeItem.getParent().getValue().getUniqueId().equals(parentNodeOfFirst.getUniqueId())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return <code>true</code> selection contains multiple {@link Node}s, otherwise <code>false</code>.
     */
    public boolean multipleNodesSelected() {
        return browserSelectionModel.getSelectedItems().size() > 1;
    }

    /**
     * Checks if selection is not allowed, i.e. not all selected nodes are snapshot nodes.
     *
     * @return <code>false</code> if any of the selected nodes is of type {@link NodeType#FOLDER} or
     * {@link NodeType#CONFIGURATION}. Since these {@link NodeType}s cannot be tagged.
     */
    public boolean checkTaggable() {
        return browserSelectionModel.getSelectedItems().stream().filter(i -> i.getValue().getNodeType().equals(NodeType.FOLDER) ||
                i.getValue().getNodeType().equals(NodeType.CONFIGURATION)).findFirst().isEmpty();
    }

    /**
     * Determines if comparing snapshots is possible, which is the case if all of the following holds true:
     * <ul>
     *     <li>The active tab must be a {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     *     <li>The active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab} must not show an unsaved snapshot.</li>
     *     <li>The snapshot selected from the tree view must have same parent as the one shown in the active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     *     <li>The snapshot selected from the tree view must not be the same as as the one shown in the active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     * </ul>
     *
     * @return <code>true</code> if selection can be added to snapshot view for comparison.
     */
    public boolean compareSnapshotsPossible() {
        Node[] configAndSnapshotNode = getConfigAndSnapshotForActiveSnapshotTab();
        if (configAndSnapshotNode == null) {
            return false;
        }
        TreeItem<Node> selectedItem = treeView.getSelectionModel().getSelectedItem();
        TreeItem<Node> parentItem = selectedItem.getParent();
        return configAndSnapshotNode[1].getUniqueId() != null &&
                parentItem.getValue().getUniqueId().equals(configAndSnapshotNode[0].getUniqueId()) &&
                !selectedItem.getValue().getUniqueId().equals(configAndSnapshotNode[1].getUniqueId());
    }

    @Override
    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens) {
        super.secureStoreChanged(validTokens);
        tabPane.getTabs().forEach(t -> {
            ((SaveAndRestoreTab) t).secureStoreChanged(validTokens);
        });
    }
}
