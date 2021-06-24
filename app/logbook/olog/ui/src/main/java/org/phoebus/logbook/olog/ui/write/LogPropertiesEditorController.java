package org.phoebus.logbook.olog.ui.write;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;
import org.phoebus.logbook.olog.ui.LogbookUIPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class LogPropertiesEditorController {

    /**
     * List of properties user may add to log entry. It is constructed from the properties as
     * provided by the log service, plus optional properties providers, see {@link LogPropertyProvider}.
     */
    private ObservableList<Property> availableProperties = FXCollections.observableArrayList();
    /**
     * List of properties selected by user to be included in the log record.
     */
    private ObservableList<Property> selectedProperties = FXCollections.observableArrayList();
    /**
     * List of hidden properties, e.g. properties that are added automatically to the log record,
     * but that should/must not be rendered in the properties view.
     */
    private List<Property> hiddenProperties;

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

    private List<String> hiddenPropertiesNames = Arrays.asList(LogbookUIPreferences.hidden_properties);

    /**
     *
     * @param properties A collection of {@link Property}s.
     */
    public LogPropertiesEditorController(Collection<Property> properties){
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
                new Callback<TreeTableColumn.CellDataFeatures<PropertyTreeNode, String>, ObservableValue<String>>() {
                    public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<PropertyTreeNode, String> p) {
                        return p.getValue().getValue().nameProperty();
                    }
                });

        value.setMaxWidth(1f * Integer.MAX_VALUE * 60);
        value.setCellValueFactory(
                new Callback<TreeTableColumn.CellDataFeatures<PropertyTreeNode, String>, ObservableValue<String>>() {
                    public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<PropertyTreeNode, String> p) {
                        return p.getValue().getValue().valueProperty();
                    }
                });
        value.setEditable(true);

        value.setCellFactory((Callback<TreeTableColumn<PropertyTreeNode, String>, TreeTableCell<PropertyTreeNode, String>>) param ->
                new TextFieldTreeTableCell<>(new DefaultStringConverter()));

        // Hide the headers
        selectedPropertiesTree.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                // Get the table header
                Pane header = (Pane) selectedPropertiesTree.lookup("TableHeaderRow");
                if (header != null && header.isVisible()) {
                    header.setMaxHeight(0);
                    header.setMinHeight(0);
                    header.setPrefHeight(0);
                    header.setVisible(false);
                    header.setManaged(false);
                }
            }
        });

        propertyName.setCellValueFactory(
                new Callback<TableColumn.CellDataFeatures<Property, String>, ObservableValue<String>>() {
                    public ObservableValue<String> call(TableColumn.CellDataFeatures<Property, String> p) {
                        return new SimpleStringProperty(p.getValue().getName());
                    }
                });
        availablePropertiesView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() > 1) {
                    availablePropertySelection();
                }
            }
        });
        availablePropertiesView.setEditable(false);
        availablePropertiesView.setItems(availableProperties);
    }

    private void constructTree(Collection<Property> properties) {
        if (properties != null && !properties.isEmpty()) {
            TreeItem root = new TreeItem(new PropertyTreeNode("properties", " "));
            AtomicReference<Double> rowCount = new AtomicReference<>((double) 1);
            root.getChildren().setAll(properties.stream()
                    .map(property -> {
                PropertyTreeNode node = new PropertyTreeNode(property.getName(), " ");
                rowCount.set(rowCount.get() + 1);
                TreeItem<PropertyTreeNode> treeItem = new TreeItem<>(node);
                property.getAttributes().entrySet().stream().forEach(entry -> {
                    rowCount.set(rowCount.get() + 1);
                    treeItem.getChildren().add(new TreeItem<>(new PropertyTreeNode(entry.getKey(), entry.getValue())));
                });
                treeItem.setExpanded(true);
                return treeItem;
            }).collect(Collectors.toSet()));
            selectedPropertiesTree.setRoot(root);
            selectedPropertiesTree.setShowRoot(false);
        }
    }

    /**
     * @return The list of log entry properties
     */
    public List<Property> getProperties() {
        List<Property> treeProperties = new ArrayList<>();
        // Add the hidden properties
        treeProperties.addAll(hiddenProperties);
        if (selectedPropertiesTree.getRoot() == null) {
            return treeProperties;
        }
        selectedPropertiesTree.getRoot().getChildren().stream().forEach(node -> {
            Map<String, String> att = node.getChildren().stream()
                    .map(TreeItem::getValue)
                    .collect(Collectors.toMap(PropertyTreeNode::getName, PropertyTreeNode::getValue));
            Property property = PropertyImpl.of(node.getValue().getName(), att);
            treeProperties.add(property);
        });
        return treeProperties;
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
     * Refreshes the list of available properties. Properties provided by {@link LogPropertyProvider} implementations
     * are considered first, and then properties available from service. However, adding items to the list of properties
     * always consider equality, i.e. properties with same name are added only once. SPI implementations should therefore
     * not support properties with same name, and should not implement properties available from service.
     *
     * On top of that, if the user is editing a copy (reply) based on another log entry, a set of properties may
     * already be present in the new log entry. The list of available properties will not contain such properties
     * as this would be confusing.
     *
     * Also, properties to be excluded as listed in the preferences (properties_excluded_from_view) are not
     * added to the properties tree.
     */
    private void setupProperties(){
        List<Property> list = new ArrayList<>();
        // First add properties from SPI implementations
        List<LogPropertyProvider> factories = new ArrayList<LogPropertyProvider>();
        ServiceLoader<LogPropertyProvider> loader = ServiceLoader.load(LogPropertyProvider.class);
        loader.stream().forEach(p -> {
            if(p.get().getProperty() != null){
                factories.add(p.get());
            }
        });
        factories.stream()
                .map(LogPropertyProvider::getProperty)
                .forEach(property -> {
                    // Do not add a property that
                    if(!selectedProperties.contains(property)){
                        list.add(property);
                    }
                });

        JobManager.schedule("Fetch Properties from service", monitor ->
        {
            LogClient logClient =
                    LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient();
            List<Property> propertyList = logClient.listProperties().stream().collect(Collectors.toList());
            Platform.runLater(() ->
            {
                propertyList.forEach(property -> {
                    if (!selectedProperties.contains(property) && !list.contains(property)) {
                        list.add(property);
                    }
                });
                availableProperties.setAll(list);
            });
        });
    }


}
