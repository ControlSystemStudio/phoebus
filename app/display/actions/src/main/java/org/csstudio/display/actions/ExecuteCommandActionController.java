/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.ActionControllerBase;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller for the execute command action editor
 */
public class ExecuteCommandActionController extends ActionControllerBase {

    private final Widget widget;

    @SuppressWarnings("unused")
    @FXML
    private TextField command;

    private final StringProperty commandProperty = new SimpleStringProperty();

    private static final Logger logger =
            Logger.getLogger(ExecuteCommandActionController.class.getName());

    public ExecuteCommandActionController(Widget widget, ExecuteCommandAction executeCommandActionInfo){
        this.widget = widget;
        descriptionProperty.set(executeCommandActionInfo.getDescription());
        commandProperty.set(executeCommandActionInfo.getCommand());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();
        command.textProperty().bindBidirectional(commandProperty);
    }

    /**
     * Prompt for command to execute
     */
    @SuppressWarnings("unused")
    @FXML
    public void selectCommand() {
        try {
            final String path = FilenameSupport.promptForRelativePath(widget, commandProperty.get());
            if (path != null) {
                commandProperty.set(path);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Cannot prompt for command/filename", ex);
        }
    }

    public ActionInfo getActionInfo(){
        return new ExecuteCommandAction(descriptionProperty.get(), commandProperty.get());
    }
}
