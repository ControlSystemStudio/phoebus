/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

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
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

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

    //TODO: BOY thermometer where show bulb property false
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
            //Legacy tank widget was always vertical; needs horizontal=false
            if (xml_version.getMajor() < 2 && XMLUtil.getChildElement(xml, propHorizontal.getName()) == null)
            {
                final Document doc = xml.getOwnerDocument();
                final Element new_el = doc.createElement(propHorizontal.getName());
                final Text falze = doc.createTextNode("false");
                new_el.appendChild(falze);
                xml.appendChild(new_el);
            }

            super.configureFromXML(model_reader, widget, xml);
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
