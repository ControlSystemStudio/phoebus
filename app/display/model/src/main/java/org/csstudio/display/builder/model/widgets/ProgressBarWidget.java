/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFillColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;

import java.util.Arrays;
import java.util.List;

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
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Widget that displays a progress bar
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ProgressBarWidget extends PVWidget
{
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

    /** Widget configurator to read legacy *.opi files*/
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
                // Do use space below where BOY placed markers for the bar itself.
                // In the future, there could be a scale.

                final Element el = XMLUtil.getChildElement(xml, "color_fillbackground");
                if (el != null)
                    bar.propBackgroundColor().readFromXML(model_reader, el);

                // Create text update for the value indicator
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

    private volatile WidgetProperty<Boolean> limits_from_pv;
    private volatile WidgetProperty<Double> minimum;
    private volatile WidgetProperty<Double> maximum;
    private volatile WidgetProperty<WidgetColor> fill_color;
    private volatile WidgetProperty<WidgetColor> background_color;
    private volatile WidgetProperty<Boolean> horizontal;

    public ProgressBarWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(fill_color = propFillColor.createProperty(this, new WidgetColor(60, 255, 60)));
        properties.add(background_color = propBackgroundColor.createProperty(this, new WidgetColor(250, 250, 250)));
        properties.add(limits_from_pv = propLimitsFromPV.createProperty(this, true));
        properties.add(minimum = propMinimum.createProperty(this, 0.0));
        properties.add(maximum = propMaximum.createProperty(this, 100.0));
        properties.add(horizontal = propHorizontal.createProperty(this, true));
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

    /** @return 'limits_from_pv' property */
    public WidgetProperty<Boolean> propLimitsFromPV()
    {
        return limits_from_pv;
    }

    /** @return 'minimum' property */
    public WidgetProperty<Double> propMinimum()
    {
        return minimum;
    }

    /** @return 'maximum' property */
    public WidgetProperty<Double> propMaximum()
    {
        return maximum;
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }
}
