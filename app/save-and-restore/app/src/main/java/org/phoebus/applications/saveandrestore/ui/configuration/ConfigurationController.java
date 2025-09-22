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

package org.phoebus.applications.saveandrestore.ui.configuration;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.ComparisonMode;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.websocket.MessageType;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.WebSocketMessageHandler;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.FocusUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfigurationController extends SaveAndRestoreBaseController implements WebSocketMessageHandler {

    @FXML
    @SuppressWarnings("unused")
    private BorderPane borderPane;

    @FXML
    @SuppressWarnings("unused")
    private TableColumn<ConfigPvEntry, String> pvNameColumn;

    @FXML
    @SuppressWarnings("unused")
    private TableColumn<ConfigPvEntry, String> readbackPvNameColumn;

    @FXML
    @SuppressWarnings("unused")
    private TableColumn<ConfigPvEntry, ComparisonMode> comparisonModeColumn;

    @FXML
    @SuppressWarnings("unused")
    private TableColumn<ConfigPvEntry, Double> toleranceColumn;

    @FXML
    @SuppressWarnings("unused")
    private TableColumn<ConfigPvEntry, Boolean> readOnlyColumn;

    @FXML
    @SuppressWarnings("unused")
    private TableView<ConfigPvEntry> pvTable;

    @FXML
    @SuppressWarnings("unused")
    private TextArea descriptionTextArea;

    @FXML
    @SuppressWarnings("unused")
    private Button saveButton;

    @FXML
    @SuppressWarnings("unused")
    private TextField pvNameField;

    @FXML
    @SuppressWarnings("unused")
    private TextField readbackPvNameField;

    @FXML
    @SuppressWarnings("unused")
    private Button addPvButton;

    @FXML
    @SuppressWarnings("unused")
    private CheckBox readOnlyCheckBox;

    @FXML
    private final SimpleStringProperty pvNameProperty = new SimpleStringProperty("");
    @FXML
    private final SimpleStringProperty readbackPvNameProperty = new SimpleStringProperty("");

    @FXML
    private final SimpleBooleanProperty readOnlyProperty = new SimpleBooleanProperty(false);

    @FXML
    @SuppressWarnings("unused")
    private TextField configurationNameField;
    @FXML
    @SuppressWarnings("unused")
    private Label configurationCreatedDateField;

    @FXML
    @SuppressWarnings("unused")
    private Label configurationLastModifiedDateField;
    @FXML
    @SuppressWarnings("unused")
    private Label createdByField;

    @FXML
    @SuppressWarnings("unused")
    private Pane addPVsPane;

    private final ObservableList<ConfigPvEntry> configurationEntries = FXCollections.observableArrayList();

    private final SimpleBooleanProperty selectionEmpty = new SimpleBooleanProperty(false);
    private final SimpleStringProperty configurationDescriptionProperty = new SimpleStringProperty();
    private final SimpleStringProperty configurationNameProperty = new SimpleStringProperty();
    private Node configurationNodeParent;

    private final Logger logger = Logger.getLogger(ConfigurationController.class.getName());

    private final BooleanProperty dirty = new SimpleBooleanProperty();

    private final SimpleStringProperty tabTitleProperty = new SimpleStringProperty();

    /**
     * Manages the id of the containing {@link javafx.scene.control.Tab}. This property will
     * also indicate if the UI has been configured to edit a new or existing configuration: for a new configuration
     * the id is <code>null</code>.
     */
    private final SimpleStringProperty tabIdProperty = new SimpleStringProperty();

    private ChangeListener<String> nodeNameChangeListener;
    private ChangeListener<String> descriptionChangeListener;

    public ConfigurationController(ConfigurationTab configurationTab) {
        configurationTab.textProperty().bind(tabTitleProperty);
        configurationTab.idProperty().bind(tabIdProperty);
    }

    @FXML
    public void initialize() {

        pvTable.editableProperty().bind(userIdentity.isNull().not());
        pvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        pvTable.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> selectionEmpty.set(nv == null));

        MenuItem deleteMenuItem = new MenuItem(Messages.menuItemDeleteSelectedPVs,
                new ImageView(ImageCache.getImage(ConfigurationController.class, "/icons/delete.png")));
        deleteMenuItem.setOnAction(ae -> {
            configurationEntries.removeAll(pvTable.getSelectionModel().getSelectedItems());
            dirty.setValue(true);
        });

        deleteMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() -> pvTable.getSelectionModel().getSelectedItems().isEmpty()
                        || userIdentity.isNull().get(),
                pvTable.getSelectionModel().getSelectedItems(), userIdentity));

        ContextMenu contextMenu = new ContextMenu();
        pvTable.setOnContextMenuRequested(event -> {
            contextMenu.getItems().clear();
            contextMenu.getItems().addAll(deleteMenuItem);
            contextMenu.getItems().add(new SeparatorMenuItem());
            ObservableList<ConfigPvEntry> selectedPVs = pvTable.getSelectionModel().getSelectedItems();
            if (!selectedPVs.isEmpty()) {
                List<ProcessVariable> selectedPVList = selectedPVs.stream()
                        .map(tableEntry -> new ProcessVariable(tableEntry.getPvNameProperty().get()))
                        .collect(Collectors.toList());
                SelectionService.getInstance().setSelection(SaveAndRestoreApplication.NAME, selectedPVList);
                ContextMenuHelper.addSupportedEntries(FocusUtil.setFocusOn(pvTable), contextMenu);
            }
        });

        pvTable.setContextMenu(contextMenu);

        pvNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        pvNameColumn.setCellValueFactory(cell -> cell.getValue().getPvNameProperty());
        pvNameColumn.setOnEditCommit(t -> {
            t.getTableView().getItems().get(t.getTablePosition().getRow()).setPvNameProperty(t.getNewValue());
            dirty.setValue(true);
        });

        readbackPvNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        readbackPvNameColumn.setCellValueFactory(cell -> cell.getValue().getReadBackPvNameProperty());
        readbackPvNameColumn.setOnEditCommit(t -> {
            t.getTableView().getItems().get(t.getTablePosition().getRow()).setReadBackPvNameProperty(t.getNewValue());
            dirty.setValue(true);
        });

        readOnlyColumn.setCellFactory(CheckBoxTableCell.forTableColumn(readOnlyColumn));
        readOnlyColumn.setCellValueFactory(cell -> {
            BooleanProperty readOnly = cell.getValue().getReadOnlyProperty();
            readOnly.addListener((obs, o, n) -> dirty.setValue(true));
            return readOnly;
        });

        comparisonModeColumn.setCellValueFactory(cell -> cell.getValue().getComparisonModeProperty());
        comparisonModeColumn.setCellFactory(callback -> {
            ObservableList<ComparisonMode> values = FXCollections.observableArrayList(Arrays.stream(ComparisonMode.values()).toList());
            values.add(0, null);
            ComboBoxTableCell<ConfigPvEntry, ComparisonMode> tableCell = new ComboBoxTableCell<>(values) {

                @Override
                public void commitEdit(ComparisonMode comparisonMode) {
                    ComparisonMode currentMode = getTableView().getItems().get(getIndex()).getComparisonModeProperty().get();
                    getTableView().getItems().get(getIndex()).setComparisonModeProperty(comparisonMode);
                    if (comparisonMode == null) {
                        getTableView().getItems().get(getIndex()).setToleranceProperty(null);
                    }
                    // User has selected a mode that was previously null -> set tolerance to a default value.
                    else if (currentMode == null) {
                        getTableView().getItems().get(getIndex()).setToleranceProperty(0.0);
                    }
                    dirty.setValue(true);
                    super.commitEdit(comparisonMode);
                }
            };

            StringConverter<ComparisonMode> converter = new StringConverter<>() {
                @Override
                public String toString(ComparisonMode object) {
                    if (object == null) {
                        return "";
                    }
                    return object.toString();
                }

                @Override
                public ComparisonMode fromString(String string) {
                    if (string == null) {
                        return null;
                    }
                    return ComparisonMode.valueOf(string);
                }
            };
            tableCell.setConverter(converter);
            return tableCell;
        });

        toleranceColumn.setCellFactory(callback -> new TextFieldTableCell<>(new DoubleStringConverter() {
            @Override
            public String toString(Double value) {
                return value == null ? null : value.toString();
            }

            @Override
            public Double fromString(String string) {
                try {
                    double value = Double.parseDouble(string);
                    if (value >= 0) {
                        // Tolerance must be >= 0.
                        return value;
                    }
                    return null;
                } catch (Exception e) {
                    // No logging needed: user has entered text that cannot be parsed as double.
                    return null;
                }
            }
        }) {
            @Override
            public void commitEdit(Double value) {
                if (value == null) {
                    return;
                }
                getTableView().getItems().get(getIndex()).setToleranceProperty(value);
                dirty.setValue(true);
                super.commitEdit(value);
            }
        });
        toleranceColumn.setCellValueFactory(cell -> cell.getValue().getToleranceProperty());

        pvNameField.textProperty().bindBidirectional(pvNameProperty);
        readbackPvNameField.textProperty().bindBidirectional(readbackPvNameProperty);
        configurationNameField.textProperty().bindBidirectional(configurationNameProperty);
        configurationNameField.disableProperty().bind(userIdentity.isNull());
        descriptionTextArea.textProperty().bindBidirectional(configurationDescriptionProperty);
        descriptionTextArea.disableProperty().bind(userIdentity.isNull());

        configurationEntries.addListener((ListChangeListener<ConfigPvEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    FXCollections.sort(configurationEntries);
                }
            }
        });
        pvTable.setItems(configurationEntries);

        dirty.addListener((obs, o, n) -> {
            if (n && !tabTitleProperty.get().startsWith("* ")) {
                Platform.runLater(() -> tabTitleProperty.setValue("* " + tabTitleProperty.get()));
            } else if (!n && tabTitleProperty.get().startsWith("* ")) {
                Platform.runLater(() -> tabTitleProperty.setValue(tabTitleProperty.get().substring(2)));
            }
        });

        nodeNameChangeListener = (observableValue, oldValue, newValue) -> dirty.setValue(true);
        descriptionChangeListener = (observableValue, oldValue, newValue) -> dirty.setValue(true);

        saveButton.disableProperty().bind(Bindings.createBooleanBinding(() -> dirty.not().get() ||
                        (!Preferences.allow_empty_descriptions && configurationDescriptionProperty.isEmpty().get()) ||
                        configurationNameProperty.isEmpty().get() ||
                        userIdentity.isNull().get(),
                dirty, configurationDescriptionProperty, configurationNameProperty, userIdentity));

        addPvButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        pvNameField.textProperty().isEmpty().get() &&
                                readbackPvNameField.textProperty().isEmpty().get(),
                pvNameField.textProperty(), readbackPvNameField.textProperty()));

        readOnlyCheckBox.selectedProperty().bindBidirectional(readOnlyProperty);

        addPVsPane.disableProperty().bind(userIdentity.isNull());

        webSocketClientService.addWebSocketMessageHandler(this);
    }

    @FXML
    @SuppressWarnings("unused")
    public void saveConfiguration() {
        JobManager.schedule("Save save&restore configuration", monitor -> {
            try {
                Node configurationNode =
                        Node.builder().nodeType(NodeType.CONFIGURATION)
                                .name(configurationNameProperty.get())
                                .description(configurationDescriptionProperty.get())
                                .uniqueId(tabIdProperty.get())
                                .build();
                ConfigurationData configurationData = new ConfigurationData();
                configurationData.setPvList(configurationEntries.stream().map(ConfigPvEntry::toConfigPv).toList());
                configurationData.setUniqueId(tabIdProperty.get());
                Configuration configuration = new Configuration();
                configuration.setConfigurationNode(configurationNode);
                configuration.setConfigurationData(configurationData);
                if (tabIdProperty.get() == null) { // New configuration
                    configuration = saveAndRestoreService.createConfiguration(configurationNodeParent,
                            configuration);
                    tabIdProperty.setValue(configuration.getConfigurationNode().getUniqueId());
                } else {
                    configuration = saveAndRestoreService.updateConfiguration(configuration);
                }
                loadConfiguration(configuration.getConfigurationNode());
                dirty.setValue(false);
            } catch (Exception e1) {
                Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(pvTable,
                        Messages.errorActionFailed,
                        Messages.errorCreateConfigurationFailed,
                        e1));
            }
        });
    }

    @FXML
    @SuppressWarnings("unused")
    public void addPv() {

        Platform.runLater(() -> {
            // Process a list of space or semicolon separated pvs
            String[] pvNames = pvNameProperty.get().trim().split("[\\s;]+");
            String[] readbackPvNames = readbackPvNameProperty.get().trim().split("[\\s;]+");

            if (!checkForDuplicatePvNames(pvNames)) {
                return;
            }

            ArrayList<ConfigPvEntry> configPVs = new ArrayList<>();
            for (int i = 0; i < pvNames.length; i++) {
                String pvName = pvNames[i].trim();
                String readbackPV = i >= readbackPvNames.length ? null : readbackPvNames[i] == null || readbackPvNames[i].isEmpty() ? null : readbackPvNames[i].trim();
                ConfigPv configPV = ConfigPv.builder()
                        .pvName(pvName)
                        .readOnly(readOnlyProperty.get())
                        .readbackPvName(readbackPV)
                        .build();
                configPVs.add(new ConfigPvEntry(configPV));
            }
            configurationEntries.addAll(configPVs);
            dirty.setValue(true);
            resetAddPv();
        });
    }

    /**
     * Checks that added PV names are not added multiple times
     *
     * @param addedPvNames New PV names added in the UI
     * @return <code>true</code> if no duplicates are detected, otherwise <code>false</code>
     */
    private boolean checkForDuplicatePvNames(String[] addedPvNames) {
        List<String> pvNamesAsList = new ArrayList<>(Arrays.asList(addedPvNames));
        pvTable.itemsProperty().get().forEach(i -> pvNamesAsList.add(i.getPvNameProperty().get()));
        List<String> duplicatePvNames = pvNamesAsList.stream().filter(n -> Collections.frequency(pvNamesAsList, n) > 1).toList();

        if (!duplicatePvNames.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(Messages.duplicatePVNamesNotSupported);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    private void resetAddPv() {
        pvNameProperty.set("");
        readOnlyProperty.set(false);
        readbackPvNameProperty.set("");

        pvNameField.requestFocus();
    }

    /**
     * Configures the controller to create a new configuration.
     *
     * @param parentNode The parent {@link Node} for the new configuration, i.e. must be a
     *                   {@link Node} of type {@link NodeType#FOLDER}.
     */
    public void newConfiguration(Node parentNode) {
        configurationNodeParent = parentNode;
        addListeners();
        tabTitleProperty.setValue(Messages.unnamedConfiguration);
        Platform.runLater(() -> configurationNameField.requestFocus());
    }

    /**
     * Loads an existing configuration {@link Node} and its associated {@link ConfigurationData}.
     *
     * @param node An existing {@link Node} of type {@link NodeType#CONFIGURATION}.
     */
    public void loadConfiguration(final Node node) {
        removeListeners();
        JobManager.schedule("Load save&restore configuration", monitor -> {
            final ConfigurationData configurationData;
            try {
                configurationData = saveAndRestoreService.getConfiguration(node.getUniqueId());
            } catch (Exception e) {
                Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(borderPane, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e));
                return;
            }

            Platform.runLater(() -> {
                try {
                    tabTitleProperty.setValue(node.getName());
                    tabIdProperty.setValue(node.getUniqueId());
                    Collections.sort(configurationData.getPvList());
                    configurationEntries.setAll(configurationData.getPvList().stream().map(ConfigPvEntry::new).toList());
                    configurationNameProperty.set(node.getName());
                    configurationCreatedDateField.textProperty().set(node.getCreated() != null ?
                            TimestampFormats.SECONDS_FORMAT.format(Instant.ofEpochMilli(node.getCreated().getTime())) : null);
                    configurationLastModifiedDateField.textProperty().set(node.getLastModified() != null ?
                            TimestampFormats.SECONDS_FORMAT.format(Instant.ofEpochMilli(node.getLastModified().getTime())) : null);
                    createdByField.textProperty().set(node.getUserName());
                    configurationDescriptionProperty.set(node.getDescription());
                    dirty.setValue(false);
                    addListeners();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to load existing configuration");
                }
            });
        });

    }

    /**
     * A check is made if content is dirty, in which case user is prompted to cancel or close anyway.
     *
     * @return <code>true</code> if content is not dirty or user chooses to close anyway,
     * otherwise <code>false</code>.
     */
    @Override
    public boolean doCloseCheck() {
        if (dirty.get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.closeConfigurationTabPrompt);
            alert.setContentText(Messages.closeConfigurationWarning);
            DialogHelper.positionDialog(alert, borderPane, -200, -200);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get().equals(ButtonType.OK);
        }

        return true;
    }

    @Override
    public void handleTabClosed(){
        webSocketClientService.removeWebSocketMessageHandler(this);
    }

    @Override
    public void handleWebSocketMessage(SaveAndRestoreWebSocketMessage<?> saveAndRestoreWebSocketMessage) {
        if (saveAndRestoreWebSocketMessage.messageType().equals(MessageType.NODE_UPDATED)) {
            Node node = (Node) saveAndRestoreWebSocketMessage.payload();
            if (tabIdProperty.get() != null && node.getUniqueId().equals(tabIdProperty.get())) {
                loadConfiguration(node);
            }
        }
    }

    private void addListeners() {
        configurationNameProperty.addListener(nodeNameChangeListener);
        configurationDescriptionProperty.addListener(descriptionChangeListener);
    }

    private void removeListeners() {
        configurationNameProperty.removeListener(nodeNameChangeListener);
        configurationDescriptionProperty.removeListener(descriptionChangeListener);
    }
}
