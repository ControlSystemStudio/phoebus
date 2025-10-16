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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.RestoreUtil;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.actions.OpenNodeAction;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVExporter;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVImporter;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.FilterActivator;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.applications.saveandrestore.ui.configuration.ConfigurationTab;
import org.phoebus.applications.saveandrestore.ui.contextmenu.CopyUniqueIdToClipboardMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.CreateSnapshotMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.EditCompositeMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.ExportToCSVMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.FindReferencesMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.ImportFromCSVMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.LoginMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.NewCompositeSnapshotMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.NewConfigurationMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.NewFolderMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.RenameFolderMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.RestoreFromClientMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.RestoreFromServiceMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.TagGoldenMenuItem;
import org.phoebus.applications.saveandrestore.ui.search.SearchAndFilterTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.CompositeSnapshotTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagUtil;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main controller for the save and restore UI.
 */
public class SaveAndRestoreController extends SaveAndRestoreBaseController
        implements Initializable, WebSocketMessageHandler {

    @FXML
    protected TreeView<Node> treeView;

    @FXML
    protected TabPane tabPane;

    @FXML
    protected SplitPane splitPane;

    @SuppressWarnings("unused")
    @FXML
    private VBox progressIndicator;

    @SuppressWarnings("unused")
    @FXML
    private ComboBox<Filter> filtersComboBox;

    @SuppressWarnings("unused")
    @FXML
    private Button searchButton;

    @SuppressWarnings("unused")
    @FXML
    private CheckBox autoFilterCheckbox;

    @SuppressWarnings("unused")
    @FXML
    private VBox treeViewPane;

    private final ObjectMapper objectMapper = new ObjectMapper();
    protected MultipleSelectionModel<TreeItem<Node>> browserSelectionModel;
    private static final String TREE_STATE = "tree_state";
    private static final String FILTER_NAME = "filter_name";
    protected static final Logger LOG = Logger.getLogger(SaveAndRestoreController.class.getName());
    protected Comparator<TreeItem<Node>> treeNodeComparator;
    protected SimpleBooleanProperty disabledUi = new SimpleBooleanProperty(false);
    private static final Logger logger = Logger.getLogger(SaveAndRestoreController.class.getName());
    private final ObjectProperty<Filter> currentFilterProperty = new SimpleObjectProperty<>(null);


    @SuppressWarnings("unused")
    @FXML
    private Tooltip filterToolTip;

    @SuppressWarnings("unused")
    @FXML
    private VBox errorPane;

    private final ObservableList<Node> searchResultNodes = FXCollections.observableArrayList();
    private final ObservableList<Filter> filtersList = FXCollections.observableArrayList();

    private final CountDownLatch treeInitializationCountDownLatch = new CountDownLatch(1);
    private final ObservableList<Node> selectedItemsProperty = FXCollections.observableArrayList();
    private final SimpleBooleanProperty serviceConnected = new SimpleBooleanProperty();

    private final ContextMenu contextMenu = new ContextMenu();
    private final Menu tagWithComment = new Menu(Messages.contextMenuTags, new ImageView(ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-add_tag.png")));
    private final MenuItem copyMenuItem = new MenuItem(Messages.copy, ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));
    private final MenuItem compareSnapshotsMenuItem = new MenuItem(Messages.contextMenuCompareSnapshots, ImageCache.getImageView(ImageCache.class, "/icons/save-and-restore/compare.png"));
    private final MenuItem deleteNodeMenuItem = new MenuItem(Messages.contextMenuDelete, ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
    private final MenuItem pasteMenuItem = new MenuItem(Messages.paste, ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));
    private final ObservableValue<? extends ObservableList<Filter>> comboBoxItems = new SimpleObjectProperty<>(filtersList);

    private final BooleanProperty autoFilterActive = new SimpleBooleanProperty();

    /**
     * Potentially empty list of {@link FilterActivator}s implementing auto selection of {@link Filter}s.
     */
    private final ObservableList<FilterActivator> filterActivators = FXCollections.observableArrayList();


    List<MenuItem> menuItems = Arrays.asList(
            new LoginMenuItem(this, selectedItemsProperty,
                    () -> ApplicationService.createInstance("credentials_management")),
            new NewFolderMenuItem(this, selectedItemsProperty, this::createNewFolder),
            new NewConfigurationMenuItem(this, selectedItemsProperty, this::createNewConfiguration),
            new CreateSnapshotMenuItem(this, selectedItemsProperty, this::openConfigurationForSnapshot),
            new NewCompositeSnapshotMenuItem(this, selectedItemsProperty, this::createNewCompositeSnapshot),
            new RestoreFromClientMenuItem(this, selectedItemsProperty,
                    () -> {
                        disabledUi.set(true);
                        RestoreUtil.restore(RestoreMode.CLIENT_RESTORE, saveAndRestoreService, selectedItemsProperty.get(0), () -> disabledUi.set(false));
                    }),
            new RestoreFromServiceMenuItem(this, selectedItemsProperty,
                    () -> {
                        disabledUi.set(true);
                        RestoreUtil.restore(RestoreMode.SERVICE_RESTORE, saveAndRestoreService, selectedItemsProperty.get(0), () -> disabledUi.set(false));
                    }),
            new SeparatorMenuItem(),
            new EditCompositeMenuItem(this, selectedItemsProperty, this::editCompositeSnapshot),
            new FindReferencesMenuItem(this, selectedItemsProperty, this::findReferences),
            new RenameFolderMenuItem(this, selectedItemsProperty, this::renameNode),
            copyMenuItem,
            pasteMenuItem,
            deleteNodeMenuItem,
            new SeparatorMenuItem(),
            compareSnapshotsMenuItem,
            new TagGoldenMenuItem(this, selectedItemsProperty),
            tagWithComment,
            new SeparatorMenuItem(),
            new CopyUniqueIdToClipboardMenuItem(this, selectedItemsProperty, this::copyUniqueNodeIdToClipboard),
            new SeparatorMenuItem(),
            new ImportFromCSVMenuItem(this, selectedItemsProperty, this::importFromCSV),
            new ExportToCSVMenuItem(this, selectedItemsProperty, this::exportToCSV)
    );


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Tree items are first compared on type, then on name (case-insensitive).
        treeNodeComparator = Comparator.comparing(TreeItem::getValue);

        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeViewPane.getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());

        browserSelectionModel = treeView.getSelectionModel();

        autoFilterCheckbox.selectedProperty().bindBidirectional(autoFilterActive);

        autoFilterActive.addListener((obs, o, n) -> {
            if (n) {
                // Check if a filter selection is active in any implementation. Match on first found.
                Optional<FilterActivator> filterActivatorOptional =
                        filterActivators.stream().filter(a -> a.getActivatedFilter() != null).findFirst();
                filterActivatorOptional.ifPresent(filterActivator -> activateFilter(filterActivator.getActivatedFilter()));
            }
        });

        autoFilterCheckbox.visibleProperty().bind(Bindings.createBooleanBinding(() ->
                !filterActivators.isEmpty(), filterActivators));

        filtersComboBox.disableProperty().bind(Bindings.createBooleanBinding(autoFilterActive::get, autoFilterActive));
        filtersComboBox.valueProperty().bindBidirectional(currentFilterProperty);
        currentFilterProperty.addListener((obs, o, n) -> applyFilter(n));

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
        treeView.setCellFactory(p -> new BrowserTreeCell(this));
        treeViewPane.disableProperty().bind(disabledUi);
        progressIndicator.visibleProperty().bind(disabledUi);

        filtersComboBox.setCellFactory(new Callback<>() {
            @Override
            public ListCell<org.phoebus.applications.saveandrestore.model.search.Filter> call(ListView<org.phoebus.applications.saveandrestore.model.search.Filter> param) {
                return new ListCell<>() {
                    @Override
                    public void updateItem(org.phoebus.applications.saveandrestore.model.search.Filter item,
                                           boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            setText(item == null ? Messages.noFilter : item.getName());
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
                            return Messages.noFilter;
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
            } else {
                clearFilter();
            }
        });

        filtersComboBox.itemsProperty().bind(comboBoxItems);

        // Clear clipboard to make sure that only custom data format is
        // considered in paste actions.
        Clipboard.getSystemClipboard().clear();

        userIdentity.addListener((a, b, c) -> {
            String name = c == null ? "Root folder" :
                    "Root folder (" + userIdentity.get() + ")";
            treeView.getRoot().setValue(Node.builder().uniqueId(Node.ROOT_FOLDER_UNIQUE_ID).name(name).build());
        });

        MenuItem addTagMenuItem = TagWidget.AddTagMenuItem();
        addTagMenuItem.setOnAction(action -> addTagToSnapshots());
        tagWithComment.getItems().addAll(addTagMenuItem);

        copyMenuItem.setOnAction(ae -> copySelectionToClipboard());
        compareSnapshotsMenuItem.setOnAction(ae -> compareSnapshot());
        deleteNodeMenuItem.setOnAction(ae -> deleteNodes());
        pasteMenuItem.setOnAction(ae -> pasteFromClipboard());

        contextMenu.getItems().addAll(menuItems);
        treeView.setContextMenu(contextMenu);

        splitPane.disableProperty().bind(serviceConnected.not());
        treeView.visibleProperty().bind(serviceConnected);
        errorPane.visibleProperty().bind(serviceConnected.not());

        webSocketClientService.addWebSocketMessageHandler(this);
        webSocketClientService.setConnectCallback(this::handleWebSocketConnected);
        webSocketClientService.setDisconnectCallback(this::handleWebSocketDisconnected);
        webSocketClientService.connect();

    }

    /**
     * Pulls initial data from the service in order to configure the UI and render the {@link TreeView}.
     * {@link org.phoebus.applications.saveandrestore.client.SaveAndRestoreClient}.
     */
    public void loadInitialData() {

        JobManager.schedule("Load save-and-restore tree data", monitor -> {
            Node rootNode = saveAndRestoreService.getRootNode();
            treeInitializationCountDownLatch.countDown();
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
                        List<TreeItem<Node>> childItems = childNodes.stream().map(this::createTreeItem).sorted(treeNodeComparator).collect(Collectors.toList());
                        childNodesMap.put(s, childItems);
                    }
                });
                setChildItems(childNodesMap, rootItem);

            } else {
                List<Node> childNodes = saveAndRestoreService.getChildNodes(rootItem.getValue());
                List<TreeItem<Node>> childItems = childNodes.stream().map(this::createTreeItem).sorted(treeNodeComparator).toList();
                rootItem.getChildren().addAll(childItems);
            }

            // Get all filters from service
            filtersList.addAll(saveAndRestoreService.getAllFilters());

            Platform.runLater(() -> {
                treeView.setRoot(rootItem);
                expandNodes(treeView.getRoot());
                // Event handler for expanding nodes
                treeView.getRoot().addEventHandler(TreeItem.<Node>branchExpandedEvent(), e -> expandTreeNode(e.getTreeItem()));
                treeInitializationCountDownLatch.countDown();
                filtersList.add(0, null);
                String savedFilterName = getSavedFilterName();
                if (savedFilterName != null) {
                    Optional<Filter> f = filtersComboBox.getItems().stream().filter(filter -> filter.getName().equals(savedFilterName)).findFirst();
                    f.ifPresent(filter -> filtersComboBox.getSelectionModel().select(filter));
                }
                setupFilterActivators();
            });
        });
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
     * then update the tree view accordingly.
     *
     * @param targetItem {@link TreeItem<Node>} on which the operation is performed.
     */
    protected void expandTreeNode(TreeItem<Node> targetItem) {
        List<Node> childNodes = saveAndRestoreService.getChildNodes(targetItem.getValue());
        List<TreeItem<Node>> list =
                childNodes.stream().map(this::createTreeItem).toList();
        targetItem.getChildren().setAll(list);
        targetItem.getChildren().sort(treeNodeComparator);
    }

    /**
     * Action when user requests comparison between an opened snapshot and the selected {@link Node}.
     */
    private void compareSnapshot() {
        Node node = browserSelectionModel.getSelectedItems().get(0).getValue();
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) {
            return;
        }
        if (tab instanceof SnapshotTab currentTab) {
            try {
                currentTab.addSnapshot(node);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to compare snapshot", e);
            }
        }
    }

    /**
     * Deletes the tree items selected in the tree view.
     */
    private void deleteNodes() {
        ObservableList<TreeItem<Node>> selectedItems = browserSelectionModel.getSelectedItems();
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
        disabledUi.set(true);
        List<String> nodeIds =
                items.stream().map(item -> item.getValue().getUniqueId()).collect(Collectors.toList());
        JobManager.schedule("Delete nodes", monitor -> {
            try {
                saveAndRestoreService.deleteNodes(nodeIds);
                disabledUi.set(false);
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                        MessageFormat.format(Messages.errorDeleteNodeFailed, items.get(0).getValue().getName()),
                        e);
                disabledUi.set(false);
            }
        });
    }

    /**
     * Opens a new snapshot view tab associated with the selected configuration.
     */
    private void openConfigurationForSnapshot() {
        TreeItem<Node> treeItem = browserSelectionModel.getSelectedItems().get(0);
        SnapshotTab tab = new SnapshotTab(treeItem.getValue(), saveAndRestoreService);
        tab.newSnapshot(treeItem.getValue());

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    @SuppressWarnings("unused")
    @FXML
    public SearchAndFilterTab openSearchWindow() {
        Optional<Tab> searchTabOptional = tabPane.getTabs().stream().filter(t -> t.getId() != null &&
                t.getId().equals(SearchAndFilterTab.SEARCH_AND_FILTER_TAB_ID)).findFirst();
        if (searchTabOptional.isPresent()) {
            tabPane.getSelectionModel().select(searchTabOptional.get());
            return (SearchAndFilterTab) searchTabOptional.get();
        } else {
            SearchAndFilterTab searchAndFilterTab = new SearchAndFilterTab(this);
            tabPane.getTabs().add(0, searchAndFilterTab);
            tabPane.getSelectionModel().select(searchAndFilterTab);
            return searchAndFilterTab;
        }
    }

    /**
     * Creates a new folder {@link Node}.
     */
    private void createNewFolder() {
        TreeItem<Node> parentTreeItem = browserSelectionModel.getSelectedItems().get(0);
        List<String> existingFolderNames =
                parentTreeItem.getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(NodeType.FOLDER))
                        .map(item -> item.getValue().getName())
                        .toList();

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
            case SNAPSHOT, COMPOSITE_SNAPSHOT:
                tab = new SnapshotTab(node, saveAndRestoreService);
                ((SnapshotTab) tab).loadSnapshot(node);
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
    private void editCompositeSnapshot() {
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
            compositeSnapshotTab.editCompositeSnapshot(compositeSnapshotNode);
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
    private void createNewConfiguration() {
        launchTabForNewConfiguration(browserSelectionModel.getSelectedItems().get(0).getValue());
    }

    private void createNewCompositeSnapshot() {
        launchTabForNewCompositeSnapshot(browserSelectionModel.getSelectedItems().get(0).getValue(),
                Collections.emptyList());
    }

    private void copyUniqueNodeIdToClipboard() {
        Node node = browserSelectionModel.getSelectedItem().getValue();
        ClipboardContent content = new ClipboardContent();
        content.putString(node.getUniqueId());
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Renames a node through the service and its underlying data provider.
     * If there is a problem in the call to the remote service,
     * the user is shown a suitable error dialog and the name of the node is restored.
     */
    private void renameNode() {
        TreeItem<Node> node = browserSelectionModel.getSelectedItems().get(0);
        List<String> existingSiblingNodes =
                node.getParent().getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(node.getValue().getNodeType()))
                        .map(item -> item.getValue().getName())
                        .toList();

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
                saveAndRestoreService.updateNode(node.getValue(), false);
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
    private TreeItem<Node> createTreeItem(final Node node) {
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

    private void nodeChanged(Node node) {
        // Find the node that has changed
        TreeItem<Node> nodeSubjectToUpdate = recursiveSearch(node.getUniqueId(), treeView.getRoot());
        if (nodeSubjectToUpdate == null) {
            return;
        }
        nodeSubjectToUpdate.setValue(node);
        // For updated and expanded folder nodes, refresh with respect to child nodes as
        // a move/copy operation may add/remove nodes.
        if (nodeSubjectToUpdate.getValue().getNodeType().equals(NodeType.FOLDER) && nodeSubjectToUpdate.isExpanded()) {
            expandTreeNode(nodeSubjectToUpdate);
        }
    }

    /**
     * Handles callback in order to update the tree view when a {@link Node} has been added, e.g. when
     * a snapshot is saved. The purpose is to update the {@link TreeView} accordingly to reflect the change.
     *
     * @param nodeId Unique id of the added {@link Node}
     */
    private void nodeAdded(String nodeId) {
        Node newNode = saveAndRestoreService.getNode(nodeId);
        try {
            Node parentNode = saveAndRestoreService.getParentNode(nodeId);
            // Find the parent to which the new node is to be added
            TreeItem<Node> parentTreeItem = recursiveSearch(parentNode.getUniqueId(), treeView.getRoot());
            TreeItem<Node> newTreeItem = createTreeItem(newNode);
            parentTreeItem.getChildren().add(newTreeItem);
            parentTreeItem.getChildren().sort(treeNodeComparator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles callback in order to update the tree view when a {@link Node} has been deleted.
     * The purpose is to update the {@link TreeView} accordingly to reflect the change.
     *
     * @param nodeId Unique id of the deleted {@link Node}
     */
    private void nodeRemoved(String nodeId) {
        TreeItem<Node> treeItemToRemove = recursiveSearch(nodeId, treeView.getRoot());
        if (treeItemToRemove != null) {
            treeItemToRemove.getParent().getChildren().remove(treeItemToRemove);
        }
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

        while (!nodeStack.isEmpty()) {
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
            if (filtersComboBox.getSelectionModel().getSelectedItem() != null) {
                PhoebusPreferenceService.userNodeForClass(SaveAndRestoreApplication.class).put(FILTER_NAME,
                        objectMapper.writeValueAsString(filtersComboBox.getSelectionModel().getSelectedItem().getName()));
            }
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, "Failed to persist tree state");
        }
    }

    @Override
    public void handleTabClosed() {
        tabPane.getTabs().forEach(t -> ((SaveAndRestoreTab)t).handleTabClosed());
        saveLocalState();
        webSocketClientService.closeWebSocket();
        filterActivators.forEach(FilterActivator::stop);
    }

    /**
     * Imports configuration or snapshot from CSV.
     */
    private void importFromCSV() {
        Node node = browserSelectionModel.getSelectedItems().get(0).getValue();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(node.getNodeType().equals(NodeType.FOLDER) ? Messages.importConfigurationLabel : Messages.importSnapshotLabel);
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
     * Exports configuration or snapshot to CSV.
     */
    private void exportToCSV() {
        Node node = browserSelectionModel.getSelectedItems().get(0).getValue();
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(node.getNodeType().equals(NodeType.FOLDER) ? Messages.exportConfigurationLabel : Messages.exportSnapshotLabel);
            String extensionFilterDesc = node.getNodeType().equals(NodeType.CONFIGURATION) ?
                    "CSV (BMS compatible)" :
                    "CSV (SNP compatible)";
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extensionFilterDesc, "*.csv"));
            fileChooser.setInitialFileName(node.getName());
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
        TagUtil.addTag(selectedNodes);
    }

    /**
     * Configures the "tag" sub-menu. Items are added based on existing {@link Tag}s on the
     * selected {@link Node}s
     *
     * @param tagMenu The {@link Menu} subject to configuration.
     */
    public void configureTagContextMenu(final Menu tagMenu) {

        List<Node> selectedNodes =
                browserSelectionModel.getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList());
        TagUtil.tag(tagMenu, selectedNodes, updatedNodes -> updatedNodes.forEach(this::nodeChanged));
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
            try {
                if (transferMode.equals(TransferMode.MOVE)) {
                    saveAndRestoreService.moveNodes(sourceNodes, targetNode);
                }// TransferMode.COPY not supported
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
     * Parses the {@link URI} to determine what to do. Supported actions/behavior:
     * <ul>
     *     <li>Open a {@link Node}, which must not be of {@link NodeType#FOLDER}.</li>
     *     <li>Launch the search/filter view to show a filter search result.</li>
     * </ul>
     *
     * @param uri An {@link URI} on the form file:/unique-id?action=[open_sar_node|open_sar_filter]&app=saveandrestore, where unique-id is the
     *            unique id of a {@link Node} or the unique id (name) of a {@link Filter}. If action is not specified,
     *            it defaults to open-node.
     */
    public void openResource(URI uri) {

        if (uri == null) {
            return;
        }
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            logger.log(Level.WARNING, "Called with empty URI");
            return;
        }
        String[] queries = query.split("&");
        String action = OpenNodeAction.OPEN_SAR_NODE;
        Optional<String> actionQuery = Arrays.stream(queries).filter(q -> q.startsWith("action=")).findFirst();
        if (actionQuery.isEmpty()) {
            logger.log(Level.WARNING, "Open resource does not specify action, defaulting to '" + action + "'");
        } else if (actionQuery.get().substring("action=".length()).isEmpty()) {
            logger.log(Level.WARNING, "Empty action specified, defaulting to '" + action + "'");
        } else {
            action = actionQuery.get().substring("action=".length());
        }

        switch (action) {
            case OpenNodeAction.OPEN_SAR_NODE:
                openNode(uri.getPath().substring(1));
                break;
            default:
                logger.log(Level.WARNING, "Action '" + action + "' not supported");
        }
    }

    /**
     * @param node A {@link Node} to be checked
     * @return <code>true</code> if a {@link Filter} is enabled/selected and if the {@link Node} is
     * contained in the search result associated with that {@link Filter}. If on the other hand filtering is
     * disabled, then all items match as we have a "no filter".
     */
    public boolean matchesFilter(Node node) {
        if (currentFilterProperty.isNull().get()) {
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
     * Applies a {@link Filter} selected by user. The service will be queries for {@link Node}s matching
     * the {@link Filter}, then the {@link TreeView} is updated based on the search result.
     *
     * @param filter {@link Filter} selected by user or through business logic. If <code>null</code>, then the
     *                             <code>no filter</code> {@link Filter} is applied.
     */
    private void applyFilter(Filter filter) {
        treeView.getSelectionModel().clearSelection();
        if(filter == null){
            return;
        }
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

    private void clearFilter() {
        searchResultNodes.clear();
        treeView.refresh();
    }

    /**
     * @return An array of two elements: the configuration {@link Node} anf the snapshot {@link Node} of
     * an active {@link SnapshotTab}.
     */
    public Node[] getConfigAndSnapshotForActiveSnapshotTab() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab instanceof SnapshotTab snapshotTab) {
            return new Node[]{snapshotTab.getConfigNode(), snapshotTab.getSnapshotNode()};
        }
        return null;
    }

    private void filterAddedOrUpdated(Filter filter) {
        if (!filtersList.contains(filter)) {
            filtersList.add(filter);
        } else {
            final int index = filtersList.indexOf(filter);
            filtersList.set(index, filter);
            filtersComboBox.valueProperty().set(filter);
            // If this is the current filter, update the tree view
            if (filter.equals(filtersComboBox.getSelectionModel().getSelectedItem())) {
               currentFilterProperty.set(filter);
            }
        }
    }

    /**
     * Handles removal of a {@link Filter}.
     * <p>
     * If the name matches the {@link Filter} currently being loaded, the filter selection is cleared, i.e.
     * it switches to "no filter".
     * </p>
     *
     * @param name The name of a {@link Filter}
     */
    private void filterRemoved(String name) {
        Optional<Filter> filterOptional = filtersList.stream().filter(f -> f != null && f.getName().equals(name)).findFirst();
        if (filterOptional.isPresent()) {
            Filter filterToRemove = new Filter();
            filterToRemove.setName(name);
            Platform.runLater(() -> {
                if (filterToRemove.equals(filtersComboBox.getSelectionModel().getSelectedItem())) {
                    currentFilterProperty.set(null);
                }
                filtersList.remove(filterToRemove);
            });
        }
    }

    private void copySelectionToClipboard() {
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
        if (selectedItemsProperty.stream().anyMatch(n -> n.getNodeType().equals(NodeType.FOLDER))) {
            return false;
        }
        NodeType nodeTypeOfFirst = selectedItemsProperty.get(0).getNodeType();
        if (selectedItemsProperty.stream().anyMatch(n -> !n.getNodeType().equals(nodeTypeOfFirst))) {
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
        if (selectedItemsProperty.size() != 1 ||
                selectedItemsProperty.get(0).getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID) ||
                (!selectedItemsProperty.get(0).getNodeType().equals(NodeType.FOLDER) && !selectedItemsProperty.get(0).getNodeType().equals(NodeType.CONFIGURATION))) {
            return false;
        }
        // Check is made if target node is of supported type for the clipboard content.
        List<Node> selectedNodes = (List<Node>) clipBoardContent;
        NodeType nodeTypeOfFirst = selectedNodes.get(0).getNodeType();
        NodeType nodeTypeOfTarget = browserSelectionModel.getSelectedItem().getValue().getNodeType();
        if ((nodeTypeOfFirst.equals(NodeType.COMPOSITE_SNAPSHOT) ||
                nodeTypeOfFirst.equals(NodeType.CONFIGURATION)) && !nodeTypeOfTarget.equals(NodeType.FOLDER)) {
            return false;
        } else {
            return !nodeTypeOfFirst.equals(NodeType.SNAPSHOT) || nodeTypeOfTarget.equals(NodeType.CONFIGURATION);
        }
    }

    private void pasteFromClipboard() {
        disabledUi.set(true);
        Object selectedNodes = Clipboard.getSystemClipboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
        if (selectedNodes == null || browserSelectionModel.getSelectedItems().size() != 1) {
            return;
        }
        List<String> selectedNodeIds =
                ((List<Node>) selectedNodes).stream().map(Node::getUniqueId).collect(Collectors.toList());
        JobManager.schedule("Copy nodes", monitor -> {
            try {
                saveAndRestoreService.copyNodes(selectedNodeIds, browserSelectionModel.getSelectedItem().getValue().getUniqueId());
                disabledUi.set(false);
            } catch (Exception e) {
                disabledUi.set(false);
                ExceptionDetailsErrorDialog.openError(Messages.errorGeneric, Messages.failedToPasteObjects, e);
                LOG.log(Level.WARNING, "Failed to paste nodes into target " + browserSelectionModel.getSelectedItem().getValue().getName());
            }
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
     * Determines if comparing snapshots is possible, which is the case if all the following holds true:
     * <ul>
     *     <li>The active tab must be a {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     *     <li>The active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab} must not show an unsaved snapshot.</li>
     *     <li>The snapshot selected from the tree view must have same parent as the one shown in the active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     *     <li>The snapshot selected from the tree view must not be the same as the one shown in the active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     * </ul>
     *
     * @return <code>true</code> if selection can be added to snapshot view for comparison.
     */
    public boolean compareSnapshotsPossible() {
        if (selectedItemsProperty.size() != 1 ||
                !selectedItemsProperty.get(0).getNodeType().equals(NodeType.SNAPSHOT)) {
            return false;
        }
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
        tabPane.getTabs().forEach(t -> ((SaveAndRestoreTab) t).secureStoreChanged(validTokens));
    }

    private void openNode(String nodeId) {
        JobManager.schedule("Open save-and-restore node", monitor -> {
            try {
                if (!treeInitializationCountDownLatch.await(30000, TimeUnit.SECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Failed to await tree view to load", e);
                return;
            }
            Node node = saveAndRestoreService.getNode(nodeId);
            if (node == null) {
                Platform.runLater(() -> {
                    // Show error dialog.
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle(Messages.openResourceFailedTitle);
                    alert.setHeaderText(MessageFormat.format(Messages.openResourceFailedHeader, nodeId));
                    DialogHelper.positionDialog(alert, treeView, -200, -200);
                    alert.show();
                });
                return;
            }
            Stack<Node> copiedStack = new Stack<>();
            Platform.runLater(() -> {
                DirectoryUtilities.CreateLocationStringAndNodeStack(node, false).getValue().forEach(copiedStack::push);
                locateNode(copiedStack);
                nodeDoubleClicked(node);
            });
        });
    }


    /**
     * Called when user requests context menu. Updates the {@link #selectedItemsProperty}, and since
     * all {@link org.phoebus.applications.saveandrestore.ui.contextmenu.SaveAndRestoreMenuItem}s listen
     * to changes to this property, they will update themselves based on the user selection.
     *
     * @param e A {@link ContextMenuEvent}
     */
    @SuppressWarnings("unused")
    @FXML
    public void configureContextMenu(ContextMenuEvent e) {
        ObservableList<? extends TreeItem<Node>> selectedItems = browserSelectionModel.getSelectedItems();
        selectedItemsProperty.setAll(selectedItems.stream().map(TreeItem::getValue).toList());

        tagWithComment.disableProperty().set(userIdentity.isNull().get() ||
                selectedItemsProperty.size() != 1 ||
                (!selectedItemsProperty.get(0).getNodeType().equals(NodeType.SNAPSHOT) &&
                        !selectedItemsProperty.get(0).getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)));
        configureTagContextMenu(tagWithComment);

        addOptionalLoggingMenuItem();

        copyMenuItem.disableProperty().set(!mayCopy());
        compareSnapshotsMenuItem.disableProperty().set(!compareSnapshotsPossible());
        deleteNodeMenuItem.disableProperty().set(getUserIdentity().isNull().get() ||
                selectedItemsProperty.stream().anyMatch(n -> n.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) ||
                !hasSameParent());
        pasteMenuItem.disableProperty().set(!mayPaste());
    }

    /**
     * Adds a create log menu item based on current selection and logbook client availability
     */
    private void addOptionalLoggingMenuItem() {
        // If logbook has been configured, add the Create Log menu item
        if (LogbookPreferences.is_supported) {
            SelectionService.getInstance().setSelection(SaveAndRestoreApplication.NAME,
                    selectedItemsProperty.size() == 1 ? List.of(selectedItemsProperty.get(0)) : Collections.emptyList());
            List<ContextMenuEntry> supported = ContextMenuService.getInstance().listSupportedContextMenuEntries();

            supported.forEach(action -> {
                // Check if the menu item is already present
                Optional<MenuItem> menuItemOptional = contextMenu.getItems().stream()
                        .filter(mi -> mi.getText() != null && mi.getText().equals(action.getName())).findFirst();
                if (menuItemOptional.isEmpty()) {
                    contextMenu.getItems().add(new SeparatorMenuItem());
                    MenuItem menuItem = new MenuItem(action.getName(), new ImageView(action.getIcon()));
                    menuItem.setOnAction((ee) -> {
                        try {
                            action.call(null, SelectionService.getInstance().getSelection());
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Failed to execute " + action.getName() + " from save&restore", ex);
                        }
                    });
                    contextMenu.getItems().add(menuItem);
                }
            });
        }
    }

    @Override
    public void handleWebSocketMessage(SaveAndRestoreWebSocketMessage<?> saveAndRestoreWebSocketMessage) {
        switch (saveAndRestoreWebSocketMessage.messageType()) {
            case NODE_ADDED -> nodeAdded((String) saveAndRestoreWebSocketMessage.payload());
            case NODE_REMOVED -> nodeRemoved((String) saveAndRestoreWebSocketMessage.payload());
            case NODE_UPDATED -> nodeChanged((Node) saveAndRestoreWebSocketMessage.payload());
            case FILTER_ADDED_OR_UPDATED -> filterAddedOrUpdated((Filter) saveAndRestoreWebSocketMessage.payload());
            case FILTER_REMOVED -> filterRemoved((String) saveAndRestoreWebSocketMessage.payload());
        }
    }

    private void setupFilterActivators() {
        filterActivators.addAll(
                ServiceLoader.load(FilterActivator.class).stream().map(ServiceLoader.Provider::get).toList());
        filterActivators.forEach(a -> a.setCallbacks(this::activateFilter, this::deactivateFilter));
    }

    private Optional<Filter> findFilter(String filterName) {
        return filtersList.stream().filter(f -> f != null && f.getName().equals(filterName)).findFirst();
    }

    /**
     * Activates a {@link Filter}. If <code>filterName</code> does not match any of the available {@link Filter}s,
     * nothing happens except logging of the inconsistency.
     *
     * @param filterName The name of the {@link Filter} to select.
     */
    public void activateFilter(String filterName) {
        if (autoFilterActive.get()) {
            Optional<Filter> filterOptional = findFilter(filterName);
            if (filterOptional.isPresent()) {
                Platform.runLater(() -> filtersComboBox.getSelectionModel().select(filterOptional.get()));
            } else {
                logger.log(Level.WARNING, "Cannot activate filter as filter named \"" + filterName + "\" was not found.");
            }
        }
    }

    /**
     * If auto {@link Filter} activation is enabled and the active filter matches <code>filterName</code>, then
     * this method will switch to <code>no filter</code> but maintain auto activation.
     * @param filterName Name/id of the de-activated filter.
     */
    public void deactivateFilter(String filterName) {
        if (autoFilterActive.get()) {
            Filter currentlySelectedFilter = filtersComboBox.getSelectionModel().getSelectedItem();
            if (currentlySelectedFilter != null && currentlySelectedFilter.getName().equals(filterName)) {
                Platform.runLater(() -> filtersComboBox.getSelectionModel().select(null));
            }
        }
    }

    private void handleWebSocketConnected() {
        serviceConnected.setValue(true);
        loadInitialData();
    }

    private void handleWebSocketDisconnected() {
        serviceConnected.setValue(false);
        saveLocalState();
    }

    @Override
    public boolean doCloseCheck(){
        for(Tab tab : tabPane.getTabs()){
            if(!((SaveAndRestoreTab)tab).doCloseCheck()){
                return false;
            }
        }
        return true;
    }

    private void findReferences(){
        SearchAndFilterTab searchAndFilterTab = openSearchWindow();
        searchAndFilterTab.getController().findReferencesForSnapshot(treeView.getSelectionModel().getSelectedItems().get(0).getValue().getUniqueId());
    }
}
