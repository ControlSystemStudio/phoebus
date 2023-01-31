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
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.ui.javafx.ImageCache;

import java.util.List;

public class SearchToolbarController {

    @FXML
    private Button searchButton;

    @FXML
    private Button saveFilterButton;

    @FXML
    private TextField queryStringTextField;

    @FXML
    private ComboBox<Filter> filtersComboBox;

    @FXML
    private Button deleteFilterButton;

    private SaveAndRestoreController saveAndRestoreController;

    private SaveAndRestoreService saveAndRestoreService;

    private Filter noFilter;

    private SimpleStringProperty queryString = new SimpleStringProperty();

    public void initialize() {

        noFilter = new Filter();
        noFilter.setName(Messages.noFilter);
        noFilter.setQueryString("");

        ImageView searchButtonImageView = ImageCache.getImageView(SearchToolbarController.class, "/icons/sar-search.png");
        searchButtonImageView.setFitWidth(16);
        searchButtonImageView.setFitHeight(16);
        searchButton.setGraphic(searchButtonImageView);
        searchButton.setTooltip(new Tooltip(Messages.buttonSearch));

        filtersComboBox.getSelectionModel().selectedItemProperty().addListener((ob, old, newValue) -> {
            queryString.set(newValue.getQueryString());
        });

        filtersComboBox.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Filter> call(ListView<Filter> param) {
                return new ListCell<>() {
                    @Override
                    public void updateItem(Filter item,
                                           boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty && item != null) {
                            setText(item.getName());
                        }
                    }
                };
            }
        });

        filtersComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Filter filter) {
                        if (filter == null) {
                            return "";
                        } else {
                            return filter.getName();
                        }
                    }

                    @Override
                    public Filter fromString(String s) {
                        return null;
                    }
                });

        queryStringTextField.textProperty().bindBidirectional(queryString);
        saveFilterButton.disableProperty().bind(queryString.isEmpty());
        //deleteFilterButton.disableProperty().bind(Bindings.createBooleanBinding(() -> queryString.get().isEmpty(), queryString));

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        loadFilters();

        filtersComboBox.getSelectionModel().select(noFilter);
    }

    public void openSearchWindow() {
        saveAndRestoreController.openSearchWindow();
    }

    public void setSaveAndRestoreController(SaveAndRestoreController saveAndRestoreController) {
        this.saveAndRestoreController = saveAndRestoreController;
    }

    private void loadFilters() {
        try {
            List<Filter> filters = saveAndRestoreService.getAllFilters();
            filters.add(noFilter);
            filtersComboBox.getItems().setAll(filters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void saveFilter() {
        Filter filter = new Filter();
        filter.setName("test");
        filter.setQueryString(queryString.get());

        try {
            saveAndRestoreService.saveFilter(filter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void deleteFilter() {

    }

    @FXML
    public void search() {

    }
}
