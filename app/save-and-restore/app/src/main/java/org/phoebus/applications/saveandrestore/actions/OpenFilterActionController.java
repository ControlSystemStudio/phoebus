/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;

import java.util.List;
import java.util.Optional;

public class OpenFilterActionController {

    private OpenFilterAction openFilterAction;

    @FXML
    private ComboBox<Filter> filterId;

    private SimpleListProperty<Filter> filters = new SimpleListProperty<>();

    public OpenFilterActionController(ActionInfo actionInfo){
        this.openFilterAction = (OpenFilterAction)actionInfo;
    }

    @FXML
    public void initialize(){

        filterId.itemsProperty().bindBidirectional(filters);

        filterId.setConverter(new StringConverter<Filter>() {
            @Override
            public String toString(Filter filter) {
                return filter == null ? null : filter.getName();
            }

            @Override
            public Filter fromString(String string) {
                return filters.stream().filter(f -> f.getName().equalsIgnoreCase(openFilterAction.getFilterId())).findFirst().get();
            }
        });

        JobManager.schedule("Get Save-And-Restore Filters", monitor -> {
            SaveAndRestoreService saveAndRestoreService = SaveAndRestoreService.getInstance();
            try {
                filters.set(FXCollections.observableArrayList(saveAndRestoreService.getAllFilters()));
                Optional<Filter> filter = filters.stream().filter(f -> f.getName().equalsIgnoreCase(openFilterAction.getFilterId())).findFirst();
                if(filter.isPresent()){
                    Platform.runLater(() -> filterId.getSelectionModel().select(filter.get()));
                }
                else if(!filters.isEmpty()){
                    Platform.runLater(() -> filters.get(0));
                }
            } catch (Exception e) {
                return;
            }
        });
    }

    public Filter getSelectedFilter(){
        return filterId.getSelectionModel().getSelectedItem();
    }
}
