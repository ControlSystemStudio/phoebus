/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.model.properties;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.spi.ActionInfo;

import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility base class taking care of common aspects of an {@link ActionInfo} implementation.
 */
public abstract class ActionInfoBase implements ActionInfo {

    protected String description;
    protected String type;
    protected Node editorUi;

    private static final Logger logger = Logger.getLogger(ActionInfoBase.class.getName());

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getType() {
        return type;
    }

    
    protected MenuItem createMenuItem(final Widget widget, final String description) {
        // Expand macros in action description
        String desc;
        try {
            desc = MacroHandler.replace(widget.getEffectiveMacros(), description);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Cannot expand macros in action description '" + description + "'", ex);
            desc = description;
        }

        final ImageView icon = new ImageView(getImage());
        final MenuItem item = new MenuItem(desc, icon);

        final Optional<WidgetProperty<Boolean>> enabled_prop = widget.checkProperty(CommonWidgetProperties.propEnabled);
        if (enabled_prop.isPresent() && !enabled_prop.get().getValue()) {
            item.setDisable(true);
            return item;
        }

        return item;
    }



    @Override
    public String toString() {
        return description;
    }

    /**
     * Writes the description tag if non-empty, as it is common for all {@link ActionInfo}s.
     *
     * @param writer A {@link XMLStreamWriter}
     * @throws Exception upon failure...
     */
    protected void writeDescriptionToXML(final XMLStreamWriter writer, String description) throws Exception {
        if (!description.isEmpty()) {
            writer.writeStartElement(XMLTags.DESCRIPTION);
            writer.writeCharacters(description);
            writer.writeEndElement();
        }
    }

    /**
     * Adds a single menu item corresponding to the action's description. If the {@link ActionInfo} implementation
     * needs additional items, it should override this method.
     * @param widget Widget associated with the context menu.
     * @return A list of {@link MenuItem}s.
     */
    @Override
    public List<MenuItem> getContextMenuItems(Widget widget) {
        List<MenuItem> items = new ArrayList<>();
        items.add(createMenuItem(widget, description));

        return items;
    }
}
