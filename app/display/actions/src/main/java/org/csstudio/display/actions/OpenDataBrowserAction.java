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

public class OpenDataBrowserAction extends ActionInfoBase {

    public static final String OPEN_DATA_BROWSER = "open_data_browser";
    private static final Integer PRIORITY = 10;
    private String pvs = "$(pv_name)";
    private String timeframe = "1 hour";

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public OpenDataBrowserAction() {
        this.description = Messages.ActionOpenDataBrowser;
        this.type = OPEN_DATA_BROWSER;
    }

    public OpenDataBrowserAction(String description, String pvs, String timeframe) {
        this.pvs = pvs;
        this.timeframe = timeframe;
        this.description = description;
        this.type = OPEN_DATA_BROWSER;
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/databrowser.png");
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) {
        pvs = XMLUtil.getChildString(actionXml, XMLTags.PV_NAME).orElse("");
        timeframe = XMLUtil.getChildString(actionXml, XMLTags.TIME_FRAME).orElse("");

        if (description.isEmpty())
            description = Messages.ActionOpenDataBrowser;
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {
        writer.writeAttribute(XMLTags.TYPE, OPEN_DATA_BROWSER);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.PV_NAME);
        writer.writeCharacters(pvs);
        writer.writeEndElement();
        writer.writeStartElement(XMLTags.TIME_FRAME);
        writer.writeCharacters(timeframe);
        writer.writeEndElement();
    }

    public String getPVs() {
        return pvs;
    }

    public String getTimeframe() {
        return timeframe;
    }

    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(OPEN_DATA_BROWSER);
    }
}
