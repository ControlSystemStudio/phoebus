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
 */

package org.csstudio.display.builder.representation.javafx.actionsdialog;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.csstudio.display.builder.representation.javafx.MacrosTable;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** FXML Controller */
public class OpenDisplayActionDetailsController implements ActionDetailsController{

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

    private OpenDisplayActionInfo openDisplayActionInfo;

    private StringProperty paneProperty = new SimpleStringProperty();
    private StringProperty displayPathProperty = new SimpleStringProperty();
    private StringProperty descriptionProperty = new SimpleStringProperty();

    private OpenDisplayActionInfo.Target target;

    private Widget widget;

    /** @param widget Widget
     *  @param actionInfo ActionInfo
     */
    public OpenDisplayActionDetailsController(Widget widget, ActionInfo actionInfo){
        this.widget = widget;
        this.openDisplayActionInfo = (OpenDisplayActionInfo)actionInfo;
    }

    /** Init */
    @FXML
    public void initialize(){
        replaceRadioButton.setUserData(Target.REPLACE);
        newTabRadioButton.setUserData(Target.TAB);
        newWindowRadioButton.setUserData(Target.WINDOW);

        ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(replaceRadioButton, newTabRadioButton, newWindowRadioButton);
        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle t1) {
                target = (Target)t1.getUserData();
            }
        });
        target = openDisplayActionInfo.getTarget();
        /*
         * Standalone is a deprecated name for Window
         */
        if (target == Target.STANDALONE)
            target = Target.WINDOW;
        toggleGroup.selectToggle(toggleGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(target)).findFirst().get());

        descriptionProperty.setValue(openDisplayActionInfo.getDescription());
        description.textProperty().bindBidirectional(descriptionProperty);

        paneProperty.setValue(openDisplayActionInfo.getPane());
        pane.textProperty().bindBidirectional(paneProperty);
        pane.disableProperty().bind(newTabRadioButton.selectedProperty().not());

        displayPathProperty.setValue(openDisplayActionInfo.getFile());
        displayPath.textProperty().bindBidirectional(displayPathProperty);

        macrosTable = new MacrosTable(openDisplayActionInfo.getMacros());
        macrosTablePlaceholder.getChildren().add(macrosTable.getNode());
        GridPane.setHgrow(macrosTable.getNode(), Priority.ALWAYS);
        VBox.setVgrow(macrosTable.getNode(), Priority.ALWAYS);
    }

    /** Prompt for filename */
    @FXML
    public void selectDisplayPath(){
        try {
            final String path = FilenameSupport.promptForRelativePath(widget, displayPathProperty.get());
            if (path != null){
                displayPathProperty.setValue(path);
            }
        } catch (Exception e) {
            Logger.getLogger(OpenDisplayActionDetailsController.class.getName())
                    .log(Level.WARNING, "Cannot prompt for filename", e);
        }
    }

    /**
     *
     * @return A new {@link ActionInfo} object as the fields in the class are read-only. Values are taken
     * from the observables in this controller.
     */
    @Override
    public ActionInfo getActionInfo(){
        return new OpenDisplayActionInfo(
                descriptionProperty.get(),
                displayPathProperty.get(),
                macrosTable.getMacros(),
                target,
                paneProperty.get());
    }
}
