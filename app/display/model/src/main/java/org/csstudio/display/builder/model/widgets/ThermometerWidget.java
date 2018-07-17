/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFillColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;

/**
 * Widget of a thermometer
 *
 * @author Amanda Carpenter
 */
public class ThermometerWidget extends PVWidget
{
    /** Widget descriptor */
    @SuppressWarnings("nls")
    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor("thermometer",
            WidgetCategory.MONITOR,
            "Thermometer",
            "/icons/Thermo.png",
            "A thermometer",
            Arrays.asList("org.csstudio.opibuilder.widgets.thermometer"))
    {
        @Override
        public Widget createWidget()
        {
            return new ThermometerWidget();
        }
    };

    //TODO: configurator that ignores if show_bulb property is false (vertical progress bar instead)

    public ThermometerWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 40, 160);
    }

    private volatile WidgetProperty<Boolean> limits_from_pv;
    private volatile WidgetProperty<Double> minimum;
    private volatile WidgetProperty<Double> maximum;
    private volatile WidgetProperty<WidgetColor> fill_color;

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(fill_color = propFillColor.createProperty(this, new WidgetColor(60, 255, 60)));
        properties.add(limits_from_pv = propLimitsFromPV.createProperty(this, true));
        properties.add(minimum = propMinimum.createProperty(this, 0.0));
        properties.add(maximum = propMaximum.createProperty(this, 100.0));
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

}
