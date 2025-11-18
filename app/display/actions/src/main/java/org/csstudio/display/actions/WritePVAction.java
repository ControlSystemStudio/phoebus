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
import java.util.logging.Level;
import java.util.logging.Logger;

public class WritePVAction extends ActionInfoBase {

    private String pv = "$(pv_name)";
    private String value = "0";

    public static final String WRITE_PV = "write_pv";
    private static final Integer PRIORITY = 20;
    private final Logger logger = Logger.getLogger(WritePVAction.class.getName());

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public WritePVAction() {
        this.description = Messages.ActionWritePV;
        this.type = WRITE_PV;
    }

    public WritePVAction(String description, String pv, String value) {
        this.description = description;
        this.pv = pv;
        this.value = value;
        this.type = WRITE_PV;
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) {
        // PV Name should be set.
        pv = XMLUtil.getChildString(actionXml, XMLTags.PV_NAME).orElse("");
        if (pv.isEmpty()) {
            logger.log(Level.WARNING, "Ignoring <action type='" + WRITE_PV + "'> with empty <pv_name> on widget");
        }

        // PV may be empty to write "".
        // In contrast to legacy opibuilder the value is _not_ trimmed,
        // so it's possible to write "   " (which opibuilder wrote as "")
        value = XMLUtil.getChildString(actionXml, XMLTags.VALUE).orElse("");
        if (description.isEmpty()) {
            description = Messages.ActionWritePV;
        }
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {

        writer.writeAttribute(XMLTags.TYPE, WRITE_PV);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.PV_NAME);
        writer.writeCharacters(pv);
        writer.writeEndElement();
        writer.writeStartElement(XMLTags.VALUE);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/write_pv.png");
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(WRITE_PV);
    }

    public String getPV() {
        return pv;
    }

    public String getValue() {
        return value;
    }

    public String getPv() {
        return pv;
    }

    public void setPv(String pv) {
        this.pv = pv;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
