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

public class OpenWebPageAction extends ActionInfoBase {

    public static final String OPEN_WEBPAGE = "open_webpage";
    private static final Integer PRIORITY = 60;
    private String url;

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public OpenWebPageAction() {
        this.description = Messages.ActionOpenWebPage;
        this.type = OPEN_WEBPAGE;
    }

    public OpenWebPageAction(String description, String url) {
        this.description = description;
        this.url = url;
        this.type = OPEN_WEBPAGE;
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) {
        url = XMLUtil.getChildString(actionXml, XMLTags.URL)
                .orElse(XMLUtil.getChildString(actionXml, "hyperlink")
                        .orElse(""));
        if (description.isEmpty()) {
            description = Messages.ActionOpenWebPage;
        }
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {

        writer.writeAttribute(XMLTags.TYPE, OPEN_WEBPAGE);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.URL);
        writer.writeCharacters(url);
        writer.writeEndElement();
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/web_browser.png");
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }
}
