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
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;

public class OpenFileAction extends ActionInfoBase {

    public static final String OPEN_FILE = "open_file";
    private static final Integer PRIORITY = 50;
    private String file;

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public OpenFileAction() {
        this.description = Messages.ActionOpenFile;
        this.type = OPEN_FILE;
    }

    /**
     * @param description Action description
     * @param file        Path to file to open
     */
    public OpenFileAction(final String description, final String file) {
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
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/open_file.png");
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
