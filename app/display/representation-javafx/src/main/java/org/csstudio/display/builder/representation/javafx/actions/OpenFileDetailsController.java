/*
 * Copyright (C) 2024 European Spallation Source ERIC.
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
public class OpenFileDetailsController {

    private final OpenFileAction openFileActionInfo;
    private final Widget widget;

    @FXML
    private TextField description;
    @FXML
    private TextField filePath;

    private final StringProperty descriptionProperty = new SimpleStringProperty();
    private final StringProperty filePathProperty = new SimpleStringProperty();

    /**
     * @param widget     Widget
     * @param actionInfo ActionInfo
     */
    public OpenFileDetailsController(Widget widget, PluggableActionInfo actionInfo) {
        this.widget = widget;
        this.openFileActionInfo = (OpenFileAction) actionInfo;
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        descriptionProperty.setValue(openFileActionInfo.getDescription());
        filePathProperty.setValue(openFileActionInfo.getFile());

        description.textProperty().bindBidirectional(descriptionProperty);
        filePath.textProperty().bindBidirectional(filePathProperty);

        descriptionProperty.addListener((obs, o, n) -> openFileActionInfo.setDescription(n));
        filePathProperty.addListener((obs, o, n) -> openFileActionInfo.setFile(n));
    }

    /**
     * Prompt for file
     */
    @FXML
    public void selectFile() {
        try {
            final String path = FilenameSupport.promptForRelativePath(widget, filePathProperty.get());
            if (path != null) {
                filePathProperty.setValue(path);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Cannot prompt for filename", ex);
        }
    }
}
