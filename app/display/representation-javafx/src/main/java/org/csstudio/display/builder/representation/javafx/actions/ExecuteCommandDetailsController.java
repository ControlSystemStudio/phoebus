/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 */

package org.csstudio.display.builder.representation.javafx.actions;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;

import java.util.logging.Level;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

/**
 * FXML Controller
 */
public class ExecuteCommandDetailsController {

    private final ExecuteCommandAction executeCommandActionInfo;

    private final Widget widget;

    @FXML
    private TextField description;
    @FXML
    private TextField command;

    private final StringProperty descriptionProperty = new SimpleStringProperty();
    private final StringProperty commandProperty = new SimpleStringProperty();

    /**
     * @param widget     Widget
     * @param actionInfo ActionInfo
     */
    public ExecuteCommandDetailsController(Widget widget, PluggableActionInfo actionInfo) {
        this.widget = widget;
        this.executeCommandActionInfo = (ExecuteCommandAction) actionInfo;
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        descriptionProperty.setValue(executeCommandActionInfo.getDescription());
        description.textProperty().bindBidirectional(descriptionProperty);
        descriptionProperty.addListener((obs, o, n) -> executeCommandActionInfo.setDescription(n));
        commandProperty.setValue(executeCommandActionInfo.getCommand());
        commandProperty.addListener((obs, o, n) -> executeCommandActionInfo.setCommand(n));

        command.textProperty().bindBidirectional(commandProperty);
    }

    /**
     * Prompt for command to execute
     */
    @FXML
    public void selectCommand() {
        try {
            final String path = FilenameSupport.promptForRelativePath(widget, commandProperty.get());
            if (path != null) {
                commandProperty.setValue(path);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Cannot prompt for command/filename", ex);
        }
    }
}
