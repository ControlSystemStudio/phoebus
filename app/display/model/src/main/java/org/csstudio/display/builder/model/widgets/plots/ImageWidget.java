/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propInteractive;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropConfigure;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propToolbar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.RuntimeWidgetProperty;
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
import org.csstudio.display.builder.model.properties.ColorMap;
import org.csstudio.display.builder.model.properties.ColorMapWidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.IntegerWidgetProperty;
import org.csstudio.display.builder.model.properties.PredefinedColorMaps;
import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.epics.vtype.VImageType;
import org.epics.vtype.VType;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays an image
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageWidget extends PVWidget
{
    /** Matcher for detecting legacy property names */
    private static final Pattern LEGACY_AXIS_PATTERN = Pattern.compile("([xy])_axis_([a-z_]+)");
    
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("image", WidgetCategory.PLOT,
            "Image",
            "/icons/image.png",
            "Display image",
            Arrays.asList("org.csstudio.opibuilder.widgets.intensityGraph"))
    {
        @Override
        public Widget createWidget()
        {
            return new ImageWidget();
        }
    };

    private static final WidgetPropertyDescriptor<InterpolationType> propInterpolationType =
            new WidgetPropertyDescriptor<>(WidgetPropertyCategory.BEHAVIOR, "interpolation", Messages.WidgetProperties_Interpolation)
    {
        @Override
        public WidgetProperty<InterpolationType> createProperty(final Widget widget,
                                                                final InterpolationType default_value)
        {
            return new EnumWidgetProperty<>(this, widget, default_value);
        }
    };

    private static final WidgetPropertyDescriptor<VImageType> propColorMode =
            new WidgetPropertyDescriptor<>(WidgetPropertyCategory.BEHAVIOR, "color_mode", Messages.WidgetProperties_ColorMode)
    {
        @Override
        public WidgetProperty<VImageType> createProperty(final Widget widget,
                                                         final VImageType default_value)
        {
            return new EnumWidgetProperty<>(this, widget, default_value);
        }
    };

    /** Color map: Maps values to colors in the image */
    private static final WidgetPropertyDescriptor<ColorMap> propDataColormap =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "color_map", Messages.WidgetProperties_ColorMap)
    {
        @Override
        public WidgetProperty<ColorMap> createProperty(final Widget widget, final ColorMap map)
        {
            return new ColorMapWidgetProperty(this, widget, map);
        }
    };

    private static final WidgetPropertyDescriptor<Integer> propColorbarSize =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "bar_size", "Color Bar Size");

    private final static StructuredWidgetProperty.Descriptor propColorbar =
        new Descriptor(WidgetPropertyCategory.DISPLAY, "color_bar", "Color Bar");

    /** Structure for color bar, the 'legend' that shows the color bar */
    public static class ColorBarProperty extends StructuredWidgetProperty
    {
        public ColorBarProperty(final Widget widget)
        {
            super(propColorbar, widget,
                  Arrays.asList(CommonWidgetProperties.propVisible.createProperty(widget, true),
                                propColorbarSize.createProperty(widget, 40),
                                PlotWidgetProperties.propScaleFont.createProperty(widget, WidgetFontService.get(NamedWidgetFonts.DEFAULT))));
        }

        public WidgetProperty<Boolean> visible()        { return getElement(0); }
        public WidgetProperty<Integer> barSize()        { return getElement(1); }
        public WidgetProperty<WidgetFont> scaleFont()   { return getElement(2); }
    };

    /** Structure for X and Y axes */
    public static class AxisWidgetProperty extends StructuredWidgetProperty
    {
        protected AxisWidgetProperty(final StructuredWidgetProperty.Descriptor axis_descriptor,
                                     final Widget widget, final String title_text)
        {
            super(axis_descriptor, widget,
                  Arrays.asList(CommonWidgetProperties.propVisible.createProperty(widget, true),
                                PlotWidgetProperties.propTitle.createProperty(widget, title_text),
                                CommonWidgetProperties.propMinimum.createProperty(widget, 0.0),
                                CommonWidgetProperties.propMaximum.createProperty(widget, 100.0),
                                PlotWidgetProperties.propTitleFont.createProperty(widget, WidgetFontService.get(NamedWidgetFonts.DEFAULT_BOLD)),
                                PlotWidgetProperties.propScaleFont.createProperty(widget, WidgetFontService.get(NamedWidgetFonts.DEFAULT))));
        }

        public WidgetProperty<Boolean> visible()        { return getElement(0); }
        public WidgetProperty<String> title()           { return getElement(1); }
        public WidgetProperty<Double> minimum()         { return getElement(2); }
        public WidgetProperty<Double> maximum()         { return getElement(3); }
        public WidgetProperty<WidgetFont> titleFont()   { return getElement(4); }
        public WidgetProperty<WidgetFont> scaleFont()   { return getElement(5); }
    };

    private final static StructuredWidgetProperty.Descriptor propXAxis =
        new Descriptor(WidgetPropertyCategory.DISPLAY, "x_axis", Messages.PlotWidget_XAxis);

    private final static StructuredWidgetProperty.Descriptor propYAxis =
        new Descriptor(WidgetPropertyCategory.DISPLAY, "y_axis", Messages.PlotWidget_YAxis);

    /** Structure for X axis */
    private static class XAxisWidgetProperty extends AxisWidgetProperty
    {
        public XAxisWidgetProperty(final Widget widget)
        {
            super(propXAxis, widget, "X");
        }
    };

    /** Structure for Y axis */
    private static class YAxisWidgetProperty extends AxisWidgetProperty
    {
        public YAxisWidgetProperty(final Widget widget)
        {
            super(propYAxis, widget, "Y");
        }
    };

    /** Image data information */
    private static final WidgetPropertyDescriptor<Integer> propDataWidth =
        new WidgetPropertyDescriptor<>(
            WidgetPropertyCategory.BEHAVIOR, "data_width", Messages.WidgetProperties_DataWidth)
    {
        @Override
        public WidgetProperty<Integer> createProperty(final Widget widget,
                                                      final Integer width)
        {
            return new IntegerWidgetProperty(this, widget, width);
        }
    };

    private static final WidgetPropertyDescriptor<Integer> propDataHeight =
        new WidgetPropertyDescriptor<>(
            WidgetPropertyCategory.BEHAVIOR, "data_height", Messages.WidgetProperties_DataHeight)
    {
        @Override
        public WidgetProperty<Integer> createProperty(final Widget widget,
                                                      final Integer height)
        {
            return new IntegerWidgetProperty(this, widget, height);
        }
    };

    private static final WidgetPropertyDescriptor<Boolean> propDataUnsigned =
        CommonWidgetProperties.newBooleanPropertyDescriptor(
            WidgetPropertyCategory.BEHAVIOR, "unsigned", Messages.WidgetProperties_UnsignedData);

    private static final WidgetPropertyDescriptor<String> propCursorInfoPV =
        CommonWidgetProperties.newPVNamePropertyDescriptor(WidgetPropertyCategory.MISC, "cursor_info_pv", Messages.WidgetProperties_CursorInfoPV);

    private static final WidgetPropertyDescriptor<String> propCursorXPV =
        CommonWidgetProperties.newPVNamePropertyDescriptor(WidgetPropertyCategory.MISC, "x_pv", Messages.WidgetProperties_CursorXPV);

    private static final WidgetPropertyDescriptor<String> propCursorYPV =
        CommonWidgetProperties.newPVNamePropertyDescriptor(WidgetPropertyCategory.MISC, "y_pv", Messages.WidgetProperties_CursorYPV);

    /** Runtime info about cursor location */
    private static final WidgetPropertyDescriptor<VType> runtimePropCursorInfo =
        CommonWidgetProperties.newRuntimeValue("cursor_info", Messages.WidgetProperties_CursorInfo);

    private static final WidgetPropertyDescriptor<Boolean> propCursorCrosshair =
        CommonWidgetProperties.newBooleanPropertyDescriptor(WidgetPropertyCategory.MISC, "cursor_crosshair", Messages.WidgetProperties_CursorCrosshair);

    private static final WidgetPropertyDescriptor<Double[]> propCrosshairLocation =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.RUNTIME, "crosshair_location", "Crosshair Location")
        {
            @Override
            public WidgetProperty<Double[]> createProperty(final Widget widget, final Double[] value)
            {
                return new RuntimeWidgetProperty<>(this, widget, value)
                {
                    @Override
                    public void setValueFromObject(final Object value) throws Exception
                    {
                        if (value instanceof Double[])
                            setValue((Double[]) value);
                        else
                            throw new Exception("Need Double[], got " + value);
                    }
                };
            }
        };

    /** Structure for ROI */
    private static final WidgetPropertyDescriptor<String> propXPVName =
        CommonWidgetProperties.newPVNamePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "x_pv", Messages.WidgetProperties_XPVName);

    private static final WidgetPropertyDescriptor<String> propYPVName =
        CommonWidgetProperties.newPVNamePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "y_pv", Messages.WidgetProperties_YPVName);

    private static final WidgetPropertyDescriptor<String> propWidthPVName =
        CommonWidgetProperties.newPVNamePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "width_pv", Messages.WidgetProperties_WidthPVName);

    private static final WidgetPropertyDescriptor<String> propHeightPVName =
        CommonWidgetProperties.newPVNamePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "height_pv", Messages.WidgetProperties_HeightPVName);

    public static final WidgetPropertyDescriptor<Double> propXValue =
        CommonWidgetProperties.newDoublePropertyDescriptor(WidgetPropertyCategory.RUNTIME, "x_value", Messages.WidgetProperties_X);

    public static final WidgetPropertyDescriptor<Double> propYValue =
        CommonWidgetProperties.newDoublePropertyDescriptor(WidgetPropertyCategory.RUNTIME, "y_value", Messages.WidgetProperties_Y);

    public static final WidgetPropertyDescriptor<Double> propWidthValue =
        CommonWidgetProperties.newDoublePropertyDescriptor(WidgetPropertyCategory.RUNTIME, "width_value", Messages.WidgetProperties_Width);

    public static final WidgetPropertyDescriptor<Double> propHeightValue =
        CommonWidgetProperties.newDoublePropertyDescriptor(WidgetPropertyCategory.RUNTIME, "height_value", Messages.WidgetProperties_Height);

    private final static StructuredWidgetProperty.Descriptor propROI =
            new Descriptor(WidgetPropertyCategory.DISPLAY, "roi", "Region of Interest");

    public static class ROIWidgetProperty extends StructuredWidgetProperty
    {
        protected ROIWidgetProperty(final Widget widget, final String name)
        {
            super(propROI, widget,
                  Arrays.asList(CommonWidgetProperties.propName.createProperty(widget, name),
                                propColor.createProperty(widget, new WidgetColor(255, 0, 0)),
                                CommonWidgetProperties.propVisible.createProperty(widget, true),
                                propInteractive.createProperty(widget, true),
                                propXPVName.createProperty(widget, ""),
                                propYPVName.createProperty(widget, ""),
                                propWidthPVName.createProperty(widget, ""),
                                propHeightPVName.createProperty(widget, ""),
                                propFile.createProperty(widget, ""),
                                propXValue.createProperty(widget, Double.NaN),
                                propYValue.createProperty(widget, Double.NaN),
                                propWidthValue.createProperty(widget, Double.NaN),
                                propHeightValue.createProperty(widget, Double.NaN) ));
        }

        public WidgetProperty<String> name()           { return getElement(0); }
        public WidgetProperty<WidgetColor> color()     { return getElement(1); }
        public WidgetProperty<Boolean> visible()       { return getElement(2); }
        public WidgetProperty<Boolean> interactive()   { return getElement(3); }
        public WidgetProperty<String> x_pv()           { return getElement(4); }
        public WidgetProperty<String> y_pv()           { return getElement(5); }
        public WidgetProperty<String> width_pv()       { return getElement(6); }
        public WidgetProperty<String> height_pv()      { return getElement(7); }
        public WidgetProperty<String> file()           { return getElement(8); }
        public WidgetProperty<Double> x_value()        { return getElement(9); }
        public WidgetProperty<Double> y_value()        { return getElement(10); }
        public WidgetProperty<Double> width_value()    { return getElement(11); }
        public WidgetProperty<Double> height_value()   { return getElement(12); }
    };

    /** 'roi' array */
    public static final ArrayWidgetProperty.Descriptor<ROIWidgetProperty> propROIs =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.MISC, "rois", "Regions of Interest",
                                             (widget, index) -> new ROIWidgetProperty(widget, "ROI " + index),
                                             0);

    /** Legacy properties that have already triggered a warning */
    private final CopyOnWriteArraySet<String> warnings_once = new CopyOnWriteArraySet<>();
    
    /** Configurator for legacy widgets */
    private class CustomWidgetConfigurator extends WidgetConfigurator
    {
        public CustomWidgetConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                final Element xml) throws Exception
        {
            final ImageWidget image = (ImageWidget) widget;
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            if (xml_version.getMajor() < 2)
            {   // Legacy had no autoscale
                image.data_autoscale.setValue(false);

                XMLUtil.getChildString(xml, "show_ramp")
                       .ifPresent(show -> image.color_bar.visible().setValue(Boolean.parseBoolean(show)));

                // Background color was ignored, always transparent
                image.propBackground().setValue(NamedWidgetColors.TRANSPARENT);

                final Element el = XMLUtil.getChildElement(xml, "font");
                if (el != null)
                    image.propColorbar().scaleFont().readFromXML(model_reader, el);

                XMLUtil.getChildString(xml, "x_axis_visible")
                       .ifPresent(show -> image.x_axis.visible().setValue(Boolean.parseBoolean(show)));
                XMLUtil.getChildDouble(xml, "x_axis_minimum")
                       .ifPresent(value -> image.x_axis.minimum().setValue(value));
                XMLUtil.getChildDouble(xml, "x_axis_maximum")
                       .ifPresent(value -> image.x_axis.maximum().setValue(value));
                XMLUtil.getChildString(xml, "x_axis_axis_title")
                       .ifPresent(title ->  image.x_axis.title().setValue(title));

                XMLUtil.getChildString(xml, "y_axis_visible")
                       .ifPresent(show -> image.y_axis.visible().setValue(Boolean.parseBoolean(show)));
                XMLUtil.getChildDouble(xml, "y_axis_minimum")
                       .ifPresent(value -> image.y_axis.minimum().setValue(value));
                XMLUtil.getChildDouble(xml, "y_axis_maximum")
                       .ifPresent(value -> image.y_axis.maximum().setValue(value));
                XMLUtil.getChildString(xml, "y_axis_axis_title")
                       .ifPresent(title ->  image.y_axis.title().setValue(title));
            }

            return true;
        }
    }

    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<Boolean> show_toolbar;
    private volatile WidgetProperty<ColorMap> data_colormap;
    private volatile ColorBarProperty color_bar;
    private volatile AxisWidgetProperty x_axis, y_axis;
    private volatile WidgetProperty<Integer> data_width, data_height;
    private volatile WidgetProperty<InterpolationType> data_interpolation;
    private volatile WidgetProperty<VImageType> data_color_mode;
    private volatile WidgetProperty<Boolean> data_unsigned;
    private volatile WidgetProperty<Boolean> data_autoscale;
    private volatile WidgetProperty<Boolean> data_logscale;
    private volatile WidgetProperty<Double> data_minimum, data_maximum;
    private volatile WidgetProperty<String> cursor_info_pv, cursor_x_pv, cursor_y_pv;
    private volatile WidgetProperty<VType> cursor_info;
    private volatile WidgetProperty<Boolean> cursor_crosshair;
    private volatile WidgetProperty<Double[]> crosshair_location;
    private volatile ArrayWidgetProperty<ROIWidgetProperty> rois;
    private volatile RuntimeEventProperty configure;

    public ImageWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 400, 300);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(show_toolbar = propToolbar.createProperty(this,false));
        properties.add(data_colormap = propDataColormap.createProperty(this, PredefinedColorMaps.VIRIDIS));
        properties.add(color_bar = new ColorBarProperty(this));
        properties.add(x_axis = new XAxisWidgetProperty(this));
        properties.add(y_axis = new YAxisWidgetProperty(this));
        properties.add(data_width = propDataWidth.createProperty(this, 100));
        properties.add(data_height = propDataHeight.createProperty(this, 100));
        properties.add(data_interpolation = propInterpolationType.createProperty(this, InterpolationType.AUTOMATIC));
        properties.add(data_color_mode = propColorMode.createProperty(this, VImageType.TYPE_MONO));
        properties.add(data_unsigned = propDataUnsigned.createProperty(this, false));
        properties.add(data_autoscale = PlotWidgetProperties.propAutoscale.createProperty(this, true));
        properties.add(data_logscale = PlotWidgetProperties.propLogscale.createProperty(this, false));
        properties.add(data_minimum = propMinimum.createProperty(this, 0.0));
        properties.add(data_maximum = propMaximum.createProperty(this, 255.0));
        properties.add(cursor_info_pv = propCursorInfoPV.createProperty(this, ""));
        properties.add(cursor_x_pv = propCursorXPV.createProperty(this, ""));
        properties.add(cursor_y_pv = propCursorYPV.createProperty(this, ""));
        properties.add(cursor_info = runtimePropCursorInfo.createProperty(this, null));
        properties.add(cursor_crosshair = propCursorCrosshair.createProperty(this, false));
        properties.add(crosshair_location = propCrosshairLocation.createProperty(this, null));
        properties.add(rois = propROIs.createProperty(this, Collections.emptyList()));
        properties.add(configure = (RuntimeEventProperty) runtimePropConfigure.createProperty(this, null));
    }

    @Override
    protected String getInitialTooltip()
    {
        return "$(pv_name)";
    }
    
    @Override
    public WidgetProperty<?> getProperty(final String name)
    {
	Matcher matcher = LEGACY_AXIS_PATTERN.matcher(name);
        if (matcher.matches())
        {
            final String axis_type = matcher.group(1);
            final String new_name = axis_type + "_axis." + matcher.group(2)
                                                  .replace("axis_title", "title")
                                                  .replace("auto_scale", "autoscale");
            if (warnings_once.add(name))
                logger.log(Level.WARNING, "Deprecated access to " + this + " property '" + name + "'. Use '" + new_name + "'");
            return getProperty(new_name);
        }
        return super.getProperty(name);
    }
    
    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new CustomWidgetConfigurator(persisted_version);
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackground()
    {
        return background;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'show_toolbar' property */
    public WidgetProperty<Boolean> propToolbar()
    {
        return show_toolbar;
    }

    /** @return 'color_map' property*/
    public WidgetProperty<ColorMap> propDataColormap()
    {
        return data_colormap;
    }

    /** @return 'color_bar' property */
    public ColorBarProperty propColorbar()
    {
        return color_bar;
    }

    /** @return 'x_axis' property */
    public AxisWidgetProperty propXAxis()
    {
        return x_axis;
    }

    /** @return 'y_axis' property */
    public AxisWidgetProperty propYAxis()
    {
        return y_axis;
    }

    /** @return 'data_width' property */
    public WidgetProperty<Integer> propDataWidth()
    {
        return data_width;
    }

    /** @return 'data_height' property */
    public WidgetProperty<Integer> propDataHeight()
    {
        return data_height;
    }

    /** @return 'interpolation' property */
    public WidgetProperty<InterpolationType> propDataInterpolation()
    {
        return data_interpolation;
    }

    /** @return 'color_mode' property */
    public WidgetProperty<VImageType> propDataColorMode()
    {
        return data_color_mode;
    }

    /** @return 'unsigned' property */
    public WidgetProperty<Boolean> propDataUnsigned()
    {
        return data_unsigned;
    }

    /** @return 'autoscale' property */
    public WidgetProperty<Boolean> propDataAutoscale()
    {
        return data_autoscale;
    }

    /** @return 'logscale' property */
    public WidgetProperty<Boolean> propDataLogscale()
    {
        return data_logscale;
    }

    /** @return 'minimum' property */
    public WidgetProperty<Double> propDataMinimum()
    {
        return data_minimum;
    }

    /** @return 'maximum' property */
    public WidgetProperty<Double> propDataMaximum()
    {
        return data_maximum;
    }

    /** @return 'cursor_info_pv' property */
    public WidgetProperty<String> propCursorInfoPV()
    {
        return cursor_info_pv;
    }

    /** @return 'x_pv' property */
    public WidgetProperty<String> propCursorXPV()
    {
        return cursor_x_pv;
    }

    /** @return 'y_pv' property */
    public WidgetProperty<String> propCursorYPV()
    {
        return cursor_y_pv;
    }

    /** @return Runtime 'cursor_info' property */
    public WidgetProperty<VType> runtimePropCursorInfo()
    {
        return cursor_info;
    }

    /** @return 'cursor_crosshair' property */
    public WidgetProperty<Boolean> propCursorCrosshair()
    {
        return cursor_crosshair;
    }

    /** @return Runtime property for location of cursor crosshair, holds Double[] { x, y } */
    public WidgetProperty<Double[]> runtimePropCrosshair()
    {
        return crosshair_location;
    }

    /** @return 'rois' property */
    public ArrayWidgetProperty<ROIWidgetProperty> propROIs()
    {
        return rois;
    }

    /** @return 'configure' property */
    public RuntimeEventProperty runtimePropConfigure()
    {
        return configure;
    }
}
