package org.phoebus.logbook.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogPropertiesController {

    @FXML
    TreeTableView<PropertyTreeNode> treeTableView;

    @FXML
    TreeTableColumn name;
    @FXML
    TreeTableColumn value;

    @FXML
    BooleanProperty editable = new SimpleBooleanProperty(false);

    @FXML
    public void initialize()
    {
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
        value.setEditable(editable.getValue());
        value.setCellFactory(new Callback<TreeTableColumn<PropertyTreeNode, String>,
                                          TreeTableCell<PropertyTreeNode, String>>() {

            @Override
            public TreeTableCell<PropertyTreeNode, String> call(TreeTableColumn<PropertyTreeNode, String> param) {
                return new TreeTableCell<PropertyTreeNode, String> () {

                    private TextField textField;

                    @Override
                    public void startEdit() {
                        super.startEdit();

                        if (textField == null) {
                            createTextField();
                        }
                        setText(null);
                        setGraphic(textField);
                        textField.selectAll();
                    }

                    @Override
                    public void cancelEdit() {
                        super.cancelEdit();
                        setText((String) getItem());
                        setGraphic(getTreeTableRow().getGraphic());
                    }

                    @Override
                    public void updateItem(String item, boolean empty){
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            try {
                                URL url = new URL(item);
                                final Hyperlink link = new Hyperlink(url.toString());
                                setGraphic(link);
                            } catch (Exception e) {
                                setGraphic(new Label(item));
                            }
                        }
                    }

                    private void createTextField() {
                        textField = new TextField(getString());
                        textField.setOnKeyReleased((KeyEvent t) -> {
                            if (t.getCode() == KeyCode.ENTER) {
                                commitEdit(textField.getText());
                            } else if (t.getCode() == KeyCode.ESCAPE) {
                                cancelEdit();
                            }
                        });
                    }
                    private String getString() {
                        return getItem() == null ? "" : getItem().toString();
                    }
                };
            }
        });

        editable.addListener((observable, oldValue, newValue) -> {
            value.setEditable(newValue);
        });
    }

    private void constructTree(List<Property> properties) {
        if (properties != null && !properties.isEmpty())
        {
            TreeItem root = new TreeItem(new PropertyTreeNode("properties", " "));
            root.getChildren().setAll(properties.stream().map(property -> {
                PropertyTreeNode node = new PropertyTreeNode(property.getName(), " ");
                TreeItem<PropertyTreeNode> treeItem = new TreeItem<>(node);
                property.getAttributes().entrySet().stream().forEach(entry -> {
                    treeItem.getChildren().add(new TreeItem<>(new PropertyTreeNode(entry.getKey(), entry.getValue())));
                });
                treeItem.setExpanded(true);
                return treeItem;
            }).collect(Collectors.toSet()));
            treeTableView.setRoot(root);
            treeTableView.setShowRoot(false);
        }
    }

    /**
     * Set the list of properties to be displayed.
     * @param properties
     */
    public void setProperties(List<Property> properties)
    {
        constructTree(properties);
    }

    /**
     * @return The list of logentry properties
     */
    public List<Property> getProperties()
    {
        List<Property> treeProperties = new ArrayList<>();
        treeTableView.getRoot().getChildren().stream().forEach(node -> {
            Map<String, String> att = node.getChildren().stream()
                    .map(TreeItem::getValue)
                    .collect(Collectors.toMap(PropertyTreeNode::getName, PropertyTreeNode::getValue));
            Property property = PropertyImpl.of(node.getValue().getName(), att);
            treeProperties.add(property);
        });
        return treeProperties;
    }

    public void setEditable(boolean editable) {
        this.editable.set(editable);
    }

    private static class PropertyTreeNode
    {
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
}
