/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.csstudio.display.builder.model.ActionControllerBase;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.csstudio.display.builder.representation.javafx.MacrosTable;
import org.phoebus.framework.macros.Macros;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML controller for the open display script action editor.
 */
public class OpenDisplayActionController extends ActionControllerBase {
    @FXML
    private RadioButton replaceRadioButton;
    @FXML
    private RadioButton newTabRadioButton;
    @FXML
    private RadioButton newWindowRadioButton;
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

    private OpenDisplayAction.Target target;

    private final Widget widget;

    /**
     * @param widget     Widget
     * @param actionInfo {@link ActionInfo}
     */
    public OpenDisplayActionController(Widget widget, ActionInfo actionInfo) {
        this.widget = widget;
        this.openDisplayActionInfo = (OpenDisplayAction) actionInfo;
        descriptionProperty.set(actionInfo.getDescription());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();

        paneProperty.set(openDisplayActionInfo.getPane());

        replaceRadioButton.setUserData(OpenDisplayAction.Target.REPLACE);
        newTabRadioButton.setUserData(OpenDisplayAction.Target.TAB);
        newWindowRadioButton.setUserData(OpenDisplayAction.Target.WINDOW);

        ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(replaceRadioButton, newTabRadioButton, newWindowRadioButton);

        target = openDisplayActionInfo.getTarget();
        /*
         * Standalone is a deprecated name for Window
         */
        if (target == OpenDisplayAction.Target.STANDALONE) {
            target = OpenDisplayAction.Target.WINDOW;
        }

        toggleGroup.selectToggle(toggleGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(target)).findFirst().get());
        toggleGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            target = (OpenDisplayAction.Target)n.getUserData();
        });

        paneProperty.set(openDisplayActionInfo.getPane());
        pane.textProperty().bindBidirectional(paneProperty);
        pane.disableProperty().bind(newTabRadioButton.selectedProperty().not());

        displayPathProperty.setValue(openDisplayActionInfo.getFile());
        displayPath.textProperty().bindBidirectional(displayPathProperty);

        macrosTable = new MacrosTable(openDisplayActionInfo.getMacros());
        macrosTablePlaceholder.getChildren().add(macrosTable.getNode());
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
            Logger.getLogger(OpenDisplayActionController.class.getName())
                    .log(Level.WARNING, "Cannot prompt for filename", e);
        }
    }

    public String getDisplayPath(){
        return displayPathProperty.get();
    }

    public String getPane(){
        return paneProperty.get();
    }

    public OpenDisplayAction.Target getTarget(){
        return target;
    }

    public Macros getMacros(){
        return macrosTable.getMacros();
    }

    public void setDisplayPath(String displayPath) {
        this.displayPathProperty.set(displayPath);
    }

    public void setPane(String pane) {
        this.paneProperty.set(pane);
    }

    public void setMacros(Macros macros) {
        if(macros != null){
            this.macrosTable.setMacros(macros);
        }
    }

    public void setTarget(OpenDisplayAction.Target target) {
        this.target = target;
    }
}
