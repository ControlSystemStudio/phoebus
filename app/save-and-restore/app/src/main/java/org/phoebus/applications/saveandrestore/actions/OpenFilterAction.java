/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import javafx.scene.image.Image;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.ActionInfoBase;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;

/**
 * {@link ActionInfo} implementation for launching/highlighting the save-and-restore app
 * and bring up a named filter in the search and filter view.
 */
public class OpenFilterAction extends ActionInfoBase {

    private String filterId;

    public static final String OPEN_SAR_FILTER = "open_sar_filter";
    private static final String FILTER_ID_TAG = "filter_id";
    private static final Integer PRIORITY = 55;

    @SuppressWarnings("unused")
    public OpenFilterAction() {
        this.description = Messages.actionOpenFilterDescription;
        this.type = OPEN_SAR_FILTER;
    }

    public OpenFilterAction(String description, String filterId) {
        this.description = description;
        this.filterId = filterId;
        this.type = OPEN_SAR_FILTER;
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(OpenFilterAction.class, "/icons/save-and-restore.png");
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) throws Exception {
        filterId = XMLUtil.getChildString(actionXml, FILTER_ID_TAG).orElse("");
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {
        writer.writeAttribute(XMLTags.TYPE, OPEN_SAR_FILTER);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(FILTER_ID_TAG);
        writer.writeCharacters(filterId);
        writer.writeEndElement();
    }

    public String getFilterId() {
        return filterId;
    }
}
