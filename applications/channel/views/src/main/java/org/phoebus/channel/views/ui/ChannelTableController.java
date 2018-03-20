package org.phoebus.channel.views.ui;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

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
    TableView<Channel> tableView;

    @SuppressWarnings("unchecked")
    @FXML
    public void initialize() {
        tableView.getColumns().clear();
        TableColumn<Channel, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<Channel, String>("name"));

        TableColumn<Channel, String> ownerCol = new TableColumn<>("Owner");
        ownerCol.setCellValueFactory(new PropertyValueFactory<Channel, String>("owner"));
        tableView.getColumns().addAll(nameCol, ownerCol);
    }

    public void setQuery(String string) {

    }

    @FXML
    public void search() {
        super.search(query.getText());
    }

    @Override
    public void setChannels(Collection<Channel> channels) {
        initialize();
        Collection<String> allProperties = ChannelUtil.getPropertyNames(channels);
        Collection<String> allTags = ChannelUtil.getAllTagNames(channels);
        List<TableColumn<Channel, String>> propColumns = allProperties.parallelStream().map(new Function<String, TableColumn<Channel, String>>() {

            @Override
            public TableColumn<Channel, String> apply(String propName) {
                TableColumn<Channel, String> propCol = new TableColumn<>(propName);
                propCol.setCellValueFactory(new Callback<CellDataFeatures<Channel, String>, ObservableValue<String>>() {

                    @Override
                    public ObservableValue<String> call(CellDataFeatures<Channel, String> channel) {
                        return new SimpleStringProperty(channel.getValue().getProperty(propName).getValue());
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
    }

}
