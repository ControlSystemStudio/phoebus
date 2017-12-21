/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propItemsFromPV;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Widget that writes to PV from selection of items
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ComboWidget extends WritablePVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("combo", WidgetCategory.CONTROL,
            "Combo Box",
            "/icons/combo.png",
            "Writes one of a selection of options to a PV",
            Arrays.asList("org.csstudio.opibuilder.widgets.MenuButton",
                          "org.csstudio.opibuilder.widgets.combo"))
    {
        @Override
        public Widget createWidget()
        {
            return new ComboWidget();
        }
    };

    /** Custom configurator to read legacy *.opi files */
    private static class ComboConfigurator extends WidgetConfigurator
    {
        public ComboConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element xml)
                throws Exception
        {
            final String typeId = xml.getAttribute("typeId");
            final boolean is_menu = typeId.equals("org.csstudio.opibuilder.widgets.MenuButton");
            if (is_menu)
            {
                final Element frompv_el = XMLUtil.getChildElement(xml, "actions_from_pv");
                //Menu buttons used "actions_from_pv" instead of "items_from_pv"
                if (frompv_el != null)
                {
                    // Legacy Menu Buttons with actions from PV=false should be processed as action buttons,
                    // not combo boxes
                    if (XMLUtil.getString(frompv_el).equalsIgnoreCase("false"))
                        return false;

                    final Document doc = xml.getOwnerDocument();
                    Element items_from = doc.createElement(propItemsFromPV.getName());

                    if (frompv_el.getFirstChild() != null)
                    {
                        items_from.appendChild(frompv_el.getFirstChild().cloneNode(true));
                    }
                    else
                    {
                        items_from.appendChild(doc.createTextNode("true"));
                    }
                    xml.appendChild(items_from);
                }

                //TODO: read in actions as items? or just remove actions, let rep. handle? actions get written?
            }

            super.configureFromXML(model_reader, widget, xml);
            return true;
        }
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new ComboConfigurator(persisted_version);
    }

    /** 'item' property: element for list of 'items' property */
    private static final WidgetPropertyDescriptor<String> propItem =
            CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "item", Messages.ComboWidget_Item);

    /** 'items' property: list of items (string properties) for combo box */
    public static final ArrayWidgetProperty.Descriptor<WidgetProperty<String> > propItems =
            new ArrayWidgetProperty.Descriptor< WidgetProperty<String> >(WidgetPropertyCategory.BEHAVIOR, "items", Messages.ComboWidget_Items,
                                                                         (widget, index) -> propItem.createProperty(widget, "Item " + index));

    private static final WidgetPropertyDescriptor<Boolean> propEditable =
            newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "editable", Messages.WidgetProperties_Editable);

    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile ArrayWidgetProperty<WidgetProperty<String>> items;
    private volatile WidgetProperty<Boolean> items_from_pv;
    private volatile WidgetProperty<Boolean> editable;
    private volatile WidgetProperty<Boolean> enabled;

    public ComboWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 100, 30);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(font = propFont.createProperty(this, NamedWidgetFonts.DEFAULT));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BUTTON_BACKGROUND)));
        properties.add(items = propItems.createProperty(this, Collections.emptyList()));
        properties.add(items_from_pv = propItemsFromPV.createProperty(this, true));
        properties.add(editable = propEditable.createProperty(this, false));
        properties.add(enabled = propEnabled.createProperty(this, true));
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'items' property */
    public ArrayWidgetProperty<WidgetProperty<String>> propItems()
    {
        return items;
    }

    /** Convenience routine for script to fetch items
     *  @return Items currently offered by the combo
     */
    public Collection<String> getItems()
    {
        return items.getValue().stream()
                               .map(item_prop -> item_prop.getValue())
                               .collect(Collectors.toList());
    }

    /** Convenience routine for script to set items
     *  @param new_items Items to offer in combo
     */
    public void setItems(final Collection<String> new_items)
    {
        items.setValue(new_items.stream()
                                .map(item_text -> propItem.createProperty(this, item_text))
                                .collect(Collectors.toList()));
    }

    /** @return 'items_from_PV' property */
    public WidgetProperty<Boolean> propItemsFromPV()
    {
        return items_from_pv;
    }

    /** @return 'editable' property */
    public WidgetProperty<Boolean> propEditable()
    {
        return editable;
    }

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }
}
