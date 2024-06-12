/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
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

public class ExecuteScriptAction extends PluggableActionBase {

    private static final String EXECUTE_SCRIPT = "execute";

    private ScriptInfo scriptInfo;

    public ExecuteScriptAction(){
        this.description = Messages.ActionExecuteScript;
        this.scriptInfo = new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON,
                ScriptInfo.EXAMPLE_PYTHON,
                false,
                Collections.emptyList());
    }

    @Override
    public Image getImage() {
        if (this.image == null) {
            this.image = ImageCache.getImage(ExecuteScriptAction.class, "/icons/execute_script.png");
        }
        return this.image;
    }

    @Override
    public Node getEditor(Widget widget) {
        ResourceBundle resourceBundle = NLS.getMessages(org.csstudio.display.builder.representation.javafx.Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("ExecuteScriptActionDetails.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, PluggableActionInfo.class).newInstance(widget, ExecuteScriptAction.this);
            } catch (Exception e) {
                Logger.getLogger(ExecuteScriptAction.class.getName()).log(Level.SEVERE, "Failed to construct ExecuteScriptActionController", e);
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
        // <script file="EmbeddedPy">
        //   <text>  the embedded text  </text>
        // </script>
        final Element el = XMLUtil.getChildElement(actionXml, XMLTags.SCRIPT);
        if (el == null) {
            throw new Exception("Missing <script..>");
        } else {
            final String path = el.getAttribute(XMLTags.FILE);
            final String text = XMLUtil.getChildString(el, XMLTags.TEXT).orElse(null);
            scriptInfo = new ScriptInfo(path, text, false, Collections.emptyList());
            if (description.isEmpty()) {
                description = Messages.ActionExecuteScript;
            }
        }
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {
        writer.writeStartElement(XMLTags.ACTION);
        writer.writeAttribute(XMLTags.TYPE, EXECUTE_SCRIPT);
        writer.writeStartElement(XMLTags.SCRIPT);
        writer.writeAttribute(XMLTags.FILE, scriptInfo.getPath());
        final String text = scriptInfo.getText();
        if (text != null) {
            writer.writeStartElement(XMLTags.TEXT);
            writer.writeCData(text);
            writer.writeEndElement();
        }
        writer.writeEndElement();

        writer.writeEndElement();
    }

    @Override
    public void execute(Widget sourceWidget, Object... arguments) {

    }

    @Override
    public boolean matchesLegacyAction(String actionId) {
        return actionId.equalsIgnoreCase(EXECUTE_SCRIPT);
    }

    public ScriptInfo getScriptInfo() {
        return scriptInfo;
    }
}
