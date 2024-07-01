/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.ActionInfoBase;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.javafx.actionsdialog.ActionsDialog;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecuteCommandAction extends ActionInfoBase {

    public static final String EXECUTE_COMMAND = "command";
    private String command;
    private ExecuteCommandActionController executeCommandActionController;

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public ExecuteCommandAction() {
        this.description = Messages.ActionExecuteCommand;
        this.type = EXECUTE_COMMAND;
    }

    public ExecuteCommandAction(String description, String command) {
        this.description = description;
        this.command = command;
        this.type = EXECUTE_COMMAND;
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/execute_script.png");
    }


    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) {
        // Legacy:
        // <action type="EXECUTE_CMD">
        //   <command>echo Hello</command>
        //   <command_directory>$(user.home)</command_directory>
        //   <wait_time>10</wait_time>
        //   <description>Hello</description>
        // </action>
        //
        // New:
        // <action type="command">
        //   <command>echo Hello</command>
        //   <description>Hello</description>
        // </action>
        command = XMLUtil.getChildString(actionXml, XMLTags.COMMAND).orElse("");
        String directory = XMLUtil.getChildString(actionXml, "command_directory")
                .orElse(null);
        // Legacy allowed "opi.dir" as magic macro.
        // Commands are now by default resolved relative to the display file.
        if ("$(opi.dir)".equals(directory))
            directory = null;
        // Legacy allowed user.home as a 'current working directory'.
        // Commands are now executed with their location as cwd.
        if ("$(user.home)".equals(directory))
            directory = null;
        // If a legacy directory was provided, locate command there
        if (directory != null && !directory.isEmpty())
            command = directory + "/" + command;
        if (description.isEmpty())
            description = Messages.ActionExecuteCommand;
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {

        writer.writeAttribute(XMLTags.TYPE, EXECUTE_COMMAND);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.COMMAND);
        writer.writeCharacters(command);
        writer.writeEndElement();
    }

    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(EXECUTE_COMMAND) ||
                "EXECUTE_CMD".equalsIgnoreCase(type);
    }

    public String getCommand() {
        return command;
    }

    @Override
    public Node getEditor(Widget widget) {
        if (editorUi != null) {
            return editorUi;
        }
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("ExecuteCommandAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, ActionInfo.class).newInstance(widget, this);
            } catch (Exception e) {
                Logger.getLogger(ExecuteCommandAction.class.getName()).log(Level.SEVERE, "Failed to construct ExecuteCommandActionDetailsController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            executeCommandActionController = fxmlLoader.getController();
            return editorUi;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revert() {
        executeCommandActionController.setDescription(this.description);
        executeCommandActionController.setCommand(command);
    }

    @Override
    public ActionInfo commit() {
        description = executeCommandActionController.getDescription();
        command = executeCommandActionController.getCommand();
        return this;
    }
}
