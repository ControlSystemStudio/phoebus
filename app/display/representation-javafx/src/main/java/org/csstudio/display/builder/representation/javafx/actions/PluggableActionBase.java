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

import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;

import javax.xml.stream.XMLStreamWriter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PluggableActionBase implements PluggableActionInfo {

    protected String description;
    protected Image image;
    protected String type;

    private static final Logger logger = Logger.getLogger(PluggableActionBase.class.getName());

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public String getType() {
        return type;
    }

    /**
     * Writes the description tag if non-empty, as it is common for all {@link PluggableActionInfo}s.
     *
     * @param writer A {@link XMLStreamWriter}
     * @throws Exception upon failure...
     */
    protected void writeDescriptionToXML(final XMLStreamWriter writer) throws Exception {
        if (!description.isEmpty()) {
            writer.writeStartElement(XMLTags.DESCRIPTION);
            writer.writeCharacters(description);
            writer.writeEndElement();
        }
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
}
