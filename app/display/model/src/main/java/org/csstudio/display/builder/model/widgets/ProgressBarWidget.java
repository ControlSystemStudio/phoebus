/*******************************************************************************
 * Copyright (c) 2015-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.phoebus.ui.color.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Widget that displays a progress bar
 *
 *  <p>Extends {@link ScaledPVWidget} so the bar offers the same range,
 *  scale and alarm limit properties as the {@link TankWidget}.
 *  The scale related properties only take effect with the RTTank based
 *  renderer, see {@link #SCALE_MODE_PROPS}.
 *
 *  <p>Existing {@code .bob} files load unchanged: {@code fill_color},
 *  {@code background_color}, {@code horizontal}, {@code limits_from_pv},
 *  {@code minimum}, {@code maximum} and {@code log_scale} keep their
 *  XML names. Older Phoebus versions ignore the new properties.
 *
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ProgressBarWidget extends ScaledPVWidget
{
    /** Properties that only have an effect with the RTTank based renderer
     *  ({@code progressbar_scale_mode=true}).
     *
     *  <p>The property editor hides these while the stock JavaFX renderer
     *  is in use. Properties that both renderers honour, like the range,
     *  {@code horizontal}, {@code log_scale}, fill and background color,
     *  are not listed here.
     */
    public static final Set<String> SCALE_MODE_PROPS = Set.of(
        "format", "precision", "font", "foreground_color",
        "scale_visible", "show_minor_ticks", "show_scale_labels",
        "opposite_scale_visible", "perpendicular_tick_labels",
        "inner_padding", "tank_border_width",
        "alarm_limits_from_pv", "show_alarm_limits",
        "level_lolo", "level_low", "level_high", "level_hihi",
        "minor_alarm_color", "major_alarm_color");

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("progressbar", WidgetCategory.MONITOR,
            "Progress Bar",
            "/icons/progressbar.png",
            "Bar graph widget that 'fills' relative to numeric value of a PV",
            Arrays.asList("org.csstudio.opibuilder.widgets.progressbar"))
    {
        @Override
        public Widget createWidget()
        {
            return new ProgressBarWidget();
        }
    };

    /** Widget configurator to read legacy *.opi files */
    private static class ProgressBarConfigurator extends WidgetConfigurator
    {
        public ProgressBarConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element xml)
                throws Exception
        {
            super.configureFromXML(model_reader, widget, xml);

            if (xml_version.getMajor() < 2)
            {
                final ProgressBarWidget bar = (ProgressBarWidget) widget;
                // BOY progress bar reserved room on top for limit markers,
                // and on bottom for scale
                if (XMLUtil.getChildBoolean(xml, "show_markers").orElse(true))
                {
                    // This widget has no markers on top, so move widget down and reduce height.
                    // There is no 'marker font', seems to have constant height
                    final int reduce = 25;
                    bar.propY().setValue(bar.propY().getValue() + reduce);
                    bar.propHeight().setValue(bar.propHeight().getValue() - reduce);
                }

                final Element el = XMLUtil.getChildElement(xml, "color_fillbackground");
                if (el != null)
                    bar.propBackgroundColor().readFromXML(model_reader, el);

                // BOY names that differ from ours. level_hihi and level_lolo match.
                readLegacyElement(model_reader, xml, "show_scale", bar.propScaleVisible());
                readLegacyElement(model_reader, xml, "scale_font", bar.propFont());
                readLegacyElement(model_reader, xml, "level_hi", bar.propLevelHigh());
                readLegacyElement(model_reader, xml, "level_lo", bar.propLevelLow());

                // Create a companion TextUpdate widget for the BOY value label.
                if (XMLUtil.getChildBoolean(xml, "show_label").orElse(true))
                {
                    final Document doc = xml.getOwnerDocument();
                    final Element text = doc.createElement(XMLTags.WIDGET);
                    text.setAttribute(XMLTags.TYPE, TextUpdateWidget.WIDGET_DESCRIPTOR.getType());
                    XMLUtil.updateTag(text, XMLTags.NAME, widget.getName() + " Label");
                    text.appendChild(doc.importNode(XMLUtil.getChildElement(xml, XMLTags.X), true));
                    text.appendChild(doc.importNode(XMLUtil.getChildElement(xml, XMLTags.Y), true));
                    text.appendChild(doc.importNode(XMLUtil.getChildElement(xml, XMLTags.WIDTH), true));
                    text.appendChild(doc.importNode(XMLUtil.getChildElement(xml, XMLTags.HEIGHT), true));
                    text.appendChild(doc.importNode(XMLUtil.getChildElement(xml, XMLTags.PV_NAME), true));

                    Element e = doc.createElement(CommonWidgetProperties.propTransparent.getName());
                    e.appendChild(doc.createTextNode(Boolean.TRUE.toString()));
                    text.appendChild(e);

                    e = doc.createElement(CommonWidgetProperties.propHorizontalAlignment.getName());
                    e.appendChild(doc.createTextNode(Integer.toString(HorizontalAlignment.CENTER.ordinal())));
                    text.appendChild(e);

                    xml.getParentNode().appendChild(text);
                }
            }

            return true;
        }

        private static void readLegacyElement(final ModelReader modelReader, final Element xml,
                                              final String name, final WidgetProperty<?> property) throws Exception
        {
            final Element element = XMLUtil.getChildElement(xml, name);
            if (element != null)
                property.readFromXML(modelReader, element);
        }
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new ProgressBarConfigurator(persisted_version);
    }

    private volatile WidgetProperty<WidgetColor> background_color;
    private volatile WidgetProperty<Boolean>     horizontal;
    private volatile WidgetProperty<Integer>     innerPadding;

    /** Constructor */
    public ProgressBarWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 100, 20);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        defineScaleLookProperties(properties, false, false);
        properties.add(background_color = propBackgroundColor.createProperty(this, new WidgetColor(250, 250, 250)));
        properties.add(horizontal       = propHorizontal.createProperty(this, true));
        properties.add(innerPadding     = propInnerPadding.createProperty(this, 3));
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background_color;
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }

    /** @return 'inner_padding' property */
    public WidgetProperty<Integer> propInnerPadding()
    {
        return innerPadding;
    }
}
