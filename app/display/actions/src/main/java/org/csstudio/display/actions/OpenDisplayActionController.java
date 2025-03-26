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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML controller for the open display script action editor.
 */
public class OpenDisplayActionController extends ActionControllerBase {
    @SuppressWarnings("unused")
    @FXML
    private RadioButton replaceRadioButton;
    @SuppressWarnings("unused")
    @FXML
    private RadioButton newTabRadioButton;
    @SuppressWarnings("unused")
    @FXML
    private RadioButton newWindowRadioButton;
    @SuppressWarnings("unused")
    @FXML
    private TextField displayPath;
    @SuppressWarnings("unused")
    @FXML
    private TextField pane;
    @SuppressWarnings("unused")
    @FXML
    private VBox macrosTablePlaceholder;

    private final MacrosTable macrosTable;

    private final StringProperty paneProperty = new SimpleStringProperty();
    private final StringProperty displayPathProperty = new SimpleStringProperty();

    private OpenDisplayAction.Target target;
    private final Widget widget;
    private final String file;

    /**
     * @param widget            Widget
     * @param openDisplayAction {@link ActionInfo}
     */
    public OpenDisplayActionController(Widget widget, OpenDisplayAction openDisplayAction) {
        this.widget = widget;
        this.file = openDisplayAction.getFile();
        descriptionProperty.set(openDisplayAction.getDescription());
        paneProperty.set(openDisplayAction.getPane());
        target = openDisplayAction.getTarget();
        paneProperty.set(openDisplayAction.getPane());
        displayPathProperty.setValue(openDisplayAction.getFile());
        macrosTable = new MacrosTable(openDisplayAction.getMacros());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();

        replaceRadioButton.setUserData(OpenDisplayAction.Target.REPLACE);
        newTabRadioButton.setUserData(OpenDisplayAction.Target.TAB);
        newWindowRadioButton.setUserData(OpenDisplayAction.Target.WINDOW);

        ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(replaceRadioButton, newTabRadioButton, newWindowRadioButton);

        /*
         * Standalone is a deprecated name for Window
         */
        if (target == OpenDisplayAction.Target.STANDALONE) {
            target = OpenDisplayAction.Target.WINDOW;
        }

        toggleGroup.selectToggle(toggleGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(target)).findFirst().get());
        toggleGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            target = (OpenDisplayAction.Target) n.getUserData();
        });

        pane.textProperty().bindBidirectional(paneProperty);
        pane.disableProperty().bind(newTabRadioButton.selectedProperty().not());

        displayPath.textProperty().bindBidirectional(displayPathProperty);

        macrosTablePlaceholder.getChildren().add(macrosTable.getNode());
        GridPane.setHgrow(macrosTable.getNode(), Priority.ALWAYS);
        VBox.setVgrow(macrosTable.getNode(), Priority.ALWAYS);
    }

    /**
     * Prompt for filename
     */
    @SuppressWarnings("unused")
    @FXML
    public void selectDisplayPath() {
        try {
            final String path = FilenameSupport.promptForRelativePath(widget, file);
            if (path != null) {
                displayPathProperty.set(path);
            }
        } catch (Exception e) {
            Logger.getLogger(OpenDisplayActionController.class.getName())
                    .log(Level.WARNING, "Cannot prompt for filename", e);
        }
    }

    public ActionInfo getActionInfo() {
        return new OpenDisplayAction(descriptionProperty.get(),
                displayPathProperty.get(),
                macrosTable.getMacros(),
                target,
                paneProperty.get());
    }
}
