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
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.csstudio.display.builder.representation.javafx.MacrosTable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenDisplayActionDetailsController {
    @FXML
    private RadioButton replaceRadioButton;
    @FXML
    private RadioButton newTabRadioButton;
    @FXML
    private RadioButton newWindowRadioButton;
    @FXML
    private TextField description;
    @FXML
    private TextField displayPath;
    @FXML
    private TextField pane;
    @FXML
    private VBox macrosTablePlaceholder;

    private MacrosTable macrosTable;

    private final OpenDisplayAction openDisplayActionInfo;

    private final StringProperty paneProperty = new SimpleStringProperty();
    private final StringProperty displayPathProperty = new SimpleStringProperty();
    private final StringProperty descriptionProperty = new SimpleStringProperty();

    private OpenDisplayAction.Target target;

    private final Widget widget;

    /**
     * @param widget     Widget
     * @param actionInfo ActionInfo
     */
    public OpenDisplayActionDetailsController(Widget widget, PluggableActionInfo actionInfo) {
        this.widget = widget;
        this.openDisplayActionInfo = (OpenDisplayAction) actionInfo;
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        replaceRadioButton.setUserData(OpenDisplayAction.Target.REPLACE);
        newTabRadioButton.setUserData(OpenDisplayAction.Target.TAB);
        newWindowRadioButton.setUserData(OpenDisplayAction.Target.WINDOW);

        ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(replaceRadioButton, newTabRadioButton, newWindowRadioButton);
        toggleGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            target = (OpenDisplayAction.Target) t1.getUserData();
            openDisplayActionInfo.setTarget(target);
        });

        target = openDisplayActionInfo.getTarget();
        /*
         * Standalone is a deprecated name for Window
         */
        if (target == OpenDisplayAction.Target.STANDALONE) {
            target = OpenDisplayAction.Target.WINDOW;
        }

        toggleGroup.selectToggle(toggleGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(target)).findFirst().get());

        descriptionProperty.setValue(openDisplayActionInfo.getDescription());
        description.textProperty().bindBidirectional(descriptionProperty);
        descriptionProperty.addListener((obs, o, n) -> openDisplayActionInfo.setDescription(n));

        paneProperty.setValue(openDisplayActionInfo.getPane());
        pane.textProperty().bindBidirectional(paneProperty);
        pane.disableProperty().bind(newTabRadioButton.selectedProperty().not());
        paneProperty.addListener((obs, o, n) -> openDisplayActionInfo.setPane(n));

        displayPathProperty.setValue(openDisplayActionInfo.getFile());
        displayPath.textProperty().bindBidirectional(displayPathProperty);
        displayPathProperty.addListener((obs, o, n) -> openDisplayActionInfo.setFile(n));

        macrosTable = new MacrosTable(openDisplayActionInfo.getMacros());
        macrosTablePlaceholder.getChildren().add(macrosTable.getNode());
        macrosTable.addListener(observable -> openDisplayActionInfo.setMacros(macrosTable.getMacros()));
        GridPane.setHgrow(macrosTable.getNode(), Priority.ALWAYS);
        VBox.setVgrow(macrosTable.getNode(), Priority.ALWAYS);
    }

    /**
     * Prompt for filename
     */
    @FXML
    public void selectDisplayPath() {
        try {
            final String path = FilenameSupport.promptForRelativePath(widget, openDisplayActionInfo.getFile());
            if (path != null) {
                displayPathProperty.set(path);
            }
        } catch (Exception e) {
            Logger.getLogger(OpenDisplayActionDetailsController.class.getName())
                    .log(Level.WARNING, "Cannot prompt for filename", e);
        }
    }
}
