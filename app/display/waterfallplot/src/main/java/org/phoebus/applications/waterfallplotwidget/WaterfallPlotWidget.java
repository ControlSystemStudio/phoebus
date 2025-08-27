package org.phoebus.applications.waterfallplotwidget;

import javafx.application.Platform;
import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.PVWidget;

import java.sql.Time;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;

public class WaterfallPlotWidget extends PVWidget {

    public WaterfallPlotWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 240, 120);
    }

    public static WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("waterfallplotwidget", WidgetCategory.PLOT,
                    "Waterfall Plot",
                    "/icons/waterfallplot.png",
                    "Waterfall Plot Widget.",
                    Arrays.asList(""))
            {
                @Override
                public Widget createWidget()
                {
                    return new WaterfallPlotWidget();
                }
            };

    enum TimeAxis {
        XAxis(Messages.XAxis),
        YAxis(Messages.YAxis);

        private final String displayName;

        private TimeAxis(String displayName) {
            this.displayName = displayName;
        }

        ;

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    private WidgetProperty<TimeAxis> time_axis;
    public static WidgetPropertyDescriptor<TimeAxis> propTimeAxis =
            new WidgetPropertyDescriptor<>(
                    WidgetPropertyCategory.DISPLAY, "time_axis", Messages.TimeAxis)
            {
                @Override
                public EnumWidgetProperty<TimeAxis> createProperty(Widget widget, TimeAxis default_value)
                {
                    return new EnumWidgetProperty<>(this, widget, default_value);
                }
            };

    public enum ColorGradientEnum {
        RAINBOW,
        RAINBOW_OPAQUE,
        JET,
        TOPO_EXT,
        WHITE_BLACK,
        BLACK_WHITE,
        HOT,
        SUNRISE,
        VIRIDIS,
        BLUE_RED,
        PINK,
        RAINBOW_EQ
    }

    private WidgetProperty<ColorGradientEnum> color_gradient;
    public static WidgetPropertyDescriptor<ColorGradientEnum> propColorGradient =
            new WidgetPropertyDescriptor<>(
                    WidgetPropertyCategory.DISPLAY, "color_gradient", Messages.ColorGradient)
            {
                @Override
                public EnumWidgetProperty<ColorGradientEnum> createProperty(Widget widget, ColorGradientEnum default_value)
                {
                    return new EnumWidgetProperty<>(this, widget, default_value);
                }
            };

    private WidgetProperty<Boolean> input_is_waveform_pv;
    private static WidgetPropertyDescriptor<Boolean> propDatasourceIsWaveformPV =
            newBooleanPropertyDescriptor(WidgetPropertyCategory.WIDGET, "input_is_waveform_pv", Messages.WaveformPV);

    private WidgetProperty<Boolean> use_pv_number_as_label_on_axis;
    private static WidgetPropertyDescriptor<Boolean> propUsePVNumberAsLabelOnAxis =
            newBooleanPropertyDescriptor(WidgetPropertyCategory.WIDGET, "use_pv_number_as_label_on_axis", Messages.UsePVNumberAsLabelOnAxis);


    private static WidgetPropertyDescriptor<String> propPV(int index) {
        return newStringPropertyDescriptor(WidgetPropertyCategory.WIDGET, "pv", Messages.PV + " " + index);
    }

    private volatile ArrayWidgetProperty<WidgetProperty<String>> input_pvs;
    public static ArrayWidgetProperty.Descriptor<WidgetProperty<String>> propPVs =
            new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.WIDGET,
                                                 Messages.PVs,
                                                 Messages.PVs,
                                                 (widget, index) -> propPV(index).createProperty(widget, ""),
                                                 1
    );

    private WidgetProperty<Boolean> retrieve_historic_values_from_the_archiver;
    private static WidgetPropertyDescriptor<Boolean> propRetrieveHistoricValuesFromTheArchiver =
            newBooleanPropertyDescriptor(WidgetPropertyCategory.WIDGET, "retrieve_historic_values_from_the_archiver", Messages.RetrieveHistoricValuesFromTheArchiver);

    private WidgetProperty<String> z_axis_name;
    private static WidgetPropertyDescriptor<String> propZAxisName =
            newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "z_axis_name", Messages.ZAxisName);

    private WidgetProperty<String> z_axis_unit;
    private static WidgetPropertyDescriptor<String> propZAxisUnit =
            newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "z_axis_unit", Messages.ZAxisUnit);

    enum ZAxisMinMax {
        FromPVLimits(Messages.FromPVLimits),
        SetAutomaticallyBasedOnReceivedValues(Messages.SetAutomaticallyBasedOnReceivedValues),
        SetManually(Messages.SetMinAndMaxManually);

        private final String displayName;

        private ZAxisMinMax(String displayName) {
            this.displayName = displayName;
        }

        ;

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    private WidgetProperty<ZAxisMinMax> z_axis_min_max;
    public static WidgetPropertyDescriptor<ZAxisMinMax> propZAxisMinMax =
            new WidgetPropertyDescriptor<>(
                    WidgetPropertyCategory.DISPLAY, "z_axis_min_max", Messages.ZAxisMinMax)
            {
                @Override
                public EnumWidgetProperty<ZAxisMinMax> createProperty(Widget widget, ZAxisMinMax default_value)
                {
                    return new EnumWidgetProperty<>(this, widget, default_value);
                }
            };

    private WidgetProperty<Double> z_axis_min;
    private static WidgetPropertyDescriptor<Double> propZAxisMin =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "z_axis_min", Messages.ZAxisMin);

    private WidgetProperty<Double> z_axis_max;
    private static WidgetPropertyDescriptor<Double> propZAxisMax =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "z_axis_max", Messages.ZAxisMax);

    private WidgetProperty<String> timespan;
    private static WidgetPropertyDescriptor<String> propTimespan =
            newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "timespan", Messages.Timespan);

    private WidgetProperty<String> pv_axis_name;
    private static WidgetPropertyDescriptor<String> propPVAxisName =
            newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "pv_axis_name", Messages.PVAxisName);

    private WidgetProperty<String> pv_axis_unit;
    private static WidgetPropertyDescriptor<String> propPVAxisUnit =
            newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "pv_axis_unit", Messages.PVAxisUnit);

    private WidgetProperty<WidgetFont> title_font;
    public static WidgetPropertyDescriptor<WidgetFont> propTitleFont =
            new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "title_font", Messages.TitleFont) {
                @Override
                public WidgetProperty<WidgetFont> createProperty(final Widget widget,
                                                                 final WidgetFont font) {
                    return new FontWidgetProperty(this, widget, font);
                }
            };

    private WidgetProperty<WidgetFont> axis_label_font;
    public static WidgetPropertyDescriptor<WidgetFont> propAxisLabelFont =
            new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "axis_label_font", Messages.AxisLabelFont) {
                @Override
                public WidgetProperty<WidgetFont> createProperty(final Widget widget,
                                                                 final WidgetFont font) {
                    return new FontWidgetProperty(this, widget, font);
                }
            };

    private WidgetProperty<WidgetFont> tick_label_font;
    public static WidgetPropertyDescriptor<WidgetFont> propTickLabelFont =
            new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "tick_label_font", Messages.TickLabelFont) {
                @Override
                public WidgetProperty<WidgetFont> createProperty(final Widget widget,
                                                                 final WidgetFont font) {
                    return new FontWidgetProperty(this, widget, font);
                }
            };

    private WidgetProperty<Double> major_tick_length;
    private static WidgetPropertyDescriptor<Double> propMajorTickLength =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "major_tick_length", Messages.MajorTickLength);

    private WidgetProperty<Double> major_tick_width;
    private static WidgetPropertyDescriptor<Double> propMajorTickWidth =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "major_tick_width", Messages.MajorTickWidth);

    private WidgetProperty<Double> minor_tick_length;
    private static WidgetPropertyDescriptor<Double> propMinorTickLength =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "minor_tick_length", Messages.MinorTickLength);

    private WidgetProperty<Double> minor_tick_width;
    private static WidgetPropertyDescriptor<Double> propMinorTickWidth =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "minor_tick_width", Messages.MinorTickWidth);

    @Override
    protected void defineProperties(List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);

        properties.add(title_font = propTitleFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.HEADER1)));
        properties.add(axis_label_font = propAxisLabelFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(tick_label_font = propTickLabelFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(color_gradient = propColorGradient.createProperty(this, ColorGradientEnum.RAINBOW));
        properties.remove(super.propPVName());
        properties.add(input_is_waveform_pv = propDatasourceIsWaveformPV.createProperty(this, true));
        properties.add(input_pvs = propPVs.createProperty(this, Collections.singletonList(propPV(0).createProperty(this, ""))));
        properties.add(use_pv_number_as_label_on_axis = propUsePVNumberAsLabelOnAxis.createProperty(this, false));
        properties.add(retrieve_historic_values_from_the_archiver = propRetrieveHistoricValuesFromTheArchiver.createProperty(this, true));
        properties.add(z_axis_name = propZAxisName.createProperty(this, ""));
        properties.add(z_axis_unit = propZAxisUnit.createProperty(this, ""));
        properties.add(z_axis_min_max = propZAxisMinMax.createProperty(this, ZAxisMinMax.FromPVLimits));
        properties.add(z_axis_min = propZAxisMin.createProperty(this, 0.0));
        properties.add(z_axis_max = propZAxisMax.createProperty(this, 100.0));
        properties.add(pv_axis_name = propPVAxisName.createProperty(this, ""));
        properties.add(pv_axis_unit = propPVAxisUnit.createProperty(this, ""));
        properties.add(major_tick_length = propMajorTickLength.createProperty(this, 10.0));
        properties.add(major_tick_width = propMajorTickWidth.createProperty(this, 2.0));
        properties.add(minor_tick_length = propMinorTickLength.createProperty(this, 5.0));
        properties.add(minor_tick_width = propMinorTickWidth.createProperty(this, 1.0));
        properties.add(time_axis = propTimeAxis.createProperty(this, TimeAxis.YAxis));
        properties.add(timespan = propTimespan.createProperty(this, "10 minutes"));

        // WidgetPropertyListeners to enable only one PV as input when "Input is a waveform" is enabled:
        WidgetPropertyListener<List<WidgetProperty<String>>> scalarPvsListener = (property2, oldValue2, newValue2) -> {
            if (input_is_waveform_pv.getValue()) {
                // Use Platform.runLater() to schedule the removal at a later time.
                // Otherwise, the call to removeElement() results in the handler
                // being called again, in an endless loop.
                Platform.runLater(() -> {
                    int scalar_pvs_size = input_pvs.size();
                    if (scalar_pvs_size > 1) {
                        input_pvs.setValue(input_pvs.getValue().subList(0, 1));
                    }
                });
            }
        };
        input_pvs.addPropertyListener(scalarPvsListener);

        LinkedList<WidgetProperty<String>> listOfPVs = new LinkedList<>(); // Note: access to this list must be synchronized using synchronized(listOfPVs) { ... }
        input_is_waveform_pv.addPropertyListener((property, oldValue, newValue) -> {
            synchronized (listOfPVs) {
                if (newValue) {
                    // When going from datasource_is_waveform_pv == true to datasource_is_waveform_pv == false,
                    // save the list of PVs in case the user changes their mind again:
                    listOfPVs.clear();
                    input_pvs.getValue().forEach(element -> listOfPVs.add(element));
                }
                else {
                    listOfPVs.forEach(pvNameWidgetProperty -> input_pvs.setValue(listOfPVs));
                    listOfPVs.clear();
                }
            }

            scalarPvsListener.propertyChanged(null, null, null);
        });
    }

    /** @return 'time_axis' property */
    public WidgetProperty<TimeAxis> propTimeAxis()
    {
        return time_axis;
    }

    /** @return 'color_gradient' property */
    public WidgetProperty<ColorGradientEnum> propColorGradient()
    {
        return color_gradient;
    }

    /** @return 'input_is_waveform_pv' property */
    public WidgetProperty<Boolean> propInputIsWaveformPV() {
        return input_is_waveform_pv;
    }

    /** @return 'use_pv_number_as_label_on_axis' property */
    public WidgetProperty<Boolean> propUsePVNumberAsLabelOnAxis() {
        return use_pv_number_as_label_on_axis;
    }

    /** @return 'input_pvs' property */
    public ArrayWidgetProperty<WidgetProperty<String>> propInputPVs() {
        return input_pvs;
    }

    /** @return 'z_axis_name' property */
    public WidgetProperty<String> propZAxisName() {
        return z_axis_name;
    }

    /** @return 'z_axis_unit' property */
    public WidgetProperty<String> propZAxisUnit() {
        return z_axis_unit;
    }

    /** @return 'z_axis_min_max' property */
    public WidgetProperty<ZAxisMinMax> propZAxisMinMax() {
        return z_axis_min_max;
    }

    /** @return 'z_axis_min' property */
    public WidgetProperty<Double> propZAxisMin() {
        return z_axis_min;
    }

    /** @return 'z_axis_max' property */
    public WidgetProperty<Double> propZAxisMax() {
        return z_axis_max;
    }

    /** @return 'pv_axis_name' property */
    public WidgetProperty<String> propPVAxisName() {
        return pv_axis_name;
    }

    /** @return 'pv_axis_unit' property */
    public WidgetProperty<String> propPVAxisUnit() {
        return pv_axis_unit;
    }

    /** @return 'timespan' property */
    public WidgetProperty<String> propTimespan() {
        return timespan;
    }

    /** @return 'title_font' property */
    public WidgetProperty<WidgetFont> propTitleFont() {
        return title_font;
    }

    /** @return 'axis_label_font' property */
    public WidgetProperty<WidgetFont> propAxisLabelFont() {
        return axis_label_font;
    }

    /** @return 'tick_label_font' property */
    public WidgetProperty<WidgetFont> propTickLabelFont() {
        return tick_label_font;
    }

    /** @return 'major_tick_length' property */
    public WidgetProperty<Double> propMajorTickLength() {
        return major_tick_length;
    }

    /** @return 'major_tick_width' property */
    public WidgetProperty<Double> propMajorTickWidth() {
        return major_tick_width;
    }

    /** @return 'minor_tick_length' property */
    public WidgetProperty<Double> propMinorTickLength() {
        return minor_tick_length;
    }

    /** @return 'minor_tick_width' property */
    public WidgetProperty<Double> propMinorTickWidth() {
        return minor_tick_width;
    }

    /** @return 'retrieve_historic_values_from_the_archiver' property */
    public WidgetProperty<Boolean> propRetrieveHistoricValuesFromTheArchiver() {
        return retrieve_historic_values_from_the_archiver;
    }
}
