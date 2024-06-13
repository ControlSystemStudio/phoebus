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
import org.csstudio.display.builder.model.properties.OpenWebpageActionInfo;
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

public class OpenWebPageAction extends PluggableActionBase {

    private static final String OPEN_WEBPAGE = "open_webpage";
    private String url;

    public OpenWebPageAction() {
        this.description = Messages.ActionOpenWebPage;
    }

    @Override
    public Node getEditor(Widget widget) {
        ResourceBundle resourceBundle = NLS.getMessages(org.csstudio.display.builder.representation.javafx.Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("OpenWebPageActionDetails.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(PluggableActionInfo.class).newInstance(OpenWebPageAction.this);
            } catch (Exception e) {
                Logger.getLogger(OpenWebPageAction.class.getName())
                        .log(Level.SEVERE, "Failed to construct OpenWebPageDetailsController", e);
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
        url = XMLUtil.getChildString(actionXml, XMLTags.URL)
                .orElse(XMLUtil.getChildString(actionXml, "hyperlink")
                        .orElse(""));
        if (description.isEmpty()){
            description = Messages.ActionOpenWebPage;
        }
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {
        writer.writeStartElement(XMLTags.ACTION);

        writer.writeAttribute(XMLTags.TYPE, OPEN_WEBPAGE);
        writer.writeStartElement(XMLTags.URL);
        writer.writeCharacters(url);
        writer.writeEndElement();

        writer.writeEndElement();
    }

    @Override
    public void execute(Widget sourceWidget, Object... arguments) {

    }

    @Override
    public boolean matchesLegacyAction(String actionId) {
        return actionId.equalsIgnoreCase(OPEN_WEBPAGE);
    }

    @Override
    public Image getImage() {
        if (this.image == null) {
            this.image = ImageCache.getImage(OpenDisplayAction.class, "/icons/web_browser.png");
        }
        return this.image;
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }
}
