/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropConfigure;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propGrid;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propToolbar;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propTrace;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propYAxis;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.traceColor;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.tracePointSize;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.tracePointType;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.traceType;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.traceY;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.traceYAxis;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.VisibleWidget;

/** Widget that displays X/Y waveforms
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StripchartWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("stripchart", WidgetCategory.PLOT,
            Messages.Stripchart_Name,
            "/icons/xyplot.png",
            Messages.Stripchart_Description)
    {
        @Override
        public Widget createWidget()
        {
            return new StripchartWidget();
        }
    };

    public static final WidgetPropertyDescriptor<String> propTimeRange =
            CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "time_range", Messages.Stripchart_TimeRange);

    public static final WidgetPropertyDescriptor<WidgetFont> propLabelFont =
            new WidgetPropertyDescriptor<>(
                    WidgetPropertyCategory.DISPLAY, "label_font", Messages.Stripchart_LabelFont)
    {
        @Override
        public WidgetProperty<WidgetFont> createProperty(final Widget widget,
                final WidgetFont font)
        {
            return new FontWidgetProperty(this, widget, font);
        }
    };

    // 'axis' structure
    public static class AxisWidgetProperty extends StructuredWidgetProperty
    {
        public static AxisWidgetProperty create(final StructuredWidgetProperty.Descriptor descriptor, final Widget widget, final String title_text)
        {
            return new AxisWidgetProperty(descriptor, widget,
                  Arrays.asList(PlotWidgetProperties.propTitle.createProperty(widget, title_text),
                                PlotWidgetProperties.propAutoscale.createProperty(widget, false),
                                PlotWidgetProperties.propLogscale.createProperty(widget, false),
                                CommonWidgetProperties.propMinimum.createProperty(widget, 0.0),
                                CommonWidgetProperties.propMaximum.createProperty(widget, 100.0),
                                PlotWidgetProperties.propGrid.createProperty(widget, false),
                                CommonWidgetProperties.propVisible.createProperty(widget, true)));
        }

        protected AxisWidgetProperty(final StructuredWidgetProperty.Descriptor axis_descriptor,
                                     final Widget widget, final List<WidgetProperty<?>> elements)
        {
            super(axis_descriptor, widget, elements);
        }

        public WidgetProperty<String> title()           { return getElement(0); }
        public WidgetProperty<Boolean> autoscale()      { return getElement(1); }
        public WidgetProperty<Boolean> logscale()       { return getElement(2); }
        public WidgetProperty<Double> minimum()         { return getElement(3); }
        public WidgetProperty<Double> maximum()         { return getElement(4); }
        public WidgetProperty<Boolean> grid()           { return getElement(5); }
        public WidgetProperty<Boolean> visible()        { return getElement(6); }
    };

    // 'y_axes' array
    public static final ArrayWidgetProperty.Descriptor<AxisWidgetProperty> propYAxes =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.BEHAVIOR, "y_axes", Messages.PlotWidget_YAxes,
                                             (widget, index) ->
                                             AxisWidgetProperty.create(propYAxis, widget,
                                                                       index > 0
                                                                       ? Messages.PlotWidget_Y + " " + index
                                                                       : Messages.PlotWidget_Y));


    /** 'trace' structure */
    public static class TraceWidgetProperty extends StructuredWidgetProperty
    {
        public TraceWidgetProperty(final Widget widget, final int index)
        {
            super(propTrace, widget,
                  Arrays.asList(CommonWidgetProperties.propName.createProperty(widget, "$(traces[" + index + "].y_pv)"),
                                traceY.createProperty(widget, ""),
                                traceYAxis.createProperty(widget, 0),
                                traceType.createProperty(widget, PlotWidgetTraceType.STEP),
                                traceColor.createProperty(widget, NamedWidgetColors.getPaletteColor(index)),
                                CommonWidgetProperties.propLineWidth.createProperty(widget, 2),
                                tracePointType.createProperty(widget, PlotWidgetPointType.NONE),
                                tracePointSize.createProperty(widget, 10),
                                CommonWidgetProperties.propVisible.createProperty(widget, true)));
        }
        public WidgetProperty<String> traceName()                   { return getElement(0); }
        public WidgetProperty<String> traceYPV()                    { return getElement(1); }
        public WidgetProperty<Integer> traceYAxis()                 { return getElement(2); }
        public WidgetProperty<PlotWidgetTraceType> traceType()      { return getElement(3); }
        public WidgetProperty<WidgetColor> traceColor()             { return getElement(4); }
        public WidgetProperty<Integer> traceWidth()                 { return getElement(5); }
        public WidgetProperty<PlotWidgetPointType> tracePointType() { return getElement(6); }
        public WidgetProperty<Integer> tracePointSize()             { return getElement(7); }
        public WidgetProperty<Boolean> traceVisible()               { return getElement(8); }
    };

    /** 'traces' array */
    public static final ArrayWidgetProperty.Descriptor<TraceWidgetProperty> propTraces =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.BEHAVIOR, "traces", Messages.PlotWidget_Traces,
                                             (widget, index) ->
                                             new TraceWidgetProperty(widget, index));


    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<Boolean> show_grid;
    private volatile WidgetProperty<String> title;
    private volatile WidgetProperty<WidgetFont> title_font;
    private volatile WidgetProperty<WidgetFont> label_font;
    private volatile WidgetProperty<WidgetFont> scale_font;
    private volatile WidgetProperty<Boolean> show_toolbar;
    private volatile WidgetProperty<Boolean> show_legend;
    private volatile WidgetProperty<String> time_range;
    private volatile ArrayWidgetProperty<AxisWidgetProperty> y_axes;
    private volatile ArrayWidgetProperty<TraceWidgetProperty> traces;
    private volatile RuntimeEventProperty configure;
    private volatile RuntimeEventProperty open_full;


    public StripchartWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 400, 300);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(show_grid = propGrid.createProperty(this, false));
        properties.add(title = PlotWidgetProperties.propTitle.createProperty(this, ""));
        properties.add(title_font = PlotWidgetProperties.propTitleFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.HEADER2)));
        properties.add(label_font = propLabelFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT_BOLD)));
        properties.add(scale_font = PlotWidgetProperties.propScaleFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(show_toolbar = propToolbar.createProperty(this, true));
        properties.add(show_legend = PlotWidgetProperties.propLegend.createProperty(this, true));
        properties.add(time_range = propTimeRange.createProperty(this, "1 minute"));
        properties.add(y_axes = propYAxes.createProperty(this, Arrays.asList(AxisWidgetProperty.create(propYAxis, this, Messages.PlotWidget_Y))));
        properties.add(traces = propTraces.createProperty(this, Arrays.asList(new TraceWidgetProperty(this, 0))));
        properties.add(configure = (RuntimeEventProperty) runtimePropConfigure.createProperty(this, null));
        properties.add(open_full = (RuntimeEventProperty) DataBrowserWidget.runtimePropOpenFull.createProperty(this, null));
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackground()
    {
        return background;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForeground()
    {
        return foreground;
    }

    /** @return 'show_grid' property */
    public WidgetProperty<Boolean> propGrid()
    {
        return show_grid;
    }

    /** @return 'title' property */
    public WidgetProperty<String> propTitle()
    {
        return title;
    }

    /** @return 'title_font' property */
    public WidgetProperty<WidgetFont> propTitleFont()
    {
        return title_font;
    }

    /** @return 'label_font' property */
    public WidgetProperty<WidgetFont> propLabelFont()
    {
        return label_font;
    }

    /** @return 'scale_font' property */
    public WidgetProperty<WidgetFont> propScaleFont()
    {
        return scale_font;
    }

    /** @return 'show_toolbar' property */
    public WidgetProperty<Boolean> propToolbar()
    {
        return show_toolbar;
    }

    /** @return 'show_legend' property */
    public WidgetProperty<Boolean> propLegend()
    {
        return show_legend;
    }

    /** @return 'time_range' property */
    public WidgetProperty<String> propTimeRange()
    {
        return time_range;
    }

    /** @return 'y_axes' property */
    public ArrayWidgetProperty<AxisWidgetProperty> propYAxes()
    {
        return y_axes;
    }

    /** @return 'traces' property */
    public ArrayWidgetProperty<TraceWidgetProperty> propTraces()
    {
        return traces;
    }

    /** @return 'configure' property */
    public RuntimeEventProperty runtimePropConfigure()
    {
        return configure;
    }

    /** @return 'open_full' property */
    public RuntimeEventProperty runtimePropOpenDataBrowser()
    {
        return open_full;
    }
}
