/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;

import javafx.scene.image.Image;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.ActionInfoBase;
import org.csstudio.display.builder.representation.javafx.actionsdialog.ActionsDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;

public class CloseDisplayAction extends ActionInfoBase {

    public static final String CLOSE_DISPLAY = "close_display";
    private static final Integer PRIORITY = 10;

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public CloseDisplayAction() {
        this.description = Messages.ActionCloseDisplay;
        this.type = CLOSE_DISPLAY;
    }

    public CloseDisplayAction(String description) {
        this.description = description;
        this.type = CLOSE_DISPLAY;
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/close_display.png");
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) {
        if (description.isEmpty())
            description = Messages.ActionCloseDisplay;
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {
        writer.writeAttribute(XMLTags.TYPE, CLOSE_DISPLAY);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.COMMAND);
        writer.writeEndElement();
    }

    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(CLOSE_DISPLAY);
    }
}
