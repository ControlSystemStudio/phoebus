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

    public OpenNodeAction(String description, String nodeId) {
        this.description = description;
        this.nodeId = nodeId;
        this.type = OPEN_SAR_NODE;
    }


    @Override
    public Image getImage() {
        return ImageCache.getImage(OpenNodeAction.class, "/icons/save-and-restore.png");
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

    public String getNodeId() {
        return nodeId;
    }
}
