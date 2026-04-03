/*******************************************************************************
 * Copyright (c) 2015-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFillColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propLogscale;


import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.ui.color.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Widget that displays a progress bar with an optional numeric scale.
 *
 *  <p>Extends {@link ScaledPVWidget} to inherit common scale/limit properties
 *  (min/max range, format, precision, alarm limit lines).  The bar uses the
 *  same rendering engine as {@link TankWidget} ({@code RTTank}) so the scale
 *  and alarm-limit features are identical.
 *
 *  <p>Existing {@code .bob} files load unchanged: {@code fill_color},
 *  {@code background_color}, {@code horizontal}, {@code limits_from_pv},
 *  {@code minimum}, {@code maximum} and {@code log_scale} keep the same
 *  XML names.  New properties ({@code format}, {@code precision},
 *  {@code scale_visible}, {@code show_minor_ticks}, alarm limit properties)
 *  are silently ignored by older Phoebus versions.
 *
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 *  @author Heredie Delvalle &mdash; CLS, ScaledPVWidget refactoring, scale support
 */
@SuppressWarnings("nls")
public class ProgressBarWidget extends ScaledPVWidget
{
    /** Property names that only take effect when the RTTank-based rendering engine is
     *  active ({@code progressbar_scale_mode=true}).  The property editor uses this set
     *  to hide irrelevant entries when the legacy JFX ProgressBar rendering is selected,
     *  keeping the panel uncluttered for operators who do not need scale features.
     *
     *  <p>Properties used by <em>both</em> renderers — {@code minimum}, {@code maximum},
     *  {@code limits_from_pv}, {@code horizontal}, {@code log_scale},
     *  {@code fill_color}, {@code background_color} — are intentionally absent. */
    public static final Set<String> SCALE_MODE_PROPS = Set.of(
        "format", "precision",
        "scale_visible", "show_minor_ticks", "opposite_scale_visible",
        "perpendicular_tick_labels", "border_width", "font",
        "border_alarm_sensitive",
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
                // BOY reserved room on top for limit markers and on the bottom for
                // a scale. This widget now actually has a scale, so only adjust for
                // the marker area that has been removed.
                if (XMLUtil.getChildBoolean(xml, "show_markers").orElse(true))
                {
                    final int reduce = 25;
                    bar.propY().setValue(bar.propY().getValue() + reduce);
                    bar.propHeight().setValue(bar.propHeight().getValue() - reduce);
                }

                final Element el = XMLUtil.getChildElement(xml, "color_fillbackground");
                if (el != null)
                    bar.propBackgroundColor().readFromXML(model_reader, el);

                // BOY's 'show_scale' boolean maps to our 'scale_visible' property.
                final Element showScaleEl = XMLUtil.getChildElement(xml, "show_scale");
                if (showScaleEl != null)
                    bar.propScaleVisible().readFromXML(model_reader, showScaleEl);

                // BOY's 'scale_font' maps to our 'font' property.
                final Element scaleFontEl = XMLUtil.getChildElement(xml, "scale_font");
                if (scaleFontEl != null)
                    bar.propFont().readFromXML(model_reader, scaleFontEl);

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
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new ProgressBarConfigurator(persisted_version);
    }

    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<WidgetColor> fill_color;
    private volatile WidgetProperty<WidgetColor> background_color;
    private volatile WidgetProperty<Boolean>     log_scale;
    private volatile WidgetProperty<Boolean>     horizontal;
    private volatile WidgetProperty<Boolean>     scale_visible;
    private volatile WidgetProperty<Boolean>     show_minor_ticks;
    private volatile WidgetProperty<Boolean>     opposite_scale_visible;
    private volatile WidgetProperty<Boolean>     perpendicular_tick_labels;
    private volatile WidgetProperty<Integer>     border_width_prop;

    /** Constructor */
    public ProgressBarWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 300, 30);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(font            = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(fill_color      = propFillColor.createProperty(this, new WidgetColor(60, 255, 60)));
        properties.add(background_color = propBackgroundColor.createProperty(this, new WidgetColor(250, 250, 250)));
        properties.add(log_scale       = propLogscale.createProperty(this, false));
        properties.add(horizontal      = propHorizontal.createProperty(this, true));
        properties.add(scale_visible          = propScaleVisible.createProperty(this, false));
        properties.add(show_minor_ticks        = propShowMinorTicks.createProperty(this, true));
        properties.add(opposite_scale_visible  = propOppositeScaleVisible.createProperty(this, false));
        properties.add(perpendicular_tick_labels = propPerpendicularTickLabels.createProperty(this, false));
        properties.add(border_width_prop         = propBorderWidth.createProperty(this, 0));
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'fill_color' property */
    public WidgetProperty<WidgetColor> propFillColor()
    {
        return fill_color;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background_color;
    }

    /** @return 'log_scale' property */
    public WidgetProperty<Boolean> propLogScale()
    {
        return log_scale;
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }

    /** @return 'scale_visible' property */
    public WidgetProperty<Boolean> propScaleVisible()
    {
        return scale_visible;
    }

    /** @return 'show_minor_ticks' property */
    public WidgetProperty<Boolean> propShowMinorTicks()
    {
        return show_minor_ticks;
    }

    /** @return 'opposite_scale_visible' property */
    public WidgetProperty<Boolean> propOppositeScaleVisible()
    {
        return opposite_scale_visible;
    }

    /** @return 'perpendicular_tick_labels' property */
    public WidgetProperty<Boolean> propPerpendicularTickLabels()
    {
        return perpendicular_tick_labels;
    }

    /** @return 'border_width' property (0 = no border) */
    public WidgetProperty<Integer> propBorderWidth()
    {
        return border_width_prop;
    }
}
