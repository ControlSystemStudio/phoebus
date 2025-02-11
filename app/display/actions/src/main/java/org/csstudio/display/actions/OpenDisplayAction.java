/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;

import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.macros.MacroXMLUtil;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.ActionInfoBase;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.builder.representation.javafx.actionsdialog.ActionsDialog;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class OpenDisplayAction extends ActionInfoBase {

    /**
     * Default {@link Target} is new tab.
     */
    private Target target = Target.TAB;

    private Macros macros;

    private String pane;

    private String file;

    public static final String OPEN_DISPLAY = "open_display";

    private static final Integer PRIORITY = 10;

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

    @SuppressWarnings("unused")
    /**
     * Do not remove, needed by SPI framework.
     */
    public OpenDisplayAction() {
        this.description = Messages.ActionOpenDisplay;
        this.type = OPEN_DISPLAY;
    }

    /**
     * @param description Action description
     * @param file        Path to the display
     * @param macros      Macros
     * @param target      Where to show the display
     */
    public OpenDisplayAction(final String description, final String file, final Macros macros, final OpenDisplayAction.Target target) {
        this(description, file, macros, target, "");
    }

    /**
     * @param description Action description
     * @param file        Path to the display
     * @param macros      Macros
     * @param target      Where to show the display
     * @param pane        Pane in which to open (for target==TAB)
     */
    public OpenDisplayAction(final String description, final String file, final Macros macros, final OpenDisplayAction.Target target, final String pane) {
        this.description = description;
        this.file = Objects.requireNonNull(file);
        this.macros = macros;
        this.target = target;
        this.pane = pane;
        this.type = OPEN_DISPLAY;
    }

    public Target getTarget() {
        return target;
    }

    public Macros getMacros() {
        return macros == null ? new Macros() : macros;
    }

    public String getPane() {
        return pane;
    }

    public String getFile() {
        return file;
    }


    @Override
    public boolean matchesAction(String actionId) {
        return actionId.equalsIgnoreCase(OPEN_DISPLAY) || actionId.equalsIgnoreCase("OPEN_OPI_IN_VIEW");
    }

    @Override
    public void readFromXML(ModelReader modelReader, Element actionXml) {
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
    public List<MenuItem> getContextMenuItems(ExecutorService executorService, Widget widget) {
        ActionHandler handler = getActionHandler();
        List<MenuItem> items = new ArrayList<>();

        // Default item
        items.add(createMenuItem(executorService, widget, description));

        // Add variant for all the available Target types: Replace, new Tab, ...
        for (OpenDisplayAction.Target target : OpenDisplayAction.Target.values()) {
            if (target == OpenDisplayAction.Target.STANDALONE || target == this.target)
                continue;
            // Mention non-default targets in the description
            MenuItem additionalItem = createMenuItem(widget, description + " (" + target + ")");
            OpenDisplayAction openDisplayAction = new OpenDisplayAction(description, file, macros, target);
            additionalItem.setOnAction(ae -> executorService.execute(() -> handler.handleAction(widget, openDisplayAction)));
            items.add(additionalItem);
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
        return ImageCache.getImage(ActionsDialog.class, "/icons/open_display.png");
    }

    @Override
    public Integer getPriority() {
        return PRIORITY;
    }
}
