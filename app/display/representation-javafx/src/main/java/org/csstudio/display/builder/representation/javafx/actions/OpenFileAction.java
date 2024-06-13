/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.representation.javafx.actions;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenFileAction extends PluggableActionBase {

    private static final String OPEN_FILE = "open_file";
    private String file;

    public OpenFileAction() {
        this.description = Messages.ActionOpenFile;
    }

    @Override
    public Node getEditor(Widget widget) {
        ResourceBundle resourceBundle = NLS.getMessages(org.csstudio.display.builder.representation.javafx.Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("OpenFileActionDetails.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, PluggableActionInfo.class).newInstance(widget, OpenFileAction.this);
            } catch (Exception e) {
                Logger.getLogger(OpenFileAction.class.getName())
                        .log(Level.SEVERE, "Failed to construct OpenFileDetailsController", e);
            }
            return null;
        });

        try {
            return fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) throws Exception {
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
        return ImageCache.getImage(OpenDisplayAction.class, "/icons/open_file.png");
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
