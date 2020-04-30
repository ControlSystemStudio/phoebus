/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays X/Y waveforms
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StripchartWidget extends VisibleWidget
{
    /** Matcher for detecting legacy property names */
    private static final Pattern LEGACY_TRACE_PATTERN = Pattern.compile("trace_([0-9]+)_([a-z_]+)");
    
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("stripchart", WidgetCategory.PLOT,
            Messages.Stripchart_Name,
            "/icons/xyplot.png",
            Messages.Stripchart_Description,
            Arrays.asList("org.csstudio.opibuilder.widgets.xyGraph"))
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
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "label_font", Messages.Stripchart_LabelFont)
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
                                             (widget, index) -> new TraceWidgetProperty(widget, index));

    /** Legacy XY Graph is handled by several new widgets:
     *  {@link StripchartWidget} if X axis 'time format' set and no 'X' PVs,
     *  else {@link XYPlotWidget}
     *
     *  @param xml Legacy widget XML
     *  @return <code>true</code> if should be handled by {@link StripchartWidget}
     *  @throws Exception on error
     */
    static boolean isLegacyStripchart(final Element xml) throws Exception
    {
        // Stripchart uses time format other than 0 (None)
        if (XMLUtil.getChildInteger(xml, "axis_0_time_format").orElse(-1) == 0)
            return false;

        // Stipchart has no X PVs
        for (int i=0; i<20; ++i)
        {
            final Element el = XMLUtil.getChildElement(xml, "trace_" + i + "_x_pv");
            if (el == null)
                break;
            if (XMLUtil.getString(el).length() > 0)
                return false;
        }

        // Assume that Stipchart is most suitable for legacy XY Graph
        return true;
    }

    /** @param legacy_type Legacy XY Graph trace type
     *  @return {@link PlotWidgetTraceType}
     */
    static PlotWidgetTraceType mapTraceType(final int legacy_type)
    {
        switch (legacy_type)
        {
        case 2: // POINT
            return PlotWidgetTraceType.NONE;
        case 6: // STEP_HORIZONTALLY
            return PlotWidgetTraceType.STEP;
        case 3: // BAR
            return PlotWidgetTraceType.BARS;
        case 0: // SOLID_LINE
        case 1: // DASH_LINE
        case 4: // AREA
        case 5: // STEP_VERTICALLY
        default:
            return PlotWidgetTraceType.LINE;
        }
    }

    /** @param legacy_type Legacy XY Graph trace point type
     *  @return {@link PlotWidgetPointType}
     */
    static PlotWidgetPointType mapPointType(final int legacy_style)
    {
        switch (legacy_style)
        {
        case 0: // None
            return PlotWidgetPointType.NONE;
        case 1: // POINT
        case 2: // CIRCLE
            return PlotWidgetPointType.CIRCLES;
        case 3: // TRIANGLE
        case 4: // FILLED_TRIANGLE
            return PlotWidgetPointType.TRIANGLES;
        case 5: // SQUARE
        case 6: // FILLED_SQUARE
            return PlotWidgetPointType.SQUARES;
        case 7: // DIAMOND
        case 8: // FILLED_DIAMOND
            return PlotWidgetPointType.DIAMONDS;
        case 9: // XCROSS
        case 10: // CROSS
        case 11: // BAR
        default:
            return PlotWidgetPointType.XMARKS;
        }
    }

    /** Legacy properties that have already triggered a warning */
    private final CopyOnWriteArraySet<String> warnings_once = new CopyOnWriteArraySet<>();
    
    /** Configurator that handles legacy properties */
    private static class Configurator extends WidgetConfigurator
    {
        public Configurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element xml)
                throws Exception
        {
            configureAllPropertiesFromMatchingXML(model_reader, widget, xml);

            if (xml_version.getMajor() < 2)
            {
                if (! isLegacyStripchart(xml))
                    return false;

                // Legacy widget had a "pv_name" property that was basically used as a macro within the widget
                final String pv_macro = XMLUtil.getChildString(xml, "pv_name").orElse("");
                final StripchartWidget strip = (StripchartWidget) widget;

                // tooltip defaulted to "$(trace_0_y_pv)\n$(trace_0_y_pv_value)"
                final MacroizedWidgetProperty<String> ttp = (MacroizedWidgetProperty<String>)strip.propTooltip();
                if (ttp.getSpecification().startsWith("$(trace_0_y_pv)"))
                    ttp.setSpecification(strip.getInitialTooltip());

                handleLegacyAxes(model_reader, strip, xml, pv_macro);

                // Turn 'transparent' flag into transparent background color
                if (XMLUtil.getChildBoolean(xml, "transparent").orElse(false))
                    strip.propBackground().setValue(NamedWidgetColors.TRANSPARENT);

                // Foreground color was basically ignored since there was a separate
                // color for each axis  =>  Use X axis color
                final Element e = XMLUtil.getChildElement(xml, "axis_0_axis_color");
                if (e != null)
                    strip.propForeground().readFromXML(model_reader, e);
                else
                    strip.propForeground().setValue(WidgetColorService.getColor(NamedWidgetColors.TEXT));

                if (! handleLegacyTraces(model_reader, strip, xml, pv_macro))
                    return false;
            }
            return true;
        }

        private void handleLegacyAxes(final ModelReader model_reader,
                final StripchartWidget strip, final Element xml, final String pv_macro)  throws Exception
        {
            final int axis_count = XMLUtil.getChildInteger(xml, "axis_count").orElse(0);

            // "axis_1_*" was the Y axis, and higher axes could be either X or Y
            int y_count = 0; // Number of y axes found in legacy config
            for (int legacy_axis=1; legacy_axis<axis_count; ++legacy_axis)
            {
                // Check for "axis_*_y_axis" (default: true).
                // If _not_, this is an additional X axis which we ignore
                if (! Boolean.parseBoolean(XMLUtil.getChildString(xml, "axis_" + legacy_axis + "_y_axis").orElse("true")))
                 continue;

                // Count actual Y axes, because legacy_axis includes skipped X axes
                ++y_count;

                final AxisWidgetProperty y_axis;
                if (strip.y_axes.size() < y_count)
                {
                    y_axis = AxisWidgetProperty.create(propYAxis, strip, "");
                    strip.y_axes.addElement(y_axis);
                }
                else
                    y_axis = strip.y_axes.getElement(y_count-1);

                readLegacyAxis(model_reader, legacy_axis, xml, y_axis, pv_macro);
            }
        }

        private void readLegacyAxis(final ModelReader model_reader, final int legacy_axis,
                                    final Element xml, final AxisWidgetProperty axis, final String pv_macro)
        {
            XMLUtil.getChildString(xml, "axis_" + legacy_axis + "_axis_title").ifPresent(title ->
            {
                final WidgetProperty<String> property = axis.title();
                ((StringWidgetProperty)property).setSpecification(title.replace("$(pv_name)", pv_macro));
            });
            XMLUtil.getChildString(xml, "axis_" + legacy_axis + "_minimum").ifPresent(txt ->
                axis.minimum().setValue(Double.parseDouble(txt)) );
            XMLUtil.getChildString(xml, "axis_" + legacy_axis + "_maximum").ifPresent(txt ->
                axis.maximum().setValue(Double.parseDouble(txt)) );
            XMLUtil.getChildString(xml, "axis_" + legacy_axis + "_auto_scale").ifPresent(txt ->
                axis.autoscale().setValue(Boolean.parseBoolean(txt)) );
            XMLUtil.getChildString(xml, "axis_" + legacy_axis + "_show_grid").ifPresent(txt ->
                axis.grid().setValue(Boolean.parseBoolean(txt)) );
        }

        private boolean handleLegacyTraces(final ModelReader model_reader, final StripchartWidget strip,
                                           final Element xml, final String pv_macro) throws Exception
        {
            final int trace_count = XMLUtil.getChildInteger(xml, "trace_count").orElse(0);

            // "trace_0_..." held the trace info
            for (int legacy_trace=0; legacy_trace < trace_count; ++legacy_trace)
            {
                // Y PV
                final String pv_name = XMLUtil.getChildString(xml, "trace_" + legacy_trace + "_y_pv").orElse("");

                final TraceWidgetProperty trace;
                if (strip.traces.size() <= legacy_trace)
                    trace = strip.traces.addElement();
                else
                    trace = strip.traces.getElement(legacy_trace);
                ((StringWidgetProperty)trace.traceYPV()).setSpecification(pv_name.replace("$(pv_name)", pv_macro));

                // Color
                Element element = XMLUtil.getChildElement(xml, "trace_" + legacy_trace + "_trace_color");
                if (element != null)
                    trace.traceColor().readFromXML(model_reader, element);

                XMLUtil.getChildInteger(xml, "trace_" + legacy_trace + "_trace_type")
                       .ifPresent(type -> trace.traceType().setValue(mapTraceType(type)));

                XMLUtil.getChildInteger(xml, "trace_" + legacy_trace + "_point_size")
                       .ifPresent(size -> trace.tracePointSize().setValue(size));

                XMLUtil.getChildInteger(xml, "trace_" + legacy_trace + "_point_style")
                       .ifPresent(style -> trace.tracePointType().setValue(mapPointType(style)));

		XMLUtil.getChildString(xml, "trace_" + legacy_trace + "_visible")
		       .ifPresent(show -> trace.traceVisible().setValue(Boolean.parseBoolean(show)) );

                // Name
                String name = XMLUtil.getChildString(xml, "trace_" + legacy_trace + "_name").orElse("");
                name = name.replace("$(trace_" + legacy_trace + "_y_pv)", "$(traces[" + legacy_trace + "].y_pv)");
                if (! name.isEmpty())
                    ((StringWidgetProperty)trace.traceName()).setSpecification(name.replace("$(pv_name)", pv_macro));

                // Legacy used index 0=X, 1=Y, 2=Y1, ..
                // except higher axis index could also stand for X1, X2, which we don't handle
                XMLUtil.getChildInteger(xml, "trace_" + legacy_trace + "_y_axis_index")
                       .ifPresent(index -> trace.traceYAxis().setValue(Math.max(0, index - 1)));
            }
            return true;

        }
    };


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
    public WidgetConfigurator getConfigurator(final Version persisted_version) throws Exception
    {
        return new Configurator(persisted_version);
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
        properties.add(show_legend = PlotWidgetProperties.propLegend.createProperty(this, false));
        properties.add(time_range = propTimeRange.createProperty(this, "1 minute"));
        properties.add(y_axes = propYAxes.createProperty(this, Arrays.asList(AxisWidgetProperty.create(propYAxis, this, Messages.PlotWidget_Y))));
        properties.add(traces = propTraces.createProperty(this, Arrays.asList(new TraceWidgetProperty(this, 0))));
        properties.add(configure = (RuntimeEventProperty) runtimePropConfigure.createProperty(this, null));
        properties.add(open_full = (RuntimeEventProperty) DataBrowserWidget.runtimePropOpenFull.createProperty(this, null));
    }

    @Override
    protected String getInitialTooltip()
    {
        return "$(traces[0].y_pv)";
    }

    @Override
    public WidgetProperty<?> getProperty(final String name)
    {
        // Translate legacy property names:
        // trace_0_y_pv, trace_0_name
        Matcher matcher = LEGACY_TRACE_PATTERN.matcher(name);
        if (matcher.matches())
        {
            final int index = Integer.parseInt(matcher.group(1));
            // Check for index 0 (x_axis.*) or 1.. (y_axes[0].*)
            final String trace = "traces[" + index + "].";
            final String new_name = trace + matcher.group(2);
            if (warnings_once.add(name))
                logger.log(Level.WARNING, "Deprecated access to " + this + " property '" + name + "'. Use '" + new_name + "'");
            return getProperty(new_name);
        }
	
        return super.getProperty(name);
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
