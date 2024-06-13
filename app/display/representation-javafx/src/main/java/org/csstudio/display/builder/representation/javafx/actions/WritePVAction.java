/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 *
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

import static org.csstudio.display.builder.model.ModelPlugin.logger;

public class WritePVAction extends PluggableActionBase {

    private String pv = "$(pv_name)";
    private String value = "0";

    private static final String WRITE_PV = "write_pv";

    private Widget widget;

    public WritePVAction() {
        this.description = Messages.ActionWritePV;
    }

    @Override
    public Node getEditor(Widget widget) {
        this.widget = widget;
        ResourceBundle resourceBundle = NLS.getMessages(org.csstudio.display.builder.representation.javafx.Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("WritePVActionDetails.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(PluggableActionInfo.class).newInstance(WritePVAction.this);
            } catch (Exception e) {
                Logger.getLogger(WritePVAction.class.getName()).log(Level.SEVERE, "Failed to construct WritePVDetailsController", e);
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
        // PV Name should be set.
        pv = XMLUtil.getChildString(actionXml, XMLTags.PV_NAME).orElse("");
        if (pv.isEmpty()) {
            logger.log(Level.WARNING, "Ignoring <action type='" + WRITE_PV + "'> with empty <pv_name> on " + widget);
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
        return ImageCache.getImage(OpenDisplayAction.class, "/icons/write_pv.png");
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
