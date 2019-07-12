/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propIncrement;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropConfigure;

import java.util.Arrays;
import java.util.List;

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
import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that can read/write numeric PV via scrollbar
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ScrollBarWidget extends WritablePVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("scrollbar", WidgetCategory.CONTROL,
            "Scrollbar",
            "/icons/scrollbar.png",
            "A scrollbar that can read/write a numeric PV",
            Arrays.asList("org.csstudio.opibuilder.widgets.scrollbar"))
    {
        @Override
        public Widget createWidget()
        {
            return new ScrollBarWidget();
        }
    };

    /** 'show_value_tip' property: Show value tip */
    public static final WidgetPropertyDescriptor<Boolean> propShowValueTip =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_value_tip", Messages.ScrollBar_ShowValueTip);

    /** 'bar_length' property: Bar length: length visible */
    public static final WidgetPropertyDescriptor<Double> propBarLength =
        newDoublePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "bar_length", Messages.ScrollBar_BarLength);

    /** Configurator that handles legacy "step_increment" as "increment" */
    public static class IncrementConfigurator extends WidgetConfigurator
    {
        public IncrementConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;
            handleLegacyIncrement(widget, xml);
            return true;
        }

        public static void handleLegacyIncrement(final Widget widget, final Element xml) throws Exception
        {
            XMLUtil.getChildDouble(xml, "step_increment")
                   .ifPresent(value -> widget.getProperty(propIncrement).setValue(value));
        }
    };

    private volatile WidgetProperty<Double> minimum;
    private volatile WidgetProperty<Double> maximum;
    private volatile WidgetProperty<Boolean> limits_from_pv;
    private volatile WidgetProperty<Boolean> horizontal;
    private volatile WidgetProperty<Boolean> show_value_tip;
    private volatile WidgetProperty<Double> bar_length;
    private volatile WidgetProperty<Double> increment;
    private volatile WidgetProperty<Boolean> enabled;
    private volatile RuntimeEventProperty configure;

    public ScrollBarWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(minimum = propMinimum.createProperty(this, 0.0));
        properties.add(maximum = propMaximum.createProperty(this, 100.0));
        properties.add(limits_from_pv = propLimitsFromPV.createProperty(this, true));
        properties.add(horizontal = propHorizontal.createProperty(this, true));
        properties.add(show_value_tip = propShowValueTip.createProperty(this, true));
        properties.add(bar_length = propBarLength.createProperty(this, 10.0));
        properties.add(increment = propIncrement.createProperty(this, 1.0));
        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(configure = (RuntimeEventProperty) runtimePropConfigure.createProperty(this, null));
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version) throws Exception
    {
        return new IncrementConfigurator(persisted_version);
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

    /** @return 'limits_from_pv' property*/
    public WidgetProperty<Boolean> propLimitsFromPV()
    {
        return limits_from_pv;
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }

    /** @return 'show_value_tip' property */
    public WidgetProperty<Boolean> propShowValueTip()
    {
        return show_value_tip;
    }

    /** @return 'bar_length' property */
    public WidgetProperty<Double> propBarLength()
    {
        return bar_length;
    }

    /** @return 'increment' property */
    public WidgetProperty<Double> propIncrement()
    {
        return increment;
    }

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }

    /** @return 'configure' property */
    public RuntimeEventProperty runtimePropConfigure()
    {
        return configure;
    }
}