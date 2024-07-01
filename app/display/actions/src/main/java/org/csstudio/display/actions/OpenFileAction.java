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

public class OpenFileAction extends ActionInfoBase {

    public static final String OPEN_FILE = "open_file";
    private String file;

    private OpenFileActionController openFileActionController;

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public OpenFileAction() {
        this.description = Messages.ActionOpenFile;
        this.type = OPEN_FILE;

    }

    /** @param description Action description
     *  @param file Path to file to open
     */
    public OpenFileAction(final String description, final String file)
    {
        this.description = description;
        this.file = file;
        this.type = OPEN_FILE;
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) {
        file = XMLUtil.getChildString(actionXml, XMLTags.FILE)
                .orElse(XMLUtil.getChildString(actionXml, XMLTags.PATH)
                        .orElse(""));
        if (description.isEmpty()) {
            description = Messages.ActionOpenFile;
        }
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {

        writer.writeAttribute(XMLTags.TYPE, OPEN_FILE);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.FILE);
        writer.writeCharacters(file);
        writer.writeEndElement();
    }

    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(OPEN_FILE);
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/open_file.png");
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public Node getEditor(Widget widget){
        if(editorUi != null){
            return editorUi;
        }
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("OpenFileAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, ActionInfo.class).newInstance(widget, this);
            } catch (Exception e) {
                Logger.getLogger(OpenFileAction.class.getName())
                        .log(Level.SEVERE, "Failed to construct OpenFileActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            openFileActionController = fxmlLoader.getController();
            return editorUi;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revert(){
        openFileActionController.setFilePath(file);
        openFileActionController.setDescription(description);
    }

    public ActionInfo commit(){
        description = openFileActionController.getDescription();
        file = openFileActionController.getFilePath();
        return this;
    }
}
