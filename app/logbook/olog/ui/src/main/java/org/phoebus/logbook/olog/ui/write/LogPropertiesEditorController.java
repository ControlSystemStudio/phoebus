package org.phoebus.logbook.olog.ui.write;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogPropertiesProvider;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.olog.ui.LogbookUIPreferences;
import org.phoebus.ui.javafx.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class LogPropertiesEditorController {

    /**
     * List of properties user may add to log entry. It is constructed from the properties as
     * provided by the log service, plus optional properties providers, see {@link LogPropertiesProvider}.
     */
    private final ObservableList<Property> availableProperties = FXCollections.observableArrayList();
    /**
     * List of properties selected by user to be included in the log record.
     */
    private final ObservableList<Property> selectedProperties = FXCollections.observableArrayList();
    /**
     * List of hidden properties, e.g. properties that are added automatically to the log record,
     * but that should/must not be rendered in the properties view.
     */
    private final List<Property> hiddenProperties;

    @FXML
    TreeTableView<PropertyTreeNode> selectedPropertiesTree;

    @FXML
    TreeTableColumn name;
    @FXML
    TreeTableColumn value;

    @FXML
    TableView<Property> availablePropertiesView;

    @FXML
    TableColumn propertyName;

    private final List<String> hiddenPropertiesNames = Arrays.asList(LogbookUIPreferences.hidden_properties);

    /**
     * @param properties A collection of {@link Property}s.
     */
    public LogPropertiesEditorController(Collection<Property> properties) {
        // Log entry may already contain properties, so need to handle them accordingly.
        this.hiddenProperties =
                properties.stream().filter(p -> hiddenPropertiesNames.contains(p.getName())).collect(Collectors.toList());
        this.selectedProperties.addAll(
                properties.stream().filter(p -> !hiddenPropertiesNames.contains(p.getName())).collect(Collectors.toList()));
    }

    @FXML
    public void initialize() {

        setupProperties();
        constructTree(selectedProperties);

        selectedProperties.addListener((ListChangeListener<Property>) p -> constructTree(selectedProperties));

        name.setMaxWidth(1f * Integer.MAX_VALUE * 40);
        name.setCellValueFactory(
                (Callback<CellDataFeatures<PropertyTreeNode, String>, ObservableValue<String>>) p -> p.getValue().getValue().nameProperty());

        name.setCellFactory((Callback<TreeTableColumn<PropertyTreeNode, String>, TreeTableCell<PropertyTreeNode, String>>) param -> new PropertyNameCell());

        value.setMaxWidth(1f * Integer.MAX_VALUE * 60);
        value.setCellValueFactory(
                (Callback<CellDataFeatures<PropertyTreeNode, String>, ObservableValue<String>>) p -> p.getValue().getValue().valueProperty());
        value.setEditable(true);

        value.setCellFactory((Callback<TreeTableColumn<PropertyTreeNode, String>, TreeTableCell<PropertyTreeNode, String>>) param ->
                new PropertyValueTreeTableCell(new DefaultStringConverter()));

        // Hide the headers
        selectedPropertiesTree.widthProperty().addListener((ov, t, t1) -> {
            // Get the table header
            Pane header = (Pane) selectedPropertiesTree.lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
                header.setManaged(false);
            }
        });

        propertyName.setCellValueFactory(
                (Callback<TableColumn.CellDataFeatures<Property, String>, ObservableValue<String>>) p -> new SimpleStringProperty(p.getValue().getName()));
        availablePropertiesView.setOnMouseClicked(event -> {
            if (event.getClickCount() > 1) {
                availablePropertySelection();
            }
        });
        availablePropertiesView.setEditable(false);
        availablePropertiesView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availablePropertiesView.setItems(availableProperties);
    }

    private void constructTree(Collection<Property> properties) {
        if (properties != null) {
            TreeItem root = new TreeItem(new PropertyTreeNode("properties", " "));
            AtomicReference<Double> rowCount = new AtomicReference<>((double) 1);
            root.getChildren().setAll(properties.stream()
                    .map(property -> {
                        PropertyTreeNode node = new PropertyTreeNode(property.getName(), " ");
                        rowCount.set(rowCount.get() + 1);
                        TreeItem<PropertyTreeNode> treeItem = new TreeItem<>(node);
                        property.getAttributes().forEach((key, value1) -> {
                            rowCount.set(rowCount.get() + 1);
                            treeItem.getChildren().add(new TreeItem<>(new PropertyTreeNode(key, value1)));
                        });
                        treeItem.setExpanded(true);
                        return treeItem;
                    }).collect(Collectors.toSet()));
            Platform.runLater(() -> {
                selectedPropertiesTree.setRoot(root);
                selectedPropertiesTree.setShowRoot(false);
            });
        }
    }

    /**
     * @return The list of log entry properties. This may contain hidden properties, i.e.
     * properties not rendered in the UI.
     */
    public List<Property> getProperties() {
        if (selectedPropertiesTree.getRoot() == null) {
            return hiddenProperties;
        }
        selectedProperties.addAll(hiddenProperties);
        return selectedProperties;
    }

    /**
     * Move the user selected available properties from the available list to the selected properties tree view
     */
    @FXML
    public void availablePropertySelection() {
        ObservableList<Property> userSelectedProperties = availablePropertiesView.getSelectionModel().getSelectedItems();
        // add user selected properties
        userSelectedProperties.forEach(selectedProperties::add);
        // remove the properties from the list of available properties
        availableProperties.removeAll(userSelectedProperties);
    }

    private void removeSelectedProperty(String propertyName) {
        Optional<Property> property =
                selectedProperties.stream().filter(p -> p.getName().equals(propertyName)).findFirst();
        if (property.isPresent()) {
            selectedProperties.remove(property.get());
            availableProperties.add(property.get());
            availableProperties.sort(Comparator.comparing(Property::getName));
        }
    }

    private static class PropertyTreeNode {
        private SimpleStringProperty name;
        private SimpleStringProperty value;

        public SimpleStringProperty nameProperty() {
            if (name == null) {
                name = new SimpleStringProperty(this, "name");
            }
            return name;
        }

        public SimpleStringProperty valueProperty() {
            if (value == null) {
                value = new SimpleStringProperty(this, "value");
            }
            return value;
        }

        private PropertyTreeNode(String name, String value) {
            this.name = new SimpleStringProperty(name);
            this.value = new SimpleStringProperty(value);
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value.set(value);
        }
    }

    /**
     * Refreshes the list of available properties. Properties provided by {@link LogPropertiesProvider} implementations
     * are considered first, and then properties available from service. However, adding items to the list of properties
     * always consider equality, i.e. properties with same name are added only once. SPI implementations should therefore
     * not support properties with same name, and should not implement properties available from service.
     * <p>
     * Further, if the user is editing a copy (reply) based on another log entry, a set of properties may
     * already be present in the new log entry. The list of available properties will not contain such properties
     * as this would be confusing.
     * <p>
     * When user chooses to remove a property from the list of properties, the list of available properties must
     * also be refreshed, so this method should handle such a use case.
     * <p>
     * Also, properties to be excluded as listed in the preferences (properties_excluded_from_view) are not
     * added to the properties tree.
     */
    private void setupProperties() {
        JobManager.schedule("Fetch Properties from service", monitor ->
        {
            // First add properties from SPI implementations
            List<LogPropertiesProvider> factories = new ArrayList<>();
            ServiceLoader<LogPropertiesProvider> loader = ServiceLoader.load(LogPropertiesProvider.class);
            loader.stream().forEach(p -> {
                if (p.get().getProperties() != null && !p.get().getProperties().isEmpty()) {
                    factories.add(p.get());
                }
            });
            // List of property names added if user is replying to a log entry.
            List<String> selectedPropertyNames =
                    selectedProperties.stream().map(Property::getName).collect(Collectors.toList());
            factories.stream()
                    .map(LogPropertiesProvider::getProperties)
                    .forEach(propertiesList -> {
                        // Do not add a property that is already present
                        propertiesList.forEach(property -> {
                            if (!selectedPropertyNames.contains(property.getName())) {
                                selectedProperties.add(property);
                                selectedPropertyNames.add(property.getName());
                            }
                        });
                    });

            LogClient logClient =
                    LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient();
            List<Property> propertyList = logClient.listProperties().stream().collect(Collectors.toList());
            List<Property> list = new ArrayList<>();
            Platform.runLater(() ->
            {
                propertyList.forEach(property -> {
                    // Do not add already selected property, or added from provider.
                    if (!selectedPropertyNames.contains(property.getName())) {
                        list.add(property);
                    }
                });
                list.sort(Comparator.comparing(Property::getName));
                availableProperties.setAll(list);
            });
        });
    }

    /**
     * Custom cell renderer supporting a context menu for property names. The context
     * menu adds a "Remove" item to let the user remove a property from the list of
     * selected properties.
     * <p>
     * Some logic is applied to make sure the context menu is shown only for the property
     * name cell, but not for attribute name cells as this would suggest that attributes
     * could be removed from a property.
     */
    private class PropertyNameCell extends TreeTableCell<PropertyTreeNode, String> {

        private final ContextMenu contextMenu;
        private final MenuItem menuItem;

        public PropertyNameCell() {
            contextMenu = new ContextMenu();
            menuItem = new MenuItem(Messages.Remove);
            contextMenu.getItems().add(menuItem);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
            } else {
                // Binding to determine if the selected cell is a leaf or not.
                // Property name cells are not leaves.
                BooleanBinding binding = Bindings.createBooleanBinding(() ->
                        getTableRow().getTreeItem() != null &&
                                !getTableRow().getTreeItem().leafProperty().get());
                // Action must specify the property name subject for removal
                menuItem.setOnAction(e -> removeSelectedProperty(item));
                contextMenuProperty().bind(Bindings
                        .when(binding)
                        .then(contextMenu)
                        .otherwise((ContextMenu) null));
                setText(item);
            }
        }
    }

    private class PropertyValueTreeTableCell extends TextFieldTreeTableCell<PropertyTreeNode, String> {

        private final TextField textField = new TextField();

        PropertyValueTreeTableCell(StringConverter stringConverter) {
            super(stringConverter);
            textField.setText(getItem());
            textField.setOnKeyReleased((KeyEvent t) -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(textField.getText());
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        @Override
        public void commitEdit(String value) {
            super.commitEdit(value);
            TreeTableRow row = getTableRow();
            PropertyTreeNode parent = (PropertyTreeNode) row.getTreeItem().getParent().getValue();
            String propertyName = parent.getName();
            String attributeName = ((PropertyTreeNode) row.getTreeItem().getValue()).getName();
            // Update the model too!
            for (Property p : selectedProperties) {
                if (!p.getName().equals(propertyName)) {
                    continue;
                }
                if (p.getAttributes().containsKey(attributeName)) {
                    p.getAttributes().put(attributeName, value);
                    // Should be OK to break here
                    break;
                }
            }
        }

        @Override
        public void startEdit() {
            super.startEdit();
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }

        @Override
        public void cancelEdit(){
            super.cancelEdit();
            // Make sure the unedited value is set in order to keep the
            // text field content consistent.
            textField.setText(getItem());
        }
    }
}
