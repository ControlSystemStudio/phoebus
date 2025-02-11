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
 * FXML Controller for the open file action editor.
 */
public class OpenFileActionController extends ActionControllerBase {

    private final Widget widget;

    @SuppressWarnings("unused")
    @FXML
    private TextField filePath;

    private final StringProperty filePathProperty = new SimpleStringProperty();

    private final Logger logger =
            Logger.getLogger(OpenFileActionController.class.getName());

    /**
     * @param widget     Widget
     * @param openFileAction {@link ActionInfo}
     */
    public OpenFileActionController(Widget widget, OpenFileAction openFileAction) {
        this.widget = widget;
        descriptionProperty.set(openFileAction.getDescription());
        filePathProperty.set(openFileAction.getFile());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();
        filePath.textProperty().bindBidirectional(filePathProperty);
    }

    /**
     * Prompt for file
     */
    @SuppressWarnings("unused")
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

    public ActionInfo getActionInfo(){
        return new OpenFileAction(descriptionProperty.get(), filePathProperty.get());
    }
}
