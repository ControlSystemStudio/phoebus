/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.applications.saveandrestore.ui.search;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.ui.*;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagUtil;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.ListSelectionPopOver;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchAndFilterViewController extends SaveAndRestoreBaseController implements Initializable, FilterChangeListener {

    private final SaveAndRestoreController saveAndRestoreController;

    @FXML
    private TableColumn<Node, ImageView> typeColumn;

    @FXML
    private TableColumn<Node, String> nameColumn;

    @FXML
    private TableColumn<Node, String> commentColumn;

    @FXML
    private TableColumn<Node, String> tagsColumn;

    @FXML
    private TableColumn<Node, String> lastUpdatedColumn;

    @FXML
    private TableColumn<Node, String> createdColumn;

    @FXML
    private TableColumn<Node, String> userColumn;

    @FXML
    private TextField pageSizeTextField;

    @FXML
    private TextField filterNameTextField;

    @FXML
    private Button saveFilterButton;

    @FXML
    private TextField nodeNameTextField;

    @FXML
    private CheckBox nodeTypeFolderCheckBox;

    @FXML
    private CheckBox nodeTypeConfigurationCheckBox;

    @FXML
    private CheckBox nodeTypeSnapshotCheckBox;

    @FXML
    private CheckBox nodeTypeCompositeSnapshotCheckBox;

    @FXML
    private TextField tagsTextField;

    @FXML
    private TextField userTextField;

    @FXML
    private TextField descTextField;

    @FXML
    private TextField startTime;

    @FXML
    private TextField endTime;

    @FXML
    private CheckBox goldenOnlyCheckbox;

    @FXML
    private Label queryLabel;

    @FXML
    private Pagination pagination;

    @FXML
    private TableView<Node> resultTableView;

    @FXML
    private TableView<Filter> filterTableView;

    @FXML
    private TableColumn<Filter, String> filterNameColumn;

    @FXML
    private TableColumn<Filter, String> queryColumn;

    @FXML
    private TableColumn<Filter, String> filterLastUpdatedColumn;

    @FXML
    private TableColumn<Filter, String> filterUserColumn;

    @FXML
    private TableColumn<Filter, Filter> deleteColumn;

    @FXML
    private TextField pvsTextField;

    private final SimpleStringProperty filterNameProperty = new SimpleStringProperty();

    private final SaveAndRestoreService saveAndRestoreService;

    private final SimpleStringProperty query = new SimpleStringProperty();

    private final SimpleIntegerProperty hitCountProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty pageCountProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty pageSizeProperty =
            new SimpleIntegerProperty(Preferences.search_result_page_size);

    private final SimpleStringProperty pvNamesProperty = new SimpleStringProperty();

    private final ObservableList<Node> tableEntries = FXCollections.observableArrayList();

    private ListSelectionPopOver tagSearchPopover;

    private boolean searchDisabled = false;

    private final SimpleStringProperty nodeNameProperty = new SimpleStringProperty();

    private final SimpleBooleanProperty nodeTypeFolderProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty nodeTypeConfigurationProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty nodeTypeSnapshotProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty nodeTypeCompositeSnapshotProperty = new SimpleBooleanProperty();

    private final SimpleStringProperty tagsProperty = new SimpleStringProperty();
    private final SimpleStringProperty userProperty = new SimpleStringProperty();

    private final SimpleStringProperty startTimeProperty = new SimpleStringProperty();
    private final SimpleStringProperty endTimeProperty = new SimpleStringProperty();

    private final SimpleStringProperty descProperty = new SimpleStringProperty();

    private final SimpleBooleanProperty goldenOnlyProperty = new SimpleBooleanProperty();

    private static final Logger LOGGER = Logger.getLogger(SearchAndFilterViewController.class.getName());

    public SearchAndFilterViewController(SaveAndRestoreController saveAndRestoreController) {
        this.saveAndRestoreController = saveAndRestoreController;
        this.saveAndRestoreService = SaveAndRestoreService.getInstance();
    }

    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {

        resultTableView.getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());
        pagination.getStylesheets().add(this.getClass().getResource("/pagination.css").toExternalForm());
        resultTableView.getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());

        nodeNameTextField.textProperty().bindBidirectional(nodeNameProperty);
        nodeNameTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });
        nodeTypeFolderCheckBox.selectedProperty().bindBidirectional(nodeTypeFolderProperty);
        nodeTypeFolderCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });
        nodeTypeConfigurationCheckBox.selectedProperty().bindBidirectional(nodeTypeConfigurationProperty);
        nodeTypeConfigurationCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });
        nodeTypeSnapshotCheckBox.selectedProperty().bindBidirectional(nodeTypeSnapshotProperty);
        nodeTypeSnapshotCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });
        nodeTypeCompositeSnapshotCheckBox.selectedProperty().bindBidirectional(nodeTypeCompositeSnapshotProperty);
        nodeTypeCompositeSnapshotCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });

        goldenOnlyCheckbox.selectedProperty().bindBidirectional(goldenOnlyProperty);
        goldenOnlyCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });

        descTextField.textProperty().bindBidirectional(descProperty);
        descTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });
        userTextField.textProperty().bindBidirectional(userProperty);
        userTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });
        tagsTextField.textProperty().bindBidirectional(tagsProperty);
        tagsTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });

        pvsTextField.textProperty().bindBidirectional(pvNamesProperty);
        // NOTE: setOnKeyPressed will not work here as that is supposed to trigger the PV autocompletion
        // mechanism, which will consume the event.
        pvsTextField.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });

        PVAutocompleteMenu.INSTANCE.attachField(pvsTextField);

        startTime.textProperty().bindBidirectional(startTimeProperty);
        startTime.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });

        endTime.textProperty().bindBidirectional(endTimeProperty);
        endTime.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });

        tagSearchPopover = ListSelectionPopOver.create(
                (tags, popover) -> {
                    String tagsValue = String.join(",", tags);
                    tagsProperty.setValue(tagsValue);
                    if (popover.isShowing()) {
                        popover.hide();
                        updateParametersAndSearch();
                    }
                },
                (tags, popover) -> {
                    if (popover.isShowing()) {
                        popover.hide();
                    }
                }
        );

        filterTableView.getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());

        filterNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        filterNameColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        queryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getQueryString()));
        queryColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        filterLastUpdatedColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getLastUpdated().toInstant())));
        filterLastUpdatedColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        filterUserColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUser()));
        filterUserColumn.getStyleClass().add("leftAlignedTableColumnHeader");

        deleteColumn.setCellValueFactory(cellValue -> new SimpleObjectProperty<>(cellValue.getValue()));
        deleteColumn.setCellFactory(column -> new DeleteTableCell());

        // Use click event listener rather than selection listener: user might select filter and then change
        // the query. So when row is clicked again, the original query should take effect and "undo" changes.
        filterTableView.setRowFactory(tv -> {
            TableRow<Filter> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    setFilter(row.getItem());
                }
            });
            return row;
        });

        queryLabel.textProperty().bind(query);

        filterNameTextField.textProperty().bindBidirectional(filterNameProperty);
        filterNameTextField.disableProperty().bind(saveAndRestoreController.getUserIdentity().isNull());
        saveFilterButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                filterNameProperty.get() == null ||
                        filterNameProperty.get().isEmpty() ||
                        saveAndRestoreController.getUserIdentity().isNull().get(),
                filterNameProperty, saveAndRestoreController.getUserIdentity()));

        resultTableView.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(Node node, boolean empty) {
                super.updateItem(node, empty);
                if (node == null || empty) {
                    setTooltip(null);
                    setOnMouseClicked(null);
                } else {
                    setTooltip(new Tooltip(Messages.searchEntryToolTip));

                    setOnMouseClicked(action -> {
                        if (action.getClickCount() == 2) {
                            Stack<Node> copiedStack = new Stack<>();
                            DirectoryUtilities.CreateLocationStringAndNodeStack(node, false).getValue().forEach(copiedStack::push);
                            locateNode(copiedStack);
                        }
                    });
                }
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem tagGoldenMenuItem = new MenuItem(Messages.contextMenuTagAsGolden, new ImageView(ImageRepository.SNAPSHOT));

        ImageView snapshotTagsWithCommentIconImage = new ImageView(ImageRepository.SNAPSHOT_ADD_TAG_WITH_COMMENT);
        Menu tagMenu = new Menu(Messages.contextMenuTagsWithComment, snapshotTagsWithCommentIconImage);

        MenuItem addTagWithCommentMenuItem = TagWidget.AddTagWithCommentMenuItem();
        addTagWithCommentMenuItem.setOnAction(event -> TagUtil.addTag(resultTableView.getSelectionModel().getSelectedItems()));
        tagMenu.getItems().add(addTagWithCommentMenuItem);

        contextMenu.setOnShowing(event -> {
            TagUtil.tagWithComment(tagMenu,
                    resultTableView.getSelectionModel().getSelectedItems(),
                    updatedNodes -> { // Callback, any extra handling added here
                    });
            TagUtil.configureGoldenItem(resultTableView.getSelectionModel().getSelectedItems(), tagGoldenMenuItem);
        });

        contextMenu.getItems().addAll(tagGoldenMenuItem, tagMenu);


        resultTableView.setContextMenu(contextMenu);

        // Bind search result table to tableEntries observable
        Property<ObservableList<Node>> authorListProperty = new SimpleObjectProperty<>(tableEntries);
        resultTableView.itemsProperty().bind(authorListProperty);

        pageCountProperty.bind(Bindings.createIntegerBinding(() -> 1 + (hitCountProperty.get() / pageSizeProperty.get()),
                hitCountProperty, pageCountProperty));

        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getImageView(cell.getValue())));
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        nameColumn.getStyleClass().add("leftAlignedTableColumnHeader");

        commentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getDescription()));
        commentColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        createdColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getCreated().toInstant())));
        createdColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        lastUpdatedColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getLastModified().toInstant())));
        lastUpdatedColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        userColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUserName()));
        userColumn.getStyleClass().add("leftAlignedTableColumnHeader");

        tagsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getTags() == null ?
                "" :
                cell.getValue().getTags().stream().map(Tag::getName).filter(name -> !name.equals(Tag.GOLDEN)).collect(Collectors.joining(System.lineSeparator()))));
        tagsColumn.getStyleClass().add("leftAlignedTableColumnHeader");

        pageSizeTextField.setText(Integer.toString(pageSizeProperty.get()));
        Pattern DIGIT_PATTERN = Pattern.compile("\\d*");
        // This is to accept numerical input only, and at most 3 digits (maximizing search to 999 hits).
        pageSizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (DIGIT_PATTERN.matcher(newValue).matches()) {
                if (newValue.isEmpty()) {
                    pageSizeProperty.set(Preferences.search_result_page_size);
                } else if (newValue.length() > 3) {
                    pageSizeTextField.setText(oldValue);
                } else {
                    pageSizeProperty.set(Integer.parseInt(newValue));
                }
            } else {
                pageSizeTextField.setText(oldValue);
            }
        });

        pagination.currentPageIndexProperty().addListener((a, b, c) -> search());
        // Hide the pagination widget if hit count == 0 or page count < 2
        pagination.visibleProperty().bind(Bindings.createBooleanBinding(() -> hitCountProperty.get() > 0 && pagination.pageCountProperty().get() > 1,
                hitCountProperty, pagination.pageCountProperty()));
        pagination.pageCountProperty().bind(pageCountProperty);

        query.addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                clearSearch();
            } else {
                search();
            }
        });

        resultTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        resultTableView.setOnDragDetected(e -> {
            List<Node> selectedNodes = resultTableView.getSelectionModel().getSelectedItems();
            if (selectedNodes.stream().anyMatch(n ->
                    !n.getNodeType().equals(NodeType.SNAPSHOT) &&
                            !n.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT))) {
                return;
            }
            final ClipboardContent content = new ClipboardContent();
            final List<Node> nodes = new ArrayList<>(resultTableView.getSelectionModel().getSelectedItems());
            content.put(SaveAndRestoreApplication.NODE_SELECTION_FORMAT, nodes);
            final Dragboard db = resultTableView.startDragAndDrop(TransferMode.LINK);
            db.setContent(content);
            e.consume();
        });

        loadFilters();

        saveAndRestoreService.addFilterChangeListener(this);
    }

    private void setFilter(Filter filter) {
        query.set(filter.getQueryString());
        filterNameProperty.set(filter.getName());
        updatedQueryEditor();
    }

    public void locateNode(Stack<Node> stack) {
        saveAndRestoreController.locateNode(stack);
    }

    @FXML
    public void showHelp() {
        new HelpViewer().show();
    }

    @FXML
    public void saveFilter() {
        // Check if the filter name is already used.
        JobManager.schedule("Get All Filters", monitor -> {
            List<Filter> allFilters = saveAndRestoreService.getAllFilters();
            if (allFilters.stream().anyMatch(f -> filterNameProperty.get().equalsIgnoreCase(f.getName()))) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.CONFIRMATION);
                    alert.setTitle(Messages.saveFilter);
                    alert.setHeaderText(null);
                    alert.setContentText(MessageFormat.format(Messages.saveFilterConfirmOverwrite, filterNameProperty.get()));
                    ButtonType buttonTypeOverwrite = new ButtonType(Messages.overwrite);
                    ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(buttonTypeOverwrite, buttonTypeCancel);
                    Optional<ButtonType> overwrite = alert.showAndWait();
                    if (overwrite.isPresent() && overwrite.get() == buttonTypeOverwrite) {
                        saveFilterAndReload();
                    }
                });
            } else {
                saveFilterAndReload();
            }
        });
    }

    private void saveFilterAndReload() {
        Filter filter = new Filter();
        filter.setName(filterNameProperty.get());
        filter.setQueryString(queryLabel.getText());
        try {
            JobManager.schedule("Save Filter", monitor -> saveAndRestoreService.saveFilter(filter));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save filter." + (e.getMessage() != null ? ("Cause: " + e.getMessage()) : ""));
            Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(Messages.errorGeneric, Messages.failedSaveFilter, e));
        }
    }

    /**
     * Clears search result and search parameters, e.g. when selected filter is deleted or if
     * search parameters evaluate to an empty query string.
     */
    private void clearSearch() {
        hitCountProperty.set(0);
        tableEntries.clear();
        updatedQueryEditor();
    }

    public void search() {

        if (searchDisabled) {
            return;
        }

        Map<String, String> params =
                SearchQueryUtil.parseHumanReadableQueryString(query.get());

        params.put(Keys.FROM.getName(), Integer.toString(pagination.getCurrentPageIndex() * pageSizeProperty.get()));
        params.put(Keys.SIZE.getName(), Integer.toString(pageSizeProperty.get()));

        JobManager.schedule("Save-and-restore Search", monitor -> {
            MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
            params.forEach(map::add);
            try {
                SearchResult searchResult = saveAndRestoreService.search(map);
                if (searchResult.getHitCount() > 0) {
                    Platform.runLater(() -> {
                        tableEntries.setAll(searchResult.getNodes());
                        hitCountProperty.set(searchResult.getHitCount());
                    });
                } else {
                    Platform.runLater(tableEntries::clear);
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(
                        resultTableView,
                        Messages.errorGeneric,
                        Messages.searchErrorBody,
                        e
                );
                tableEntries.clear();
            }
        });
    }

    @FXML
    public void showTagsSelectionPopover() {
        if (tagSearchPopover.isShowing()) {
            tagSearchPopover.hide();
        } else {
            List<String> selectedTags = Arrays.stream(tagsProperty.getValueSafe().split(","))
                    .map(String::trim)
                    .filter(it -> !it.isEmpty())
                    .collect(Collectors.toList());
            List<String> availableTags = new ArrayList<>();
            try {
                List<String> tagNames = new ArrayList<>();
                SaveAndRestoreService.getInstance().getAllTags().forEach(tag -> {
                    if (!tagNames.contains(tag.getName()) && !tag.getName().equalsIgnoreCase(Tag.GOLDEN)) {
                        tagNames.add(tag.getName());
                    }
                });
                availableTags = tagNames
                        .stream()
                        .sorted(Comparator.comparing(String::toLowerCase))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to retrieve all tags from service");
            }
            tagSearchPopover.setAvailable(availableTags, selectedTags);
            tagSearchPopover.setSelected(selectedTags);
            tagSearchPopover.show(tagsTextField);
        }
    }

    private void updateParametersAndSearch() {
        if (searchDisabled) {
            return;
        }
        query.set(buildQueryString());
    }

    /**
     * Builds the query string based on editor input.
     *
     * @return A query string
     */
    private String buildQueryString() {
        Map<String, String> map = new HashMap<>();
        if (nodeNameProperty.get() != null && !nodeNameProperty.get().isEmpty()) {
            map.put(Keys.NAME.getName(), nodeNameProperty.get());
        }
        if (userProperty.get() != null && !userProperty.get().isEmpty()) {
            map.put(Keys.USER.getName(), userProperty.get());
        }
        if (descProperty.get() != null && !descProperty.get().isEmpty()) {
            map.put(Keys.DESC.getName(), descProperty.get());
        }
        if (tagsProperty.get() != null && !tagsProperty.get().isEmpty()) {
            map.put(Keys.TAGS.getName(), tagsProperty.get());
        }
        if (pvNamesProperty.get() != null && !pvNamesProperty.get().isEmpty()) {
            map.put(Keys.PVS.getName(), pvNamesProperty.get());
        }
        List<String> types = new ArrayList<>();
        if (nodeTypeFolderProperty.get()) {
            types.add(NodeType.FOLDER.name().toLowerCase());
        }
        if (nodeTypeConfigurationProperty.get()) {
            types.add(NodeType.CONFIGURATION.name().toLowerCase());
        }
        if (nodeTypeSnapshotProperty.get()) {
            types.add(NodeType.SNAPSHOT.name().toLowerCase());
        }
        if (nodeTypeCompositeSnapshotProperty.get()) {
            types.add(NodeType.COMPOSITE_SNAPSHOT.name().toLowerCase());
        }
        if (!types.isEmpty()) {
            map.put(Keys.TYPE.getName(), String.join(",", types));
        }
        if (startTimeProperty.get() != null && !startTimeProperty.get().isEmpty()) {
            map.put(Keys.STARTTIME.getName(), startTimeProperty.get());
        }
        if (endTimeProperty.get() != null && !endTimeProperty.get().isEmpty()) {
            map.put(Keys.ENDTIME.getName(), endTimeProperty.get());
        }
        if (goldenOnlyProperty.get()) {
            String tags = map.get(Keys.TAGS.getName());
            if (tags == null) {
                map.put(Keys.TAGS.getName(), Tag.GOLDEN);
            } else if (!tags.toLowerCase().contains(Tag.GOLDEN.toLowerCase())) {
                map.put(Keys.TAGS.getName(), tags + "," + Tag.GOLDEN);
            }
        }
        return SearchQueryUtil.toQueryString(map);
    }

    private void clearFilter(Filter filter) {
        if (filterNameProperty.get() != null && filterNameProperty.get().equals(filter.getName())) {
            filterNameProperty.set(null);
            query.set(null);
        }
    }

    private void loadFilters() {
        try {
            List<Filter> filters = saveAndRestoreService.getAllFilters();
            filterTableView.getItems().setAll(filters);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load filters", e);
        }
    }

    private class DeleteTableCell extends TableCell<Filter, Filter> {
        @Override
        protected void updateItem(final Filter filter, final boolean empty) {
            super.updateItem(filter, empty);
            // If user clicks on the delete column cell, consume the mouse event to prevent the filter from being loaded.
            setOnMouseClicked(Event::consume);
            if (empty) {
                setGraphic(null);
            } else {
                Button button = new Button();
                button.setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
                button.setTooltip(new Tooltip(Messages.deleteFilter));
                button.setOnAction(event -> {
                    try {
                        saveAndRestoreService.deleteFilter(filter);
                        clearFilter(filter);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Failed to delete filter", e);
                        ExceptionDetailsErrorDialog.openError(Messages.errorGeneric, Messages.faildDeleteFilter, e);
                    }
                });
                button.disableProperty().bind(saveAndRestoreController.getUserIdentity().isNull());
                setGraphic(button);
            }
        }
    }

    private ImageView getImageView(Node node) {
        switch (node.getNodeType()) {
            case SNAPSHOT:
                if (node.hasTag(Tag.GOLDEN)) {
                    return new ImageView(ImageRepository.GOLDEN_SNAPSHOT);
                } else {
                    return new ImageView(ImageRepository.SNAPSHOT);
                }
            case COMPOSITE_SNAPSHOT:
                return new ImageView(ImageRepository.COMPOSITE_SNAPSHOT);
            case FOLDER:
                return new ImageView(ImageRepository.FOLDER);
            case CONFIGURATION:
                return new ImageView(ImageRepository.CONFIGURATION);
        }
        return null;
    }

    /**
     * Updates the query editor UI components based on the current query string
     */
    private void updatedQueryEditor() {

        searchDisabled = true;

        Map<String, String> searchParams = SearchQueryUtil.parseHumanReadableQueryString(query.get());
        nodeNameProperty.set(searchParams.get(Keys.NAME.getName()));
        userProperty.set(searchParams.get(Keys.USER.getName()));
        descProperty.set(searchParams.get(Keys.DESC.getName()));
        // Add tags, but exclude "golden"
        String tagsString = searchParams.get(Keys.TAGS.getName());
        if (tagsString != null) {
            List<String> tags = Arrays.asList(tagsString.trim().split(","));
            tagsProperty.set(tags.stream().filter(s -> !s.equals(Tag.GOLDEN)).collect(Collectors.joining(",")));
        } else {
            tagsProperty.set(null);
        }
        startTimeProperty.set(searchParams.get(Keys.STARTTIME.getName()));
        endTimeProperty.set(searchParams.get(Keys.ENDTIME.getName()));

        String typeValue = searchParams.get(Keys.TYPE.getName());
        nodeTypeFolderProperty.set(false);
        nodeTypeConfigurationProperty.set(false);
        nodeTypeSnapshotProperty.set(false);
        nodeTypeCompositeSnapshotProperty.set(false);
        if (typeValue != null && !typeValue.isEmpty()) {
            String[] types = typeValue.split(",");
            for (String type : types) {
                if (type.equalsIgnoreCase(NodeType.FOLDER.name())) {
                    nodeTypeFolderProperty.set(true);
                }
                if (type.equalsIgnoreCase(NodeType.CONFIGURATION.name())) {
                    nodeTypeConfigurationProperty.set(true);
                }
                if (type.equalsIgnoreCase(NodeType.SNAPSHOT.name())) {
                    nodeTypeSnapshotProperty.set(true);
                }
                if (type.equalsIgnoreCase(NodeType.COMPOSITE_SNAPSHOT.name())) {
                    nodeTypeCompositeSnapshotProperty.set(true);
                }
            }
        }
        goldenOnlyProperty.set(searchParams.get(Keys.TAGS.getName()) != null &&
                searchParams.get(Keys.TAGS.getName()).contains(Tag.GOLDEN));

        searchDisabled = false;
    }

    public void nodeChanged(Node updatedNode) {
        for (Node node : resultTableView.getItems()) {
            if (node.getUniqueId().equals(updatedNode.getUniqueId())) {
                node.setTags(updatedNode.getTags());
                resultTableView.refresh();
            }
        }
    }

    @Override
    public void filterAddedOrUpdated(Filter filter) {
        loadFilters();
    }

    @Override
    public void filterRemoved(Filter filter) {
        loadFilters();
    }

    public void handleSaveAndFilterTabClosed() {
        saveAndRestoreService.removeFilterChangeListener(this);
    }
}
