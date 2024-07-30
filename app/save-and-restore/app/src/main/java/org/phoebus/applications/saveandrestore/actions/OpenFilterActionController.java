/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.csstudio.display.builder.model.ActionControllerBase;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenFilterActionController extends ActionControllerBase {

    private final OpenFilterAction openFilterAction;

    @FXML
    private ComboBox<Filter> filterId;

    private final SimpleListProperty<Filter> filters = new SimpleListProperty<>();

    public OpenFilterActionController(ActionInfo actionInfo) {
        this.openFilterAction = (OpenFilterAction) actionInfo;
        descriptionProperty.set(actionInfo.getDescription());
    }

    @FXML
    public void initialize() {
        super.initialize();
        filterId.itemsProperty().bindBidirectional(filters);

        filterId.setConverter(new StringConverter<>() {
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
                setFilter(openFilterAction.getFilterId());
            } catch (Exception e) {
                Logger.getLogger(OpenFilterActionController.class.getName()).log(Level.WARNING, "Failed to retrieve all filters");
            }
        });
    }

    public Filter getSelectedFilter() {
        return filterId.getSelectionModel().getSelectedItem();
    }

    public void setFilter(String filterId) {
        Optional<Filter> filter = filters.stream().filter(f -> f.getName().equalsIgnoreCase(filterId)).findFirst();
        if (filter.isPresent()) {
            Platform.runLater(() -> this.filterId.getSelectionModel().select(filter.get()));
        } else if (!filters.isEmpty()) {
            Platform.runLater(() -> this.filterId.getSelectionModel().select(filters.get(0)));
        }
    }
}
