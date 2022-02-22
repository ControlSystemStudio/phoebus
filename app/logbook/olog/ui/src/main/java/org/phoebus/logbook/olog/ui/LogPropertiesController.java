package org.phoebus.logbook.olog.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LogPropertiesController {

    private static final Logger logger = Logger.getLogger(LogPropertiesController.class.getName());
    static final ImageView copy = ImageCache.getImageView(LogPropertiesController.class, "/icons/copy_edit.png");

    private Map<String, String> attributeTypes = new HashMap<>();

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
        // Read the attribute types
        String url = LogbookUIPreferences.log_attribute_desc;
        if (url.isEmpty())
        {
            final URL resource = getClass().getResource("log_property_attributes.properties");
            url = resource.toExternalForm();
        }
        try (InputStream input = new URL(url).openStream() ) {
            Properties prop = new Properties();
            prop.load(input);
            prop.stringPropertyNames().stream().forEach((p) -> {
                attributeTypes.put(p.toLowerCase(), prop.getProperty(p).toLowerCase());
                }
            );

        } catch (IOException ex) {
            ex.printStackTrace();
        }


        // create the property trees
        name.setMaxWidth(1f * Integer.MAX_VALUE * 40);
        name.setCellValueFactory(
                new Callback<TreeTableColumn.CellDataFeatures<PropertyTreeNode, String>, ObservableValue<String>>() {
                    public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<PropertyTreeNode, String> p) {
                        return p.getValue().getValue().nameProperty();
                    }
                });

        value.setMaxWidth(1f * Integer.MAX_VALUE * 60);
        value.setCellValueFactory(
                new Callback<TreeTableColumn.CellDataFeatures<PropertyTreeNode, ?>, SimpleObjectProperty<PropertyTreeNode>>() {
                    public SimpleObjectProperty<PropertyTreeNode> call(TreeTableColumn.CellDataFeatures<PropertyTreeNode, ?> p) {
                        return p.getValue().getValue().nodeProperty();
                    }
                });
        value.setEditable(editable.getValue());
        value.setCellFactory(new Callback<TreeTableColumn<PropertyTreeNode, Object>, TreeTableCell<PropertyTreeNode, Object>>() {

            @Override
            public TreeTableCell<PropertyTreeNode, Object> call(TreeTableColumn<PropertyTreeNode, Object> param) {
                return new TreeTableCell<>() {

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
                        setText(((PropertyTreeNode)getItem()).getValue());
                        setGraphic(getTreeTableRow().getGraphic());
                    }

                    @Override
                    public void updateItem(Object item, boolean empty){
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            PropertyTreeNode propertyItem = ((PropertyTreeNode)item);
                            if (attributeTypes.containsKey(propertyItem.getFullQualifiedName().toLowerCase())) {
                                switch (attributeTypes.get(propertyItem.getFullQualifiedName().toLowerCase())) {
                                    case "url":
                                        final String url = propertyItem.getValue();
                                        final URI uri = URI.create(url);
                                        final Hyperlink urlLink = new Hyperlink(propertyItem.getValue());
                                        // TODO open url in some webclient
                                        urlLink.setOnContextMenuRequested((e) -> {
                                            ContextMenu contextMenu = new ContextMenu();
                                            final List<AppResourceDescriptor> applications = ApplicationService.getApplications(uri);
                                            applications.forEach( app -> {
                                                MenuItem menuItem = new MenuItem(app.getDisplayName());
                                                menuItem.setGraphic(ImageCache.getImageView(app.getIconURL()));
                                                menuItem.setOnAction(actionEvent -> app.create(uri));
                                                contextMenu.getItems().add(menuItem);
                                            });
                                            contextMenu.getItems().add(new SeparatorMenuItem());
                                            MenuItem copyMenuItem = new MenuItem("copy", copy);
                                            copyMenuItem.setOnAction(event -> {
                                                final ClipboardContent content = new ClipboardContent();
                                                content.putString(uri.toString());
                                                Clipboard.getSystemClipboard().setContent(content);
                                            });
                                            contextMenu.getItems().add(copyMenuItem);
                                            urlLink.setContextMenu(contextMenu);
                                        });
                                        setGraphic(urlLink);
                                        break;
                                    case "resource":
                                        final String resourceURL = propertyItem.getValue();
                                        final URI resource = URI.create(resourceURL);
                                        final Hyperlink resourceLink = new Hyperlink(resourceURL);
                                        setGraphic(resourceLink);
                                        // Open resource using the default application
                                        resourceLink.setOnAction((e) -> {
                                            final List<AppResourceDescriptor> applications = ApplicationService.getApplications(resource);
                                            if (!applications.isEmpty()) {
                                                applications.get(0).create(resource);
                                            }
                                        });
                                        resourceLink.setOnContextMenuRequested((e) -> {
                                            ContextMenu contextMenu = new ContextMenu();
                                            final List<AppResourceDescriptor> applications = ApplicationService.getApplications(resource);
                                            applications.forEach( app -> {
                                                MenuItem menuItem = new MenuItem(app.getDisplayName());
                                                menuItem.setGraphic(ImageCache.getImageView(app.getIconURL()));
                                                menuItem.setOnAction(actionEvent -> app.create(resource));
                                                contextMenu.getItems().add(menuItem);
                                            });
                                            contextMenu.getItems().add(new SeparatorMenuItem());
                                            MenuItem copyMenuItem = new MenuItem("copy", copy);
                                            copyMenuItem.setOnAction(event -> {
                                                final ClipboardContent content = new ClipboardContent();
                                                content.putString(resource.toString());
                                                Clipboard.getSystemClipboard().setContent(content);
                                            });
                                            contextMenu.getItems().add(copyMenuItem);
                                            resourceLink.setContextMenu(contextMenu);

                                        });
                                        break;
                                    default:
                                        setGraphic(new Label(propertyItem.getValue()));
                                }
                            } else {
                                setGraphic(new Label(propertyItem.getValue()));
                            }
                        }
                    }

                    private void createTextField() {
                        textField = new TextField(getString());
                        textField.setOnKeyReleased((KeyEvent t) -> {
                            if (t.getCode() == KeyCode.ENTER) {
                                ((PropertyTreeNode) getItem()).setValue(textField.getText());
                                commitEdit(getItem());
                            } else if (t.getCode() == KeyCode.ESCAPE) {
                                cancelEdit();
                            }
                        });
                    }
                    private String getString()
                    {
                        if (getItem() != null && ((PropertyTreeNode) getItem()).getValue() != null) {
                            return ((PropertyTreeNode) getItem()).getValue();
                        } else {
                            return "";
                        }
                    }
                };
            }
        });

        editable.addListener((observable, oldValue, newValue) -> {
            value.setEditable(newValue);
        });

        // Hide the headers
        treeTableView.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                // Get the table header
                Pane header = (Pane)treeTableView.lookup("TableHeaderRow");
                if(header!=null && header.isVisible()) {
                    header.setMaxHeight(0);
                    header.setMinHeight(0);
                    header.setPrefHeight(0);
                    header.setVisible(false);
                    header.setManaged(false);
                }
            }
        });
    }

    private void constructTree(Collection<Property> properties) {
        TreeItem root = new TreeItem(new PropertyTreeNode("", "properties", " "));
        AtomicReference<Double> rowCount = new AtomicReference<>((double) 1);
        root.getChildren().setAll(properties.stream()
                .map(property -> {
            PropertyTreeNode node = new PropertyTreeNode(property.getName(), property.getName(), " ");
            rowCount.set(rowCount.get() + 1);
            TreeItem<PropertyTreeNode> treeItem = new TreeItem<>(node);
            property.getAttributes().entrySet().stream().forEach(entry -> {
                rowCount.set(rowCount.get() + 1);
                treeItem.getChildren().add(new TreeItem<>(
                        new PropertyTreeNode(property.getName()+"."+entry.getKey(),
                                             entry.getKey(),
                                             entry.getValue())));
            });
            treeItem.setExpanded(true);
            return treeItem;
        }).collect(Collectors.toSet()));
        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);
        //treeTableView.setPrefHeight(rowCount.get()*22);
    }

    /**
     * Set the list of properties to be displayed.
     * @param properties
     */
    public void setProperties(Collection<Property> properties)
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
                    .collect(Collectors.toMap(
                            PropertyTreeNode::getName,
                            (PropertyTreeNode prop)-> prop.getValue() == null ? "" : prop.getValue()));
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
        private String fullQualifiedName;
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

        public SimpleObjectProperty<PropertyTreeNode> nodeProperty() {
            return new SimpleObjectProperty<PropertyTreeNode>(this);
        }

        private PropertyTreeNode(String fullQualifiedName, String name, String value) {
            this.fullQualifiedName = fullQualifiedName;
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

        public String getFullQualifiedName() {
            return this.fullQualifiedName;
        }
    }
}
