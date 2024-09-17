/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.ActionInfoBase;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ActionInfo} implementation for launching/highlighting the save-and-restore app
 * and bring a node, e.g. show a specific snapshot.
 */
public class OpenNodeAction extends ActionInfoBase {

    private String nodeId;

    public static final String OPEN_SAR_NODE = "open_sar_node";
    private static final String NODE_ID_TAG = "node_id";
    private static final Integer PRIORITY = 56;

    private OpenNodeActionController openNodeActionController;

    public OpenNodeAction() {
        this.description = Messages.actionOpenNodeDescription;
        this.type = OPEN_SAR_NODE;
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(OpenNodeAction.class, "/icons/bookcase.png");
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) {
        nodeId = XMLUtil.getChildString(actionXml, NODE_ID_TAG).orElse("");
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {
        writer.writeAttribute(XMLTags.TYPE, OPEN_SAR_NODE);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(NODE_ID_TAG);
        writer.writeCharacters(nodeId);
        writer.writeEndElement();

    }

    @Override
    public Node getEditor(Widget widget) {
        if (editorUi != null) {
            return editorUi;
        }
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("OpenNodeAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(ActionInfo.class).newInstance(this);
            } catch (Exception e) {
                Logger.getLogger(OpenNodeAction.class.getName()).log(Level.SEVERE, "Failed to construct OpenNodeActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            openNodeActionController = fxmlLoader.getController();
            return editorUi;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revert() {
        openNodeActionController.setInitialNodeId(nodeId);
        openNodeActionController.setDescription(description);
    }

    @Override
    public ActionInfo commit() {
        nodeId = openNodeActionController.getNodeId();
        description = openNodeActionController.getDescription();
        return this;
    }

    public String getNodeId() {
        return nodeId;
    }
}
