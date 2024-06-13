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
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.macros.MacroXMLUtil;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenDisplayAction extends PluggableActionBase {

    /**
     * Default {@link Target} is new tab.
     */
    private Target target = Target.TAB;

    private Macros macros;

    private String pane;

    private String file;

    private static final String OPEN_DISPLAY = "open_display";

    private static final Logger logger = Logger.getLogger(OpenDisplayAction.class.getName());

    public enum Target {
        /**
         * Replace current display
         */
        REPLACE(Messages.Target_Replace),

        /**
         * Open a new tab in existing window
         */
        TAB(Messages.Target_Tab),

        /**
         * Open a new window
         */
        WINDOW(Messages.Target_Window),

        /**
         * Open standalone window
         *
         * @deprecated Was only used in RCP version.
         */
        @Deprecated
        STANDALONE(Messages.Target_Standalone);

        private final String name;

        Target(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public OpenDisplayAction() {
        this.description = Messages.ActionOpenDisplay;
        this.type = OPEN_DISPLAY;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Macros getMacros() {
        return macros == null ? new Macros() : macros;
    }

    public void setMacros(Macros macros) {
        this.macros = macros;
    }

    public String getPane() {
        return pane;
    }

    public void setPane(String pane) {
        this.pane = pane;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(OPEN_DISPLAY) || actionId.equalsIgnoreCase("OPEN_OPI_IN_VIEW");
    }

    @Override
    public Node getEditor(Widget widget) {
        ResourceBundle resourceBundle = NLS.getMessages(org.csstudio.display.builder.representation.javafx.Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("OpenDisplayActionDetails.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, PluggableActionInfo.class).newInstance(widget, OpenDisplayAction.this);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to construct ExecuteScriptActionDetailsController", e);
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
        Optional<String> targetOptional = XMLUtil.getChildString(actionXml, XMLTags.TARGET);
        if (targetOptional.isPresent()) {
            target = OpenDisplayAction.Target.valueOf(targetOptional.get().toUpperCase());
        } else {
            Optional<String> replace = XMLUtil.getChildString(actionXml, "replace");
            if (replace.isPresent()) {
                if ("0".equals(replace.get())) {
                    target = OpenDisplayAction.Target.TAB;
                } else if ("2".equals(replace.get())) {
                    target = OpenDisplayAction.Target.WINDOW;
                }
            } else {
                Optional<String> mode = XMLUtil.getChildString(actionXml, "mode");
                mode.ifPresent(s -> target = modeToTargetConvert(Integer.parseInt(s)));
            }
        }

        // Use <file>, falling back to legacy <path>
        file = XMLUtil.getChildString(actionXml, XMLTags.FILE)
                .orElse(XMLUtil.getChildString(actionXml, XMLTags.PATH)
                        .orElse(""));

        final Element macroXml = XMLUtil.getChildElement(actionXml, XMLTags.MACROS);
        if (macroXml != null) {
            macros = MacroXMLUtil.readMacros(macroXml);
        } else {
            macros = new Macros();
        }

        pane = XMLUtil.getChildString(actionXml, XMLTags.NAME).orElse("");
    }

    @Override
    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {

        writer.writeAttribute(XMLTags.TYPE, OPEN_DISPLAY);
        writeDescriptionToXML(writer, description);
        writer.writeStartElement(XMLTags.FILE);
        writer.writeCharacters(file);
        writer.writeEndElement();
        if (macros != null && !macros.getNames().isEmpty()) {
            writer.writeStartElement(XMLTags.MACROS);
            MacroXMLUtil.writeMacros(writer, macros);
            writer.writeEndElement();
        }
        writer.writeStartElement(XMLTags.TARGET);
        writer.writeCharacters(target.name().toLowerCase());
        writer.writeEndElement();
        if (pane != null && !pane.isEmpty()) {
            writer.writeStartElement(XMLTags.NAME);
            writer.writeCharacters(pane);
            writer.writeEndElement();
        }
    }

    @Override
    public void setModifiers(final MouseEvent event) {
        boolean middle_click = event.isMiddleButtonDown();
        if (event.isShortcutDown() || middle_click) {
            target = Target.TAB;
        } else if (event.isShiftDown()) {
            target = Target.WINDOW;
        }
    }

    @Override
    public List<MenuItem> getContextMenuItems(Widget widget) {
        List<MenuItem> items = new ArrayList<>();
        String desc;
        try {
            desc = MacroHandler.replace(widget.getEffectiveMacros(), description);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Cannot expand macros in action description '" + description + "'", ex);
            desc = description;
        }
        items.add(createMenuItem(widget, desc));

        // Add variant for all the available Target types: Replace, new Tab, ...
        for (OpenDisplayAction.Target target : OpenDisplayAction.Target.values()) {
            if (target == OpenDisplayAction.Target.STANDALONE || target == this.target)
                continue;
            // Mention non-default targets in the description
            items.add(createMenuItem(widget, desc + " (" + target + ")"));
        }

        return items;
    }

    private OpenDisplayAction.Target modeToTargetConvert(int mode) {
        return switch (mode) {
            // 0 - REPLACE
            case 0 -> Target.REPLACE;
            // 7 - NEW_WINDOW
            // 8 - NEW_SHELL
            case 7, 8 -> Target.WINDOW;
            // 1 - NEW_TAB
            // 2 - NEW_TAB_LEFT
            // 3 - NEW_TAB_RIGHT
            // 4 - NEW_TAB_TOP
            // 5 - NEW_TAB_BOTTOM
            // 6 - NEW_TAB_DETACHED
            default -> Target.TAB;
        };
    }

    @Override
    public String toString() {
        if (getDescription().isEmpty())
            return "Open " + file;
        else
            return description;
    }

    @Override
    public Image getImage() {
        return ImageCache.getImage(OpenDisplayAction.class, "/icons/open_display.png");
    }
}
