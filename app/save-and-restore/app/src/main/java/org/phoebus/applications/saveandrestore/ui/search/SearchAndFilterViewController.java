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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;
import org.phoebus.applications.saveandrestore.ui.FilterChangeListener;
import org.phoebus.applications.saveandrestore.ui.HelpViewer;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.ListSelectionPopOver;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SearchAndFilterViewController extends SaveAndRestoreBaseController implements Initializable, FilterChangeListener {

    private final SaveAndRestoreController saveAndRestoreController;

    @SuppressWarnings("unused")
    @FXML
    private javafx.scene.Node mainUi;

    @SuppressWarnings("unused")
    @FXML
    private TextField filterNameTextField;

    @SuppressWarnings("unused")
    @FXML
    private Button saveFilterButton;

    @SuppressWarnings("unused")
    @FXML
    private TextField uniqueIdTextField;

    @SuppressWarnings("unused")
    @FXML
    private TextField nodeNameTextField;

    @SuppressWarnings("unused")
    @FXML
    private CheckBox nodeTypeFolderCheckBox;

    @SuppressWarnings("unused")
    @FXML
    private CheckBox nodeTypeConfigurationCheckBox;

    @SuppressWarnings("unused")
    @FXML
    private CheckBox nodeTypeSnapshotCheckBox;

    @SuppressWarnings("unused")
    @FXML
    private CheckBox nodeTypeCompositeSnapshotCheckBox;

    @SuppressWarnings("unused")
    @FXML
    private TextField tagsTextField;

    @SuppressWarnings("unused")
    @FXML
    private TextField userTextField;

    @SuppressWarnings("unused")
    @FXML
    private TextField descTextField;

    @SuppressWarnings("unused")
    @FXML
    private TextField startTime;

    @SuppressWarnings("unused")
    @FXML
    private TextField endTime;

    @SuppressWarnings("unused")
    @FXML
    private CheckBox goldenOnlyCheckbox;

    @SuppressWarnings("unused")
    @FXML
    private Label queryLabel;

    @SuppressWarnings("unused")
    @FXML
    private TableView<Filter> filterTableView;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Filter, String> filterNameColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Filter, String> queryColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Filter, String> filterLastUpdatedColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Filter, String> filterUserColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Filter, Filter> deleteColumn;

    @SuppressWarnings("unused")
    @FXML
    private TextField pvsTextField;

    @SuppressWarnings("unused")
    @FXML
    private VBox progressIndicator;

    private final SimpleStringProperty filterNameProperty = new SimpleStringProperty();

    private final SaveAndRestoreService saveAndRestoreService;

    private final SimpleStringProperty query = new SimpleStringProperty();

    private final SimpleStringProperty pvNamesProperty = new SimpleStringProperty();

    private ListSelectionPopOver tagSearchPopover;

    private boolean searchDisabled = false;

    private final SimpleStringProperty uniqueIdProperty = new SimpleStringProperty();
    private final SimpleStringProperty nodeNameProperty = new SimpleStringProperty();

    private final SimpleBooleanProperty nodeTypeFolderProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty nodeTypeConfigurationProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty nodeTypeSnapshotProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty nodeTypeCompositeSnapshotProperty = new SimpleBooleanProperty();

    private final SimpleStringProperty tagsProperty = new SimpleStringProperty();
    private final SimpleStringProperty userNameProperty = new SimpleStringProperty();

    private final SimpleStringProperty startTimeProperty = new SimpleStringProperty();
    private final SimpleStringProperty endTimeProperty = new SimpleStringProperty();

    private final SimpleStringProperty descProperty = new SimpleStringProperty();

    private final SimpleBooleanProperty goldenOnlyProperty = new SimpleBooleanProperty();

    private static final Logger LOGGER = Logger.getLogger(SearchAndFilterViewController.class.getName());

    private final SimpleBooleanProperty disableUi = new SimpleBooleanProperty();

    @SuppressWarnings("unused")
    @FXML
    private SearchResultTableViewController searchResultTableViewController;

    public SearchAndFilterViewController(SaveAndRestoreController saveAndRestoreController) {
        this.saveAndRestoreController = saveAndRestoreController;
        this.saveAndRestoreService = SaveAndRestoreService.getInstance();
    }

    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {

        uniqueIdTextField.textProperty().bindBidirectional(uniqueIdProperty);
        uniqueIdTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                LOGGER.log(Level.INFO, "ENTER has been pressed in uniqueIdTextField");
                LOGGER.log(Level.INFO, "uniqueIdProperty: " + uniqueIdProperty.getValueSafe());
                if (uniqueIdProperty.isEmpty().get()) {
                    LOGGER.log(Level.INFO, "uniqueIdString: is empty");
                } else {
                    searchResultTableViewController.uniqueIdSearch(uniqueIdProperty.getValueSafe());
                }
            }
        });
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
        userTextField.textProperty().bindBidirectional(userNameProperty);
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
            filterNameProperty.isEmpty().get() ||
                saveAndRestoreController.getUserIdentity().isNull().get() ||
                uniqueIdProperty.isNotEmpty().get(),
            filterNameProperty, saveAndRestoreController.getUserIdentity(), uniqueIdProperty));

        query.addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                clearSearch();
            } else {
                searchResultTableViewController.search(query.get());
            }
        });

        loadFilters();

        saveAndRestoreService.addFilterChangeListener(this);

        progressIndicator.visibleProperty().bind(disableUi);
        disableUi.addListener((observable, oldValue, newValue) -> mainUi.setDisable(newValue));
    }

    public void setFilter(Filter filter) {
        query.set(filter.getQueryString());
        filterNameProperty.set(filter.getName());
        filterTableView.getSelectionModel().select(filter);
        updatedQueryEditor();
    }

    @SuppressWarnings("unused")
    @FXML
    public void showHelp() {
        new HelpViewer().show();
    }

    @SuppressWarnings("unused")
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
        searchResultTableViewController.clearTable();
        updatedQueryEditor();
    }

    @SuppressWarnings("unused")
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
        if (userNameProperty.get() != null && !userNameProperty.get().isEmpty()) {
            map.put(Keys.USER.getName(), userNameProperty.get());
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
                        ExceptionDetailsErrorDialog.openError(Messages.errorGeneric, Messages.failedDeleteFilter, e);
                    }
                });
                button.disableProperty().bind(saveAndRestoreController.getUserIdentity().isNull());
                setGraphic(button);
            }
        }
    }

    /**
     * Updates the query editor UI components based on the current query string
     */
    private void updatedQueryEditor() {

        searchDisabled = true;

        Map<String, String> searchParams = SearchQueryUtil.parseHumanReadableQueryString(query.get());
        nodeNameProperty.set(searchParams.get(Keys.NAME.getName()));
        userNameProperty.set(searchParams.get(Keys.USER.getName()));
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
        searchResultTableViewController.nodeChanged(updatedNode);
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

    @Override
    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens){
        super.secureStoreChanged(validTokens);
        searchResultTableViewController.secureStoreChanged(validTokens);
    }
}
