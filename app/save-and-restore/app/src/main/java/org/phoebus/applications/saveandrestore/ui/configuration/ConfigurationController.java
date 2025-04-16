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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.ComparisonMode;
import org.phoebus.applications.saveandrestore.ui.NodeChangedListener;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.FocusUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfigurationController extends SaveAndRestoreBaseController implements NodeChangedListener {

    @FXML
    @SuppressWarnings("unused")
    private BorderPane root;

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

    private SaveAndRestoreService saveAndRestoreService;

    private static final Executor UI_EXECUTOR = Platform::runLater;

    private final ObservableList<ConfigPvEntry> configurationEntries = FXCollections.observableArrayList();

    private final SimpleBooleanProperty selectionEmpty = new SimpleBooleanProperty(false);
    private final SimpleStringProperty configurationDescriptionProperty = new SimpleStringProperty();
    private final SimpleStringProperty configurationNameProperty = new SimpleStringProperty();
    private Node configurationNodeParent;

    private final SimpleObjectProperty<Node> configurationNode = new SimpleObjectProperty<>();

    private final ConfigurationTab configurationTab;

    private ConfigurationData configurationData;

    private final Logger logger = Logger.getLogger(ConfigurationController.class.getName());

    private final BooleanProperty loadInProgress = new SimpleBooleanProperty();
    private final BooleanProperty dirty = new SimpleBooleanProperty();

    public ConfigurationController(ConfigurationTab configurationTab) {
        this.configurationTab = configurationTab;
    }

    @FXML
    public void initialize() {

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        dirty.addListener((obs, o, n) -> configurationTab.annotateDirty(n));

        pvTable.editableProperty().bind(userIdentity.isNull().not());
        pvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        pvTable.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> selectionEmpty.set(nv == null));

        MenuItem deleteMenuItem = new MenuItem(Messages.menuItemDeleteSelectedPVs,
                new ImageView(ImageCache.getImage(ConfigurationController.class, "/icons/delete.png")));
        deleteMenuItem.setOnAction(ae -> {
            configurationEntries.removeAll(pvTable.getSelectionModel().getSelectedItems());
            //configurationTab.annotateDirty(true);
            pvTable.refresh();
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
            setDirty(true);
        });

        readbackPvNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        readbackPvNameColumn.setCellValueFactory(cell -> cell.getValue().getReadBackPvNameProperty());
        readbackPvNameColumn.setOnEditCommit(t -> {
            t.getTableView().getItems().get(t.getTablePosition().getRow()).setReadBackPvNameProperty(t.getNewValue());
            setDirty(true);
        });

        readOnlyColumn.setCellFactory(CheckBoxTableCell.forTableColumn(readOnlyColumn));
        readOnlyColumn.setCellValueFactory(cell -> {
            BooleanProperty readOnly = cell.getValue().getReadOnlyProperty();
            readOnly.addListener((obs, o, n) -> setDirty(true));
            return readOnly;
        });

        comparisonModeColumn.setCellValueFactory(cell -> cell.getValue().getComparisonModeProperty());
        comparisonModeColumn.setCellFactory(callback -> {
            ObservableList<ComparisonMode> values = FXCollections.observableArrayList(Arrays.stream(ComparisonMode.values()).toList());
            values.add(0, null);
            ComboBoxTableCell<ConfigPvEntry, ComparisonMode> tableCell = new ComboBoxTableCell<>(values) {

                @Override
                public void commitEdit(ComparisonMode comparisonMode) {
                    getTableView().getItems().get(getIndex()).setComparisonModeProperty(comparisonMode);
                    if (comparisonMode == null) {
                        getTableView().getItems().get(getIndex()).setToleranceProperty(null);
                    }
                    setDirty(true);
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
                    if(value >= 0){
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
                setDirty(true);
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
                    setDirty(true);
                }
            }
        });

        configurationNameProperty.addListener((observableValue, oldValue, newValue) -> setDirty(!newValue.equals(configurationNode.getName())));
        configurationDescriptionProperty.addListener((observable, oldValue, newValue) -> setDirty(!newValue.equals(configurationNode.get().getDescription())));

        saveButton.disableProperty().bind(Bindings.createBooleanBinding(() -> dirty.not().get() ||
                        configurationDescriptionProperty.isEmpty().get() ||
                        configurationNameProperty.isEmpty().get() ||
                        userIdentity.isNull().get(),
                dirty, configurationDescriptionProperty, configurationNameProperty, userIdentity));

        addPvButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                pvNameField.textProperty().isEmpty().get() &&
                        readbackPvNameField.textProperty().isEmpty().get(),
                pvNameField.textProperty(), readbackPvNameField.textProperty()));

        readOnlyCheckBox.selectedProperty().bindBidirectional(readOnlyProperty);

        configurationNode.addListener(observable -> {
            if (observable != null) {
                SimpleObjectProperty<Node> simpleObjectProperty = (SimpleObjectProperty<Node>) observable;
                Node newValue = simpleObjectProperty.get();
                configurationNameProperty.set(newValue.getName());
                Platform.runLater(() -> {
                    configurationCreatedDateField.textProperty().set(newValue.getCreated() != null ?
                            TimestampFormats.SECONDS_FORMAT.format(Instant.ofEpochMilli(newValue.getCreated().getTime())) : null);
                    configurationLastModifiedDateField.textProperty().set(newValue.getLastModified() != null ?
                            TimestampFormats.SECONDS_FORMAT.format(Instant.ofEpochMilli(newValue.getLastModified().getTime())) : null);
                    createdByField.textProperty().set(newValue.getUserName());
                });
                configurationDescriptionProperty.set(configurationNode.get().getDescription());
            }
        });

        addPVsPane.disableProperty().bind(userIdentity.isNull());

        SaveAndRestoreService.getInstance().addNodeChangeListener(this);
    }

    @FXML
    @SuppressWarnings("unused")
    public void saveConfiguration() {
        UI_EXECUTOR.execute(() -> {
            try {
                configurationNode.get().setName(configurationNameProperty.get());
                configurationNode.get().setDescription(configurationDescriptionProperty.get());
                configurationData.setPvList(configurationEntries.stream().map(ConfigPvEntry::toConfigPv).toList());
                Configuration configuration = new Configuration();
                configuration.setConfigurationNode(configurationNode.get());
                configuration.setConfigurationData(configurationData);
                if (configurationNode.get().getUniqueId() == null) { // New configuration
                    configuration = saveAndRestoreService.createConfiguration(configurationNodeParent,
                            configuration);
                    configurationTab.setId(configuration.getConfigurationNode().getUniqueId());
                } else {
                    configuration = saveAndRestoreService.updateConfiguration(configuration);
                }
                configurationData = configuration.getConfigurationData();
                loadConfiguration(configuration.getConfigurationNode());
            } catch (Exception e1) {
                ExceptionDetailsErrorDialog.openError(pvTable,
                        Messages.errorActionFailed,
                        Messages.errorCreateConfigurationFailed,
                        e1);
            }
        });
    }

    @FXML
    @SuppressWarnings("unused")
    public void addPv() {

        UI_EXECUTOR.execute(() -> {
            // Process a list of space or semicolon separated pvs
            String[] pvNames = pvNameProperty.get().trim().split("[\\s;]+");
            String[] readbackPvNames = readbackPvNameProperty.get().trim().split("[\\s;]+");

            if(!checkForDuplicatePvNames(pvNames)){
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
            resetAddPv();
        });
    }

    /**
     * Checks that added PV names are not added multiple times
     * @param addedPvNames New PV names added in the UI
     * @return <code>true</code> if no duplicates are detected, otherwise <code>false</code>
     */
    private boolean checkForDuplicatePvNames(String[] addedPvNames){
        List<String> pvNamesAsList = new ArrayList<>();
        pvNamesAsList.addAll(Arrays.asList(addedPvNames));
        pvTable.itemsProperty().get().forEach(i -> pvNamesAsList.add(i.getPvNameProperty().get()));
        List<String> duplicatePvNames = pvNamesAsList.stream().filter(n -> Collections.frequency(pvNamesAsList, n) > 1).toList();

        if(duplicatePvNames.size() > 0){
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
        configurationNode.set(Node.builder().nodeType(NodeType.CONFIGURATION).build());
        configurationData = new ConfigurationData();
        pvTable.setItems(configurationEntries);
        UI_EXECUTOR.execute(() -> configurationNameField.requestFocus());
        setDirty(false);
    }

    /**
     * Loads an existing configuration {@link Node} and its associated {@link ConfigurationData}.
     *
     * @param node An existing {@link Node} of type {@link NodeType#CONFIGURATION}.
     */
    public void loadConfiguration(final Node node) {
        try {
            configurationData = saveAndRestoreService.getConfiguration(node.getUniqueId());
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(root, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
            return;
        }
        loadInProgress.set(true);
        // Create a cloned Node object to avoid changes in the Node object contained in the tree view.
        configurationNode.set(Node.builder().uniqueId(node.getUniqueId())
                .name(node.getName())
                .nodeType(NodeType.CONFIGURATION)
                .description(node.getDescription())
                .userName(node.getUserName())
                .created(node.getCreated())
                .lastModified(node.getLastModified())
                .build());
        loadConfigurationData(() -> loadInProgress.set(false));
    }

    private void loadConfigurationData(Runnable completion) {
        UI_EXECUTOR.execute(() -> {
            try {
                Collections.sort(configurationData.getPvList());
                configurationEntries.setAll(configurationData.getPvList().stream().map(ConfigPvEntry::new).toList());
                pvTable.setItems(configurationEntries);
                completion.run();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to load existing configuration");
            }
        });
    }

    public boolean handleConfigurationTabClosed() {
        if (dirty.get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.closeTabPrompt);
            alert.setContentText(Messages.closeConfigurationWarning);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get().equals(ButtonType.OK);
        }
        return true;
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(configurationNode.get().getUniqueId())) {
            configurationNode.setValue(Node.builder().uniqueId(node.getUniqueId())
                    .name(node.getName())
                    .nodeType(NodeType.CONFIGURATION)
                    .userName(node.getUserName())
                    .description(node.getDescription())
                    .created(node.getCreated())
                    .lastModified(node.getLastModified())
                    .build());
        }
    }

    private void setDirty(boolean dirty) {
        this.dirty.set(dirty && !loadInProgress.get());
    }
}
