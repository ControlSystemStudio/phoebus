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

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SearchQueryEditorController implements Initializable {

    private SearchAndFilterViewController searchAndFilterViewController;

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

    private SimpleStringProperty nodeNameProperty = new SimpleStringProperty();

    private SimpleBooleanProperty nodeTypeFolderProperty = new SimpleBooleanProperty();
    private SimpleBooleanProperty nodeTypeConfigurationProperty = new SimpleBooleanProperty();

    private SimpleBooleanProperty nodeTypeSnapshotProperty = new SimpleBooleanProperty();
    private SimpleBooleanProperty nodeTypeCompositeSnapshotProperty = new SimpleBooleanProperty();

    private SimpleStringProperty tagsProperty = new SimpleStringProperty();
    private SimpleStringProperty userProperty = new SimpleStringProperty();

    private SimpleStringProperty descProperty = new SimpleStringProperty();

    private boolean searchDisabled = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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
    }

    public void setFilter(Filter filter) {
        // Temporarily disable search as setting the types options would otherwise trigger
        // a search for each selection.
        searchDisabled = true;
        Map<String, String> searchParams = SearchQueryUtil.parseHumanReadableQueryString(filter.getQueryString());
        nodeNameProperty.set(searchParams.get(Keys.NAME.getName()));
        userProperty.set(searchParams.get(Keys.USER.getName()));
        descProperty.set(searchParams.get(Keys.DESC.getName()));
        tagsProperty.set(searchParams.get(Keys.TAGS.getName()));

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
        // Enable search...
        searchDisabled = false;
        // ...and trigger it.
        updateParametersAndSearch();
    }

    public void setSearchAndFilterViewController(SearchAndFilterViewController searchAndFilterViewController) {
        this.searchAndFilterViewController = searchAndFilterViewController;
    }

    private void updateParametersAndSearch() {
        if(searchDisabled){
            return;
        }
        String queryString = buildQueryString();
        searchAndFilterViewController.search(queryString);
    }

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
            map.put(Keys.TYPE.getName(), types.stream().collect(Collectors.joining(",")));
        }
        return SearchQueryUtil.toQueryString(map);
    }
}
