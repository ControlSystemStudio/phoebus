/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;

import javafx.scene.image.Image;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.ActionInfoBase;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.representation.javafx.actionsdialog.ActionsDialog;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.util.Collections;

public class ExecuteScriptAction extends ActionInfoBase {

    public static final String EXECUTE_SCRIPT = "execute";
    public static final String EXECUTE_PYTHONSCRIPT = "EXECUTE_PYTHONSCRIPT";
    public static final String EXECUTE_JAVASCRIPT = "EXECUTE_JAVASCRIPT";

    private static final Integer PRIORITY = 30;

    private ScriptInfo scriptInfo;
    private String text;
    private String path;

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public ExecuteScriptAction() {
        this.description = Messages.ActionExecuteScript;
        this.type = EXECUTE_SCRIPT;
        this.scriptInfo = new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON,
                ScriptInfo.EXAMPLE_PYTHON,
                false,
                Collections.emptyList());
    }

    public ExecuteScriptAction(String description, ScriptInfo scriptInfo) {
        this.description = description;
        this.type = EXECUTE_SCRIPT;
        this.scriptInfo = scriptInfo;
        this.text = scriptInfo.getText();
        this.path = scriptInfo.getPath();
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/execute_script.png");
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) throws Exception {
        String type = actionXml.getAttribute(XMLTags.TYPE);
        if (type.equalsIgnoreCase(EXECUTE_SCRIPT)) {
            // <script file="EmbeddedPy">
            //   <text>  the embedded text  </text>
            // </script>
            final Element el = XMLUtil.getChildElement(actionXml, XMLTags.SCRIPT);
            if (el == null) {
                throw new Exception("Missing <script..>");
            } else {
                path = el.getAttribute(XMLTags.FILE);
                if (ScriptInfo.EMBEDDED_PYTHON.equals(path) || ScriptInfo.EMBEDDED_JAVASCRIPT.equals(path)) {
                    text = XMLUtil.getChildString(el, XMLTags.TEXT).orElse(null);
                }
                scriptInfo = new ScriptInfo(path, text, false, Collections.emptyList());
                if (description.isEmpty()) {
                    description = Messages.ActionExecuteScript;
                }
            }
        } else if (type.equalsIgnoreCase(EXECUTE_JAVASCRIPT) ||
                type.equalsIgnoreCase(EXECUTE_PYTHONSCRIPT)) {
            // Legacy XML:
            // <action type="EXECUTE_PYTHONSCRIPT"> .. or "EXECUTE_JAVASCRIPT"
            //     <path>script.py</path>
            //     <scriptText><![CDATA[ /* The script */ ]]></scriptText>
            //     <embedded>false</embedded>
            //     <description>A script</description>
            // </action>
            final boolean embed = Boolean.parseBoolean(XMLUtil.getChildString(actionXml, "embedded").orElse("false"));
            path = XMLUtil.getChildString(actionXml, XMLTags.PATH).orElse("");
            text = XMLUtil.getChildString(actionXml, "scriptText").orElse("");
            if (embed) {
                final String dialect = type.contains("PYTHON")
                        ? ScriptInfo.EMBEDDED_PYTHON : ScriptInfo.EMBEDDED_JAVASCRIPT;
                scriptInfo = new ScriptInfo(dialect, text, false, Collections.emptyList());
            } else
                scriptInfo = new ScriptInfo(path, null, false, Collections.emptyList());
            if (description.isEmpty()) {
                description = Messages.ActionExecuteScript;
            }
        }
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {

        writer.writeAttribute(XMLTags.TYPE, EXECUTE_SCRIPT);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.SCRIPT);
        writer.writeAttribute(XMLTags.FILE, scriptInfo.getPath());
        // The controller updates the text and path not the scriptInfo
        if (scriptInfo.getPath().equals(ScriptInfo.EMBEDDED_PYTHON) ||
                scriptInfo.getPath().equals(ScriptInfo.EMBEDDED_JAVASCRIPT)) {
            final String text = this.text;
            if (text != null) {
                writer.writeStartElement(XMLTags.TEXT);
                writer.writeCData(text);
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(EXECUTE_SCRIPT) ||
                actionId.equalsIgnoreCase(EXECUTE_PYTHONSCRIPT) ||
                actionId.equalsIgnoreCase(EXECUTE_JAVASCRIPT);
    }
    
    public ScriptInfo getScriptInfo() {
        return scriptInfo;
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }
}
