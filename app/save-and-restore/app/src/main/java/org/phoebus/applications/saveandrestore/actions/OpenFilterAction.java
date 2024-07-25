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
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenFilterAction extends ActionInfoBase {

    private String filterId;

    private static final String OPEN_SAR_FILTER = "open_sar_filter";
    private static final String FILTER_ID_TAG = "filter_id";

    private OpenFilterActionController openFilterActionController;

    public OpenFilterAction(){
        this.description = Messages.actionOpenFilterDescription;
        this.type = OPEN_SAR_FILTER;
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(OpenFilterAction.class, "/icons/bookcase.png");
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

    @Override
    public Node getEditor(Widget widget) {
        if (editorUi != null) {
            return editorUi;
        }
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("OpenFilterAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(ActionInfo.class).newInstance(this);
            } catch (Exception e) {
                Logger.getLogger(OpenFilterAction.class.getName()).log(Level.SEVERE, "Failed to construct OpenFilterActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            openFilterActionController = fxmlLoader.getController();
            return editorUi;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revert() {
    }

    @Override
    public ActionInfo commit() {
        Filter filter = openFilterActionController.getSelectedFilter();
        if(filter != null){
            filterId = filter.getName();
        }
        return this ;
    }

    public String getFilterId() {
        return filterId;
    }
}
