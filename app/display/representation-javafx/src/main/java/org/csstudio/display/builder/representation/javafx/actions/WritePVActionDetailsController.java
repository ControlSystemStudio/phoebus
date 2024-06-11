/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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

package org.csstudio.display.builder.representation.javafx.actions;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;

/**
 * FXML Controller
 */
public class WritePVActionDetailsController {

    private final WritePVAction writePVActionInfo;

    @FXML
    private TextField description;
    @FXML
    private TextField pvName;
    @FXML
    private TextField pvValue;

    private final StringProperty descriptionProperty = new SimpleStringProperty();
    private final StringProperty pvNameProperty = new SimpleStringProperty();
    private final StringProperty pvValueProperty = new SimpleStringProperty();

    /**
     * @param actionInfo ActionInfo
     */
    public WritePVActionDetailsController(PluggableActionInfo actionInfo) {
        this.writePVActionInfo = (WritePVAction) actionInfo;
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        descriptionProperty.setValue(writePVActionInfo.getDescription());
        descriptionProperty.addListener((obs, o, n) -> writePVActionInfo.setDescription(n));

        pvNameProperty.setValue(writePVActionInfo.getPV());
        pvNameProperty.addListener((obs, o, n) -> writePVActionInfo.setPv(n));

        pvValueProperty.setValue(writePVActionInfo.getValue());
        pvValueProperty.addListener((obs, o, n) -> writePVActionInfo.setValue(n));

        description.textProperty().bindBidirectional(descriptionProperty);
        pvName.textProperty().bindBidirectional(pvNameProperty);
        pvValue.textProperty().bindBidirectional(pvValueProperty);

        PVAutocompleteMenu.INSTANCE.attachField(pvName);
    }
}
