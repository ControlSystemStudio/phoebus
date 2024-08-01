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

public class OpenWebPageAction extends ActionInfoBase {

    public static final String OPEN_WEBPAGE = "open_webpage";
    private static final Integer PRIORITY = 60;
    private String url;
    private OpenWebPageActionController openWebPageController;

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public OpenWebPageAction() {
        this.description = Messages.ActionOpenWebPage;
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
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(OPEN_WEBPAGE);
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

    @Override
    public Node getEditor(Widget widget){
        if(editorUi != null){
            return editorUi;
        }
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("OpenWebPageAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(ActionInfo.class).newInstance(this);
            } catch (Exception e) {
                Logger.getLogger(OpenWebPageAction.class.getName())
                        .log(Level.SEVERE, "Failed to construct OpenWebPageActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            openWebPageController = fxmlLoader.getController();
            return editorUi;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revert(){
        openWebPageController.setDescription(description);
        openWebPageController.setUrl(url);
    }

    @Override
    public ActionInfo commit(){
        description = openWebPageController.getDescription();
        url = openWebPageController.getUrl();
        return this;
    }
}
