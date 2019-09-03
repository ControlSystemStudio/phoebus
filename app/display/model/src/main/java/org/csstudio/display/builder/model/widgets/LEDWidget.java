/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBit;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLabelsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOffColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOffLabel;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOnColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOnLabel;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propSquare;

import java.util.Arrays;
import java.util.List;

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
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays an LED which reflects the on/off state of a PV
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LEDWidget extends BaseLEDWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("led", WidgetCategory.MONITOR,
            "LED",
            "/icons/led.png",
            "LED that represents on/off",
            Arrays.asList("org.csstudio.opibuilder.widgets.LED"))
    {
        @Override
        public Widget createWidget()
        {
            return new LEDWidget();
        }
    };

    /** Custom configurator to read legacy *.opi files */
    private static class LEDConfigurator extends WidgetConfigurator
    {
        public LEDConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element xml)
                throws Exception
        {
            // Legacy XML with <state_count> identifies MultiStateLEDWidget
            final Element element = XMLUtil.getChildElement(xml, "state_count");
            if (element != null)
                return false;

            super.configureFromXML(model_reader, widget, xml);

            final LEDWidget led = (LEDWidget) widget;
            handle_legacy_LED(model_reader, led, xml_version, xml);

            // If legacy widgets was configured to not use labels, clear them
            XMLUtil.getChildBoolean(xml, "show_boolean_label").ifPresent(show ->
            {
                if (! show)
                {
                    led.propOffLabel().setValue("");
                    led.propOnLabel().setValue("");
                }
            });
            return true;
        }
    }

    private volatile WidgetProperty<Integer> bit;
    private volatile WidgetProperty<String> off_label;
    private volatile WidgetProperty<WidgetColor> off_color;
    private volatile WidgetProperty<String> on_label;
    private volatile WidgetProperty<WidgetColor> on_color;
    private volatile WidgetProperty<Boolean> labels_from_pv;

    public LEDWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(bit = propBit.createProperty(this, -1));
        properties.add(off_label = propOffLabel.createProperty(this, ""));
        properties.add(off_color = propOffColor.createProperty(this, WidgetColorService.resolve(new NamedWidgetColor("Off", 60, 100, 60))));
        properties.add(on_label = propOnLabel.createProperty(this, ""));
        properties.add(on_color = propOnColor.createProperty(this, WidgetColorService.resolve(new NamedWidgetColor("On", 0, 255, 0))));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(line_color = propLineColor.createProperty(this, new WidgetColor(50, 50, 50, 178)));
        properties.add(square = propSquare.createProperty(this, false));
        // Ideally, widgets should fetch their information from a PV,
        // but the LED does not allow much room for text,
        // so the default text from the PV is likely too large..
        properties.add(labels_from_pv = propLabelsFromPV.createProperty(this, false));
    }

    /** @return 'bit' property */
    public WidgetProperty<Integer> propBit()
    {
        return bit;
    }

    /** @return 'off_label' property*/
    public WidgetProperty<String> propOffLabel()
    {
        return off_label;
    }

    /** @return 'off_color' property*/
    public WidgetProperty<WidgetColor> propOffColor()
    {
        return off_color;
    }

    /** @return 'on_label' property */
    public WidgetProperty<String> propOnLabel()
    {
        return on_label;
    }

    /** @return 'off_color' property */
    public WidgetProperty<WidgetColor> propOnColor()
    {
        return on_color;
    }

    /** @return 'labels_from_pv' property */
    public WidgetProperty<Boolean> propLabelsFromPV()
    {
        return labels_from_pv;
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new LEDConfigurator(persisted_version);
    }
}
