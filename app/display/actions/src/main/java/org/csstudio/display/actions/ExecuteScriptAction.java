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
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.javafx.actionsdialog.ActionsDialog;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecuteScriptAction extends ActionInfoBase {

    public static final String EXECUTE_SCRIPT = "execute";
    public static final String EXECUTE_PYTHONSCRIPT = "EXECUTE_PYTHONSCRIPT";
    public static final String EXECUTE_JAVASCRIPT = "EXECUTE_JAVASCRIPT";

    private ScriptInfo scriptInfo;
    private String text;
    private String path;

    private ExecuteScriptActionController executeScriptController;


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
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(ActionsDialog.class, "/icons/execute_script.png");
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) throws Exception {
        if(type.equalsIgnoreCase(EXECUTE_SCRIPT)){
            // <script file="EmbeddedPy">
            //   <text>  the embedded text  </text>
            // </script>
            final Element el = XMLUtil.getChildElement(actionXml, XMLTags.SCRIPT);
            if (el == null) {
                throw new Exception("Missing <script..>");
            } else {
                path = el.getAttribute(XMLTags.FILE);
                if(ScriptInfo.EMBEDDED_PYTHON.equals(path) || ScriptInfo.EMBEDDED_JAVASCRIPT.equals(path)){
                    text = XMLUtil.getChildString(el, XMLTags.TEXT).orElse(null);
                }
                scriptInfo = new ScriptInfo(path, text, false, Collections.emptyList());
                if (description.isEmpty()) {
                    description = Messages.ActionExecuteScript;
                }
            }
        }
        else if(type.equalsIgnoreCase(EXECUTE_JAVASCRIPT) ||
                type.equalsIgnoreCase(EXECUTE_PYTHONSCRIPT)){
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
            if (embed)
            {
                final String dialect = type.contains("PYTHON")
                        ? ScriptInfo.EMBEDDED_PYTHON : ScriptInfo.EMBEDDED_JAVASCRIPT;
                scriptInfo = new ScriptInfo(dialect, text, false, Collections.emptyList());
            }
            else
                scriptInfo = new ScriptInfo(path, null, false, Collections.emptyList());
            if (description.isEmpty()){
                description = Messages.ActionExecuteScript;
            }
        }
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {

        writer.writeAttribute(XMLTags.TYPE, EXECUTE_SCRIPT);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.SCRIPT);
        writer.writeAttribute(XMLTags.FILE, path);
        if(scriptInfo.getPath().equals(ScriptInfo.EMBEDDED_PYTHON) ||
            scriptInfo.getPath().equals(ScriptInfo.EMBEDDED_JAVASCRIPT)){
            final String text = scriptInfo.getText();
            if (text != null) {
                writer.writeStartElement(XMLTags.TEXT);
                writer.writeCData(text);
                writer.writeEndElement();
            }
        }
        else{
            writer.writeAttribute(XMLTags.FILE, path);
        }
        writer.writeEndElement();
    }

    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(EXECUTE_SCRIPT) ||
                actionId.equalsIgnoreCase(EXECUTE_JAVASCRIPT) ||
                actionId.equalsIgnoreCase(EXECUTE_PYTHONSCRIPT);
    }

    public ScriptInfo getScriptInfo() {
        return scriptInfo;
    }

    @Override
    public Node getEditor(Widget widget){
        if(editorUi != null){
            return editorUi;
        }
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("ExecuteScriptActionDetails.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, ActionInfo.class).newInstance(widget, this);
            } catch (Exception e) {
                Logger.getLogger(ExecuteScriptAction.class.getName()).log(Level.SEVERE, "Failed to construct ExecuteScriptActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            executeScriptController = fxmlLoader.getController();
            return editorUi;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revert(){
        executeScriptController.setScriptPath(path);
        executeScriptController.setScriptBody(text);
    }

    @Override
    public ActionInfo commit(){
        path = executeScriptController.getScriptPath();
        text = executeScriptController.getScriptBody();
        return this;
    }
}
