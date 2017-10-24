/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPVName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propRotationStep;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propText;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVWritable;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Widget that provides button for invoking actions.
 *
 *  <B>Note:</B> this class cannot inherit from {@link PVWidget}
 *  because doesn't need active border and the "runtime value".
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionButtonWidget extends VisibleWidget
{
    public static final int DEFAULT_WIDTH = 100,
                            DEFAULT_HEIGHT = 30;

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("action_button", WidgetCategory.CONTROL,
                    "Action Button",
                    "/icons/action_button.png",
                    "Button that can open related displays or write PVs",
                    Arrays.asList("org.csstudio.opibuilder.widgets.ActionButton",
                                  "org.csstudio.opibuilder.widgets.MenuButton",
                                  "org.csstudio.opibuilder.widgets.NativeButton"))
    {
        @Override
        public Widget createWidget()
        {
            return new ActionButtonWidget();
        }
    };

    /** Custom configurator to read legacy *.opi files */
    private static class ActionButtonConfigurator extends WidgetConfigurator
    {
        public ActionButtonConfigurator(final Version xml_version)
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
                //Legacy Menu Buttons with actions from PV should be processed as combo boxes, not action buttons
                final Element frompv_el = XMLUtil.getChildElement(xml, "actions_from_pv");
                if ((frompv_el == null) || (XMLUtil.getString(frompv_el).equals("true")))
                    return false;

                //Menu buttons used "label" instead of text
                final Element label_el = XMLUtil.getChildElement(xml, "label");

                if (label_el != null)
                {
                    final Document doc = xml.getOwnerDocument();
                    Element the_text = doc.createElement(propText.getName());

                    if (label_el.getFirstChild() != null)
                    {
                        the_text.appendChild(label_el.getFirstChild().cloneNode(true));
                    }
                    else
                    {
                        Text the_label = doc.createTextNode("Menu Button Label");
                        the_text.appendChild(the_label);
                    }
                    xml.appendChild(the_text);
                }
            }

            super.configureFromXML(model_reader, widget, xml);

            final ActionButtonWidget button = (ActionButtonWidget)widget;
            final MacroizedWidgetProperty<String> tooltip = (MacroizedWidgetProperty<String>) button.propTooltip();
            if (xml_version.getMajor() < 3)
                // See getInitialTooltip()
                tooltip.setSpecification(tooltip.getSpecification().replace("pv_value", "actions"));
            // If there is no pv_name, remove from tool tip
            if ( ((MacroizedWidgetProperty<String>)button.pv_name).getSpecification().isEmpty())
                tooltip.setSpecification(tooltip.getSpecification().replace("$(pv_name)\n", ""));

            return true;
        }
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new ActionButtonConfigurator(persisted_version);
    }

    // Has pv_name and pv_writable, but no pv_value which would make it a WritablePVWidget
    private volatile WidgetProperty<String> pv_name;
    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<String> text;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<RotationStep> rotation_step;
    private volatile WidgetProperty<Boolean> pv_writable;

    public ActionButtonWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /** org.csstudio.opibuilder.widgets.ActionButton used 2.0.0 */
    private static final Version VERSION = Version.parse("3.0.0");

    /** @return Widget version number */
    @Override
    public Version getVersion()
    {
        return VERSION;
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(pv_name = propPVName.createProperty(this, ""));
        properties.add(text = propText.createProperty(this, "$(actions)"));
        properties.add(font = propFont.createProperty(this, NamedWidgetFonts.DEFAULT));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BUTTON_BACKGROUND)));
        properties.add(rotation_step = propRotationStep.createProperty(this, RotationStep.NONE));
        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(pv_writable = runtimePropPVWritable.createProperty(this, true));
    }

    @Override
    protected String getInitialTooltip()
    {
        // Default would show $(pv_value), which doesn't exist for this widget.
        // Use $(actions) instead.
        return "$(pv_name)\n$(actions)";
    }

    /** @return 'pv_name' property */
    public final WidgetProperty<String> propPVName ( ) {
        return pv_name;
    }

    /** @return 'text' property */
    public WidgetProperty<String> propText()
    {
        return text;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'rotation_step' property */
    public WidgetProperty<RotationStep> propRotationStep()
    {
        return rotation_step;
    }

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }

    /** @return 'pv_writable' property */
    public final WidgetProperty<Boolean> runtimePropPVWritable()
    {
        return pv_writable;
    }
}
