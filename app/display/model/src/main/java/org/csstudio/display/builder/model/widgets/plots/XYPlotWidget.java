/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propInteractive;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPVName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropConfigure;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propGridColor;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propToolbar;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propXAxis;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propYAxis;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty.Descriptor;
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
import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.AxisWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.TraceWidgetProperty;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays X/Y waveforms
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYPlotWidget extends VisibleWidget
{
    /** Matcher for detecting legacy property names */
    private static final Pattern LEGACY_AXIS_PATTERN = Pattern.compile("axis_([0-9]+)_([a-z_]+)");
    private static final Pattern LEGACY_TRACE_PATTERN = Pattern.compile("trace_([0-9]+)_([a-z_]+)");

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("xyplot", WidgetCategory.PLOT,
            Messages.XYPlot_Name,
            "/icons/xyplot.png",
            Messages.XYPlot_Description,
            Arrays.asList("org.csstudio.opibuilder.widgets.xyGraph"))
    {
        @Override
        public Widget createWidget()
        {
            return new XYPlotWidget();
        }
    };

    // Elements of Plot Marker
    private static final WidgetPropertyDescriptor<Double> propValue =
            CommonWidgetProperties.newDoublePropertyDescriptor(WidgetPropertyCategory.RUNTIME, "value", Messages.WidgetProperties_Value);

    private static final StructuredWidgetProperty.Descriptor propMarker =
            new Descriptor(WidgetPropertyCategory.DISPLAY, "marker", "Marker");

    /** Structure for Plot Marker */
    public static class MarkerProperty extends StructuredWidgetProperty
    {
        protected MarkerProperty(final Widget widget, final String name)
        {
            super(propMarker, widget,
                  Arrays.asList(propColor.createProperty(widget, new WidgetColor(0, 0, 255)),
                                propPVName.createProperty(widget, ""),
                                propInteractive.createProperty(widget, true),
                                propValue.createProperty(widget, Double.NaN)
                               ));
        }

        public WidgetProperty<WidgetColor> color()     { return getElement(0); }
        public WidgetProperty<String> pv()             { return getElement(1); }
        public WidgetProperty<Boolean> interactive()   { return getElement(2); }
        public WidgetProperty<Double> value()          { return getElement(3); }
    };

    /** 'marker' array */
    private static final ArrayWidgetProperty.Descriptor<MarkerProperty> propMarkers =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.MISC, "marker", "Markers",
                                             (widget, index) -> new MarkerProperty(widget, "Marker " + index),
                                             0);

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
                if (StripchartWidget.isLegacyStripchart(xml))
                    return false;

                // Legacy widget had a "pv_name" property that was basically used as a macro within the widget
                final String pv_macro = XMLUtil.getChildString(xml, "pv_name").orElse("");
                final XYPlotWidget plot = (XYPlotWidget) widget;

                // tooltip defaulted to "$(trace_0_y_pv)\n$(trace_0_y_pv_value)"
                final MacroizedWidgetProperty<String> ttp = (MacroizedWidgetProperty<String>)plot.propTooltip();
                if (ttp.getSpecification().startsWith("$(trace_0_y_pv)"))
                    ttp.setSpecification(plot.getInitialTooltip());

                // "axis_0_*" was the X axis config
                readLegacyAxis(model_reader, 0, xml, plot.x_axis, pv_macro);

                handleLegacyYAxes(model_reader, widget, xml, pv_macro);

                // Turn 'transparent' flag into transparent background color
                if (XMLUtil.getChildBoolean(xml, "transparent").orElse(false))
                    plot.propBackground().setValue(NamedWidgetColors.TRANSPARENT);

                // Foreground color was basically ignored since there was a separate
                // color for each axis  =>  Use X axis color
                final Element e = XMLUtil.getChildElement(xml, "axis_0_axis_color");
                if (e != null)
                    plot.propForeground().readFromXML(model_reader, e);
                else
                    plot.propForeground().setValue(WidgetColorService.getColor(NamedWidgetColors.TEXT));

                if (! handleLegacyTraces(model_reader, widget, xml, pv_macro))
                    return false;

                // If legend was turned off, clear the trace names since they would
                // otherwise show on the Y axes
                if (! plot.propLegend().getValue())
                    for (TraceWidgetProperty trace : plot.propTraces().getValue())
                        trace.traceName().setValue("");
            }
            return true;
        }

        private void readLegacyAxis(final ModelReader model_reader,
                                    final int legacy_axis, final Element xml, final AxisWidgetProperty axis, final String pv_macro) throws Exception
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

            Element font_el = XMLUtil.getChildElement(xml, "axis_" + legacy_axis + "_title_font");
            if (font_el != null)
                axis.titleFont().readFromXML(model_reader, font_el);

            font_el = XMLUtil.getChildElement(xml, "axis_" + legacy_axis + "_scale_font");
            if (font_el != null)
                axis.scaleFont().readFromXML(model_reader, font_el);
        }

        private void handleLegacyYAxes(final ModelReader model_reader,
                                       final Widget widget, final Element xml, final String pv_macro)  throws Exception
        {
            final XYPlotWidget plot = (XYPlotWidget) widget;

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
                if (plot.y_axes.size() < y_count)
                {
                    y_axis = AxisWidgetProperty.create(propYAxis, widget, "");
                    plot.y_axes.addElement(y_axis);
                }
                else
                    y_axis = plot.y_axes.getElement(y_count-1);

                readLegacyAxis(model_reader, legacy_axis, xml, y_axis, pv_macro);
            }
        }

        private boolean handleLegacyTraces(final ModelReader model_reader,
                                           final Widget widget, final Element xml, final String pv_macro) throws Exception
        {
            final XYPlotWidget plot = (XYPlotWidget) widget;

            final int trace_count = XMLUtil.getChildInteger(xml, "trace_count").orElse(0);

            // "trace_0_..." held the trace info
            for (int legacy_trace=0; legacy_trace < trace_count; ++legacy_trace)
            {
                // Y PV
                final String pv_name = XMLUtil.getChildString(xml, "trace_" + legacy_trace + "_y_pv").orElse("");

                // Was legacy widget used with scalar data, concatenated into waveform?
                final Optional<String> concat = XMLUtil.getChildString(xml, "trace_" + legacy_trace + "_concatenate_data");
                if (concat.isPresent()  &&  concat.get().equals("true"))
                {
                	logger.log(Level.WARNING, plot + " does not support 'concatenate_data' for trace " + legacy_trace + ", PV " + pv_name);
                	logger.log(Level.WARNING, "To plot a scalar PV over time, consider the Strip Chart or Data Browser widget");
                }

                final TraceWidgetProperty trace;
                if (plot.traces.size() <= legacy_trace)
                    trace = plot.traces.addElement();
                else
                    trace = plot.traces.getElement(legacy_trace);
                ((StringWidgetProperty)trace.traceYPV()).setSpecification(pv_name.replace("$(pv_name)", pv_macro));

                // X PV
                XMLUtil.getChildString(xml, "trace_" + legacy_trace + "_x_pv")
                       .ifPresent(pv ->
                        {
                            ((StringWidgetProperty)trace.traceXPV()).setSpecification(pv.replace("$(pv_name)", pv_macro));
                        });

                // Color
                Element element = XMLUtil.getChildElement(xml, "trace_" + legacy_trace + "_trace_color");
                if (element != null)
                    trace.traceColor().readFromXML(model_reader, element);

                XMLUtil.getChildInteger(xml, "trace_" + legacy_trace + "_trace_type")
                        .ifPresent(type -> trace.traceType().setValue(StripchartWidget.mapTraceType(type)));

                XMLUtil.getChildInteger(xml, "trace_" + legacy_trace + "_point_size")
                       .ifPresent(size -> trace.tracePointSize().setValue(size));

                XMLUtil.getChildInteger(xml, "trace_" + legacy_trace + "_point_style")
                       .ifPresent(style -> trace.tracePointType().setValue(StripchartWidget.mapPointType(style)));

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
    private volatile WidgetProperty<WidgetColor> grid;
    private volatile WidgetProperty<String> title;
    private volatile WidgetProperty<WidgetFont> title_font;
    private volatile WidgetProperty<Boolean> show_toolbar;
    private volatile WidgetProperty<Boolean> show_legend;
    private volatile AxisWidgetProperty x_axis;
    private volatile ArrayWidgetProperty<AxisWidgetProperty> y_axes;
    private volatile ArrayWidgetProperty<TraceWidgetProperty> traces;
    private volatile ArrayWidgetProperty<MarkerProperty> markers;
    private volatile RuntimeEventProperty configure;

    public XYPlotWidget()
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
        properties.add(grid = propGridColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.GRID)));
        properties.add(title = PlotWidgetProperties.propTitle.createProperty(this, ""));
        properties.add(title_font = PlotWidgetProperties.propTitleFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.HEADER2)));
        properties.add(show_toolbar = propToolbar.createProperty(this,false));
        properties.add(show_legend = PlotWidgetProperties.propLegend.createProperty(this, true));
        properties.add(x_axis = AxisWidgetProperty.create(propXAxis, this, Messages.PlotWidget_X));
        properties.add(y_axes = PlotWidgetProperties.propYAxes.createProperty(this, Arrays.asList(AxisWidgetProperty.create(propYAxis, this, Messages.PlotWidget_Y))));
        properties.add(traces = PlotWidgetProperties.propTraces.createProperty(this, Arrays.asList(new TraceWidgetProperty(this, 0))));
        properties.add(markers = propMarkers.createProperty(this, Collections.emptyList()));
        properties.add(configure = (RuntimeEventProperty) runtimePropConfigure.createProperty(this, null));
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
        // 'axis_1_log_scale', 'axis_1_minimum', '..maximum', '.._axis_title', '.._auto_scale'
        Matcher matcher = LEGACY_AXIS_PATTERN.matcher(name);
        if (matcher.matches())
        {
            final int index = Integer.parseInt(matcher.group(1));
            // Check for index 0 (x_axis.*) or 1.. (y_axes[0].*)
            final String axis = (index == 0)
                ? "x_axis."
                : "y_axes[" + (index-1) + "].";
            final String new_name = axis + matcher.group(2)
                                                  .replace("axis_title", "title")
                                                  .replace("auto_scale", "autoscale");
            if (warnings_once.add(name))
                logger.log(Level.WARNING, "Deprecated access to " + this + " property '" + name + "'. Use '" + new_name + "'");
            return getProperty(new_name);
        }

        // trace_0_y_pv, trace_0_name
        matcher = LEGACY_TRACE_PATTERN.matcher(name);
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

    /** @return 'grid_color' property */
    public WidgetProperty<WidgetColor> propGridColor()
    {
        return grid;
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

    /** @return 'x_axis' property */
    public AxisWidgetProperty propXAxis()
    {
        return x_axis;
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

    /** @return 'markers' property */
    public ArrayWidgetProperty<MarkerProperty> propMarkers()
    {
        return markers;
    }

    /** @return 'configure' property */
    public RuntimeEventProperty runtimePropConfigure()
    {
        return configure;
    }
}
