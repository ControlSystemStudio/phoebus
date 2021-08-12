/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.RuntimeWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.properties.IntegerWidgetProperty;
import org.phoebus.framework.macros.Macros;

/** Widget that duplicates a 'template' multiple times
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TemplateInstanceWidget extends VisibleWidget
{
    public static final int DEFAULT_WIDTH = 400,
                            DEFAULT_HEIGHT = 300;

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("template", WidgetCategory.STRUCTURE,
            "Template/Instance",
            "/icons/embedded.png",
            "Widget that embeds a template with multiple instances")
    {
        @Override
        public Widget createWidget()
        {
            return new TemplateInstanceWidget();
        }
    };

    // 'instance' structure that describes each copy of the template
    private static final StructuredWidgetProperty.Descriptor propInstance =
        new StructuredWidgetProperty.Descriptor(WidgetPropertyCategory.BEHAVIOR, "instance", "Instance");

    /** Structure for one instance */
    public static class InstanceProperty extends StructuredWidgetProperty
    {
        public InstanceProperty(final Widget widget, final int index)
        {
            super(propInstance, widget,
                  Arrays.asList(propMacros.createProperty(widget, new Macros())
                               ));
        }
        public WidgetProperty<Macros>       macros()  { return getElement(0); }
    }

    // 'instances' array
    private static final ArrayWidgetProperty.Descriptor<InstanceProperty> propInstances =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.WIDGET, "instances", "Instances",
                (widget, index) -> new InstanceProperty(widget, index),
                /* minimum count */ 1);

    /** 'gap' property: Gap between instances */
    public static final WidgetPropertyDescriptor<Integer> propGap =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "gap", Messages.WidgetProperties_Gap)
        {
            @Override
            public WidgetProperty<Integer> createProperty(final Widget widget, final Integer value)
            {
                return new IntegerWidgetProperty(this, widget, value, 0, 500);
            }
        };

    /** 'wrap_count' property: Optional instance count for wrapping to next row/column */
    public static final WidgetPropertyDescriptor<Integer> propWrapCount =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "wrap_count", Messages.WidgetProperties_WrapCount)
        {
            @Override
            public WidgetProperty<Integer> createProperty(final Widget widget, final Integer value)
            {
                return new IntegerWidgetProperty(this, widget, value, 0, 1000);
            }
        };

    public static final WidgetPropertyDescriptor<DisplayModel> runtimeModel =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.RUNTIME, "embedded_model", "Embedded Model")
        {
            @Override
            public WidgetProperty<DisplayModel> createProperty(final Widget widget, DisplayModel default_value)
            {
                return new RuntimeWidgetProperty<>(runtimeModel, widget, default_value)
                {
                    @Override
                    public void setValueFromObject(final Object value)
                            throws Exception
                    {
                        if (! (value instanceof DisplayModel))
                            throw new IllegalArgumentException("Expected DisplayModel, got " + Objects.toString(value));
                        doSetValue((DisplayModel)value, true);
                    }
                };
            }
        };

    private volatile WidgetProperty<String> file;
    private volatile ArrayWidgetProperty<InstanceProperty> instances;
    private volatile WidgetProperty<Boolean> horizontal;
    private volatile WidgetProperty<Integer> gap;
    private volatile WidgetProperty<Integer> wrap_count;
    private volatile WidgetProperty<DisplayModel> embedded_model;


    public TemplateInstanceWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(file = propFile.createProperty(this, ""));
        properties.add(instances = propInstances.createProperty(this, Arrays.asList(new InstanceProperty(this, 0))));
        properties.add(horizontal = propHorizontal.createProperty(this, false));
        properties.add(gap = propGap.createProperty(this, 10));
        properties.add(wrap_count = propWrapCount.createProperty(this, 0));
        properties.add(embedded_model = runtimeModel.createProperty(this, null));
    }

    /** @return 'file' property */
    public WidgetProperty<String> propFile()
    {
        return file;
    }

    /** @return 'instances' property */
    public ArrayWidgetProperty<InstanceProperty> propInstances()
    {
        return instances;
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }

    /** @return 'gap' property */
    public WidgetProperty<Integer> propGap()
    {
        return gap;
    }

    /** @return 'wrap_count' property */
    public WidgetProperty<Integer> propWrapCount()
    {
        return wrap_count;
    }

    /** @return Runtime 'model' property for the embedded display */
    public WidgetProperty<DisplayModel> runtimePropEmbeddedModel()
    {
        return embedded_model;
    }
}
