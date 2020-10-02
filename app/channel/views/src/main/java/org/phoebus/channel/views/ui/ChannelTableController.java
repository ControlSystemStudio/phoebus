package org.phoebus.channel.views.ui;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javafx.scene.control.Label;
import org.phoebus.channel.views.ChannelTableApp;
import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelUtil;
import org.phoebus.channelfinder.Property;
import org.phoebus.channelfinder.Tag;
import org.phoebus.channelfinder.utility.AddProperty2ChannelsJob;
import org.phoebus.channelfinder.utility.AddTag2ChannelsJob;
import org.phoebus.channelfinder.utility.RemovePropertyChannelsJob;
import org.phoebus.channelfinder.utility.RemoveTagChannelsJob;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import static org.phoebus.channel.views.ui.ChannelFinderController.logger;

/**
 * Controller for the file browser app
 * 
 * @author Kunal Shroff
 *
 */
public class ChannelTableController extends ChannelFinderController {

    @FXML
    TextField query;
    @FXML
    Button search;
    @FXML
    CheckBox showactive;
    @FXML
    TableView<Channel> tableView;
    @FXML
    GridPane gridp;

    @FXML
    Label count;

    private Collection<String> properties;
    private Collection<String> tags;
    private boolean isCBSelected = true;
    public static final boolean showActiveCb;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(ChannelTableController.class, "/cv_preferences.properties");
        showActiveCb = prefs.getBoolean("show_active_cb");
    }

    @SuppressWarnings("unchecked")
    @FXML
    public void initialize() {

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                SelectionService.getInstance().setSelection(tableView, tableView.getSelectionModel().getSelectedItems());
            }
        });

        tableView.getColumns().clear();
        TableColumn<Channel, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<Channel, String>("name"));

        TableColumn<Channel, String> ownerCol = new TableColumn<>("Owner");
        ownerCol.setCellValueFactory(new PropertyValueFactory<Channel, String>("owner"));
        tableView.getColumns().addAll(nameCol, ownerCol);

        if (showActiveCb) {
            showactive.setSelected(isCBSelected);
        } else {
            gridp.getChildren().remove(showactive);
        }
    }

    public void setQuery(String string) {
        query.setText(string);
        search();
    }

    @FXML
    public void search() {
        if (showActiveCb) {
            String currentQuery = query.getText();
            String updatedQuery = currentQuery + " pvStatus=" + (showactive.isSelected() ? "Active" : "*");
            isCBSelected = showactive.isSelected();
            super.search(updatedQuery);
        } else {
            super.search(query.getText());
        }
    }

    private Job addPropertyJob;
    private Job addTagJob;
    private Job removePropertyJob;
    private Job removeTagJob;

    Image addProperties = ImageCache.getImage(ChannelTableApp.class, "/icons/add_properties.png");
    Image addTags = ImageCache.getImage(ChannelTableApp.class, "/icons/add_tag.png");
    Image removeProperties = ImageCache.getImage(ChannelTableApp.class, "/icons/remove_properties.png");
    Image removeTags = ImageCache.getImage(ChannelTableApp.class, "/icons/remove_tag.png");

    @FXML
    public void createContextMenu() {

        final ContextMenu contextMenu = new ContextMenu();
        // Add property to channel
        MenuItem addProperty = new MenuItem("Add Property", new ImageView(addProperties));
        addProperty.setOnAction(e -> {

            // get the list of cf properties
            properties = getClient().getAllProperties();
            AddPropertyDialog dialog = new AddPropertyDialog(tableView, properties);
            Optional<Property> result = dialog.showAndWait();
            result.ifPresent(property -> {
                if (addPropertyJob != null) {
                    addPropertyJob.cancel();
                }
                List<String> channelNames = tableView.getSelectionModel().getSelectedItems().stream().map(ch -> {
                    return ch.getName();
                }).collect(Collectors.toList());
                AddProperty2ChannelsJob.submit(getClient(),
                        channelNames,
                        property,
                        (url, ex) -> ExceptionDetailsErrorDialog.openError("ChannelFinder Query Error", ex.getMessage(), ex));

            });
        });
        contextMenu.getItems().add(addProperty);
        // Add tag to channel
        MenuItem addTag = new MenuItem("Add tag", new ImageView(addTags));
        addTag.setOnAction(e -> {

            // get the list of cf tags
            tags = getClient().getAllTags();
            AddTagDialog dialog = new AddTagDialog(tableView, tags);
            Optional<Tag> result = dialog.showAndWait();
            result.ifPresent(tag -> {
                if (addTagJob != null) {
                    addTagJob.cancel();
                }
                List<String> channelNames = tableView.getSelectionModel().getSelectedItems().stream().map(ch -> {
                    return ch.getName();
                }).collect(Collectors.toList());
                AddTag2ChannelsJob.submit(getClient(),
                        channelNames,
                        tag,
                        (url, ex) -> ExceptionDetailsErrorDialog.openError("ChannelFinder Query Error", ex.getMessage(), ex));

            });
        });
        contextMenu.getItems().add(addTag);
        // Remove property from channels
        MenuItem removeProperty = new MenuItem("Remove Property", new ImageView(removeProperties));
        removeProperty.setOnAction(e -> {

            List<Channel> channels = tableView.getSelectionModel().getSelectedItems();
            List<String> channelNames = channels.stream().map(Channel::getName).collect(Collectors.toList());

            RemovePropertyDialog dialog = new RemovePropertyDialog(tableView, ChannelUtil.getPropertyNames(channels));
            Optional<Property> result = dialog.showAndWait();
            result.ifPresent(property -> {
                if (removePropertyJob != null) {
                    removePropertyJob.cancel();
                }
                RemovePropertyChannelsJob.submit(getClient(),
                        channelNames,
                        property,
                        (url, ex) -> ExceptionDetailsErrorDialog.openError("ChannelFinder Query Error", ex.getMessage(), ex));

            });
        });
        contextMenu.getItems().add(removeProperty);
        // Remove tag from channels
        MenuItem removeTag = new MenuItem("Remove Tag", new ImageView(removeTags));
        removeTag.setOnAction(e -> {

            List<Channel> channels = tableView.getSelectionModel().getSelectedItems();
            List<String> channelNames = channels.stream().map(Channel::getName).collect(Collectors.toList());

            // get the list of cf properties
            RemoveTagDialog dialog = new RemoveTagDialog(tableView, ChannelUtil.getAllTagNames(channels));
            Optional<Tag> result = dialog.showAndWait();
            result.ifPresent(tag -> {
                if (removeTagJob != null) {
                    removeTagJob.cancel();
                }
                RemoveTagChannelsJob.submit(getClient(),
                        channelNames,
                        tag,
                        (url, ex) -> ExceptionDetailsErrorDialog.openError("ChannelFinder Query Error", ex.getMessage(), ex));

            });
        });
        contextMenu.getItems().add(removeTag);
        contextMenu.getItems().add(new SeparatorMenuItem());

        List<ContextMenuEntry> contextEntries = ContextMenuService.getInstance().listSupportedContextMenuEntries();

        contextEntries.forEach(entry -> {
            MenuItem item = new MenuItem(entry.getName(), new ImageView(entry.getIcon()));
            item.setOnAction(e -> {
                try {
                    ObservableList<Channel> old = tableView.getSelectionModel().getSelectedItems();

                    List<Object> pvs = SelectionService.getInstance().getSelection().getSelections().stream().map(s -> {
                        return AdapterService.adapt(s, entry.getSupportedType()).get();
                    }).collect(Collectors.toList());
                    // set the selection 
                    SelectionService.getInstance().setSelection(tableView, pvs);
                    entry.call(SelectionService.getInstance().getSelection());
                    // reset the selection
                    SelectionService.getInstance().setSelection(tableView, old);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Failed to execute action " + entry.getName(), ex);
                }
            });
            contextMenu.getItems().add(item);
        });

        tableView.setContextMenu(contextMenu);
    }

    @Override
    public void setChannels(Collection<Channel> channels) {
        initialize();
        List<String> allProperties = ChannelUtil.getPropertyNames(channels).stream().sorted().collect(Collectors.toList());
        List<String> allTags = ChannelUtil.getAllTagNames(channels).stream().sorted().collect(Collectors.toList());
        List<TableColumn<Channel, String>> propColumns = allProperties.parallelStream().map(new Function<String, TableColumn<Channel, String>>() {

            @Override
            public TableColumn<Channel, String> apply(String propName) {
                TableColumn<Channel, String> propCol = new TableColumn<>(propName);
                propCol.setCellValueFactory(new Callback<CellDataFeatures<Channel, String>, ObservableValue<String>>() {

                    @Override
                    public ObservableValue<String> call(CellDataFeatures<Channel, String> channel) {
                        Property prop = channel.getValue().getProperty(propName);
                        return new SimpleStringProperty(prop != null ? prop.getValue(): "");
                    }
                });
                return propCol;
            }
        }).collect(Collectors.toList());

        List<TableColumn<Channel, String>> tagColumns = allTags.parallelStream().map(new Function<String, TableColumn<Channel, String>>() {

            @Override
            public TableColumn<Channel, String> apply(String tagName) {
                TableColumn<Channel, String> tagCol = new TableColumn<>(tagName);
                tagCol.setCellValueFactory(new Callback<CellDataFeatures<Channel, String>, ObservableValue<String>>() {

                    @Override
                    public ObservableValue<String> call(CellDataFeatures<Channel, String> channel) {
                        return new SimpleStringProperty(channel.getValue().getTag(tagName) != null ? "tagged" : "");
                    }
                });
                return tagCol;
            }
        }).collect(Collectors.toList());
        tableView.getColumns().addAll(propColumns);
        tableView.getColumns().addAll(tagColumns);
        tableView.setItems(FXCollections.observableArrayList(channels));

        // the channel finder queries are limited to 10k by default
        // TODO update check for the situation where the max size is set up user
        if(channels.size() >= 10000) {
            count.setText(String.valueOf(channels.size() + "+"));
        } else {
            count.setText(String.valueOf(channels.size()));
        }
    }

}
