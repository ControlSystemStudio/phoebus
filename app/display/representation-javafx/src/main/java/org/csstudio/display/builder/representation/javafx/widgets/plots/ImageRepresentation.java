/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.ColorMap;
import org.csstudio.display.builder.model.properties.PredefinedColorMaps;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.plots.ImageWidget;
import org.csstudio.display.builder.model.widgets.plots.ImageWidget.AxisWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.ImageWidget.ROIWidgetProperty;
import org.csstudio.display.builder.representation.Preferences;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.ColorMappingFunction;
import org.csstudio.javafx.rtplot.Interpolation;
import org.csstudio.javafx.rtplot.NamedColorMappings;
import org.csstudio.javafx.rtplot.RTImagePlot;
import org.csstudio.javafx.rtplot.RTImagePlotListener;
import org.csstudio.javafx.rtplot.RegionOfInterest;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayInteger;
import org.epics.vtype.VImage;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.phoebus.framework.macros.MacroHandler;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageRepresentation extends RegionBaseRepresentation<Pane, ImageWidget>
{
    private final DirtyFlag dirty_position = new DirtyFlag();

    /** Actual plotting delegated to {@link RTImagePlot} */
    private RTImagePlot image_plot;

    private final UntypedWidgetPropertyListener coloringChangedListener = this::coloringChanged;
    private final UntypedWidgetPropertyListener configChangedListener = this::configChanged;
    private final UntypedWidgetPropertyListener contentChangedListener = this::contentChanged;
    private final UntypedWidgetPropertyListener positionChangedListener = this::positionChanged;
    private final UntypedWidgetPropertyListener rangeChangedListener = this::rangeChanged;
    private final WidgetPropertyListener<Double[]> crosshairChangedListener = this::crosshairChanged;
    private final WidgetPropertyListener<Instant> runtimeConfigChangedListener = (p, o, n) -> image_plot.showConfigurationDialog();

    private volatile boolean changing_roi = false,
                             changing_range = false,
                             changing_axis_range = false;

    private final static List<String> cursor_info_names = Arrays.asList("X", "Y", "Value", "xi", "yi");
    private final static List<Class<?>> cursor_info_types = Arrays.asList(Double.TYPE, Double.TYPE, Double.TYPE, Integer.TYPE, Integer.TYPE);

    private final RTImagePlotListener plot_listener = new RTImagePlotListener()
    {
        @Override
        public void changedCursorInfo(final double x, final double y, final int xi, final int yi, final double value)
        {
            model_widget.runtimePropCursorInfo().setValue(
                    VTable.of(cursor_info_types,
                              cursor_info_names,
                              Arrays.asList(ArrayDouble.of(x), ArrayDouble.of(y),
                                            ArrayDouble.of(value),
                                            ArrayInteger.of(xi), ArrayInteger.of(yi))));
        }

        @Override
        public void changedCrosshair(final double x, final double y)
        {
            model_widget.runtimePropCrosshair().setValue(new Double[] { x, y });
        }

        @Override
        public void changedROI(final int index, final String name, final Rectangle2D region)
        {
            if (changing_roi)
                return;
            final ROIWidgetProperty widget_roi = model_widget.propROIs().getValue().get(index);
            changing_roi =  true;
            widget_roi.x_value().setValue(region.getMinX());
            widget_roi.y_value().setValue(region.getMinY());
            widget_roi.width_value().setValue(region.getWidth());
            widget_roi.height_value().setValue(region.getHeight());
            changing_roi =  false;
        }

        @Override
        public void changedXAxis(double low, double high)
        {
            changing_axis_range = true;
            model_widget.propXAxis().minimum().setValue(low);
            model_widget.propXAxis().maximum().setValue(high);
            changing_axis_range = false;
        }

        @Override
        public void changedYAxis(double low, double high)
        {
            changing_axis_range = true;
            model_widget.propYAxis().minimum().setValue(low);
            model_widget.propYAxis().maximum().setValue(high);
            changing_axis_range = false;
        }

        @Override
        public void changedAutoScale(final boolean autoscale)
        {
            changing_range = true;
            model_widget.propDataAutoscale().setValue(autoscale);
            changing_range = false;
        }

        @Override
        public void changedLogarithmic(final boolean logscale)
        {
            changing_range = true;
            model_widget.propDataLogscale().setValue(logscale);
            changing_range = false;
        }

        @Override
        public void changedValueRange(final double minimum, final double maximum)
        {
            changing_range = true;
            model_widget.propDataMinimum().setValue(minimum);
            model_widget.propDataMaximum().setValue(maximum);
            changing_range = false;
        }
    };

    @Override
    public Pane createJFXNode() throws Exception
    {
        // Plot is only active in runtime mode, not edit mode
        image_plot = new RTImagePlot(! toolkit.isEditMode());
        image_plot.setUpdateThrottle(Preferences.image_update_delay, TimeUnit.MILLISECONDS);
        image_plot.setAutoscale(false);
        image_plot.setManaged(false);

        if (! toolkit.isEditMode())
        {
            // Create ROIs once. Not allowing adding/removing ROIs in runtime.
            for (ROIWidgetProperty roi : model_widget.propROIs().getValue())
                createROI(roi);
        }

        return new Pane(image_plot);
    }

    @Override
    protected void configurePVNameDrag()
    {
        // Don't drag PV name, we use drag to pan or zoom
    }

    private void createROI(final ROIWidgetProperty model_roi)
    {
        final RegionOfInterest plot_roi = image_plot.addROI(model_roi.name().getValue(),
                                                            JFXUtil.convert(model_roi.color().getValue()),
                                                            model_roi.visible().getValue(),
                                                            model_roi.interactive().getValue());
        // Show/hide ROI as roi.visible() changes
        model_roi.visible().addPropertyListener((prop, old, visible) ->
        {
            plot_roi.setVisible(visible);
            Platform.runLater(() -> image_plot.removeROITracker());
            image_plot.requestUpdate();
        });

        // Allow interactive adjustment of ROI?
        model_roi.interactive().addPropertyListener((prop, old, interactive) ->
        {
            plot_roi.setInteractive(interactive);
            // Cancel tracker which might have been active
            if (! interactive)
                Platform.runLater(() -> image_plot.removeROITracker());
        });
        // For now _not_ listening to runtime changes of roi.file() ...

        // Listen to roi.x_value(), .. and update plot_roi
        final WidgetPropertyListener<Double> model_roi_listener = (o, old, value) ->
        {
            if (changing_roi)
                return;
            Rectangle2D region = plot_roi.getRegion();
            region = new Rectangle2D(existingOrProperty(region.getMinX(), model_roi.x_value()),
                                     existingOrProperty(region.getMinY(), model_roi.y_value()),
                                     existingOrProperty(region.getWidth(), model_roi.width_value()),
                                     existingOrProperty(region.getHeight(), model_roi.height_value()));
            changing_roi = true;
            plot_roi.setRegion(region);
            changing_roi = false;
            image_plot.requestUpdate();
        };
        model_roi.x_value().addPropertyListener(model_roi_listener);
        model_roi.y_value().addPropertyListener(model_roi_listener);
        model_roi.width_value().addPropertyListener(model_roi_listener);
        model_roi.height_value().addPropertyListener(model_roi_listener);

        // Load image file (if there is one) on background thread
        ModelThreadPool.getExecutor().execute(() -> loadROI_Image(plot_roi, model_roi));
    }

    /** @param old Existing value
     *  @param prop Property that may have new value, or <code>null</code>
     *  @return Property's value, falling back to old value
     */
    private double existingOrProperty(final double old, final WidgetProperty<Double> prop)
    {
        final Double value = prop.getValue();
        if (value == null)
            return old;
        return value;
    }

    private void loadROI_Image(final RegionOfInterest plot_roi, final ROIWidgetProperty model_roi)
    {
        String image_name = model_roi.file().getValue();
        Image image = null;
        try
        {
            image_name = MacroHandler.replace(model_widget.getMacrosOrProperties(), image_name);
            if (! image_name.isEmpty())
            {
                // Resolve new image file relative to the source widget model (not 'top'!)
                // Get the display model from the widget tied to this representation
                final DisplayModel widget_model = model_widget.getDisplayModel();
                // Resolve the image path using the parent model file path
                image_name = ModelResourceUtil.resolveResource(widget_model, image_name);

                image = new Image(ModelResourceUtil.openResourceStream(image_name));
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load ROI image " + image_name, ex);
        }
        plot_roi.setImage(image);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(positionChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(positionChangedListener);

        model_widget.propToolbar().addUntypedPropertyListener(configChangedListener);
        model_widget.propBackground().addUntypedPropertyListener(configChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(configChangedListener);
        model_widget.propColorbar().visible().addUntypedPropertyListener(configChangedListener);
        model_widget.propColorbar().barSize().addUntypedPropertyListener(configChangedListener);
        model_widget.propColorbar().scaleFont().addUntypedPropertyListener(configChangedListener);
        model_widget.propCursorCrosshair().addUntypedPropertyListener(configChangedListener);
        addAxisListener(model_widget.propXAxis());
        addAxisListener(model_widget.propYAxis());

        model_widget.propDataInterpolation().addUntypedPropertyListener(coloringChangedListener);
        model_widget.propDataColormap().addUntypedPropertyListener(coloringChangedListener);
        model_widget.propDataAutoscale().addUntypedPropertyListener(rangeChangedListener);
        model_widget.propDataLogscale().addUntypedPropertyListener(rangeChangedListener);
        model_widget.propDataMinimum().addUntypedPropertyListener(rangeChangedListener);
        model_widget.propDataMaximum().addUntypedPropertyListener(rangeChangedListener);

        model_widget.propDataWidth().addUntypedPropertyListener(contentChangedListener);
        model_widget.propDataHeight().addUntypedPropertyListener(contentChangedListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(contentChangedListener);

        model_widget.runtimePropCrosshair().addPropertyListener(crosshairChangedListener);

        model_widget.runtimePropConfigure().addPropertyListener(runtimeConfigChangedListener);

        image_plot.setListener(plot_listener);

        // Initial update
        rangeChanged(null, null, null);
        coloringChanged(null, null,null);
        configChanged(null, null, null);
        contentChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(positionChangedListener);
        model_widget.propHeight().removePropertyListener(positionChangedListener);
        model_widget.propToolbar().removePropertyListener(configChangedListener);
        model_widget.propBackground().removePropertyListener(configChangedListener);
        model_widget.propForegroundColor().removePropertyListener(configChangedListener);
        model_widget.propColorbar().visible().removePropertyListener(configChangedListener);
        model_widget.propColorbar().barSize().removePropertyListener(configChangedListener);
        model_widget.propColorbar().scaleFont().removePropertyListener(configChangedListener);
        model_widget.propCursorCrosshair().removePropertyListener(configChangedListener);
        removeAxisListener(model_widget.propXAxis());
        removeAxisListener(model_widget.propYAxis());
        model_widget.propDataInterpolation().removePropertyListener(coloringChangedListener);
        model_widget.propDataColormap().removePropertyListener(coloringChangedListener);
        model_widget.propDataAutoscale().removePropertyListener(rangeChangedListener);
        model_widget.propDataLogscale().removePropertyListener(rangeChangedListener);
        model_widget.propDataMinimum().removePropertyListener(rangeChangedListener);
        model_widget.propDataMaximum().removePropertyListener(rangeChangedListener);
        model_widget.propDataWidth().removePropertyListener(contentChangedListener);
        model_widget.propDataHeight().removePropertyListener(contentChangedListener);
        model_widget.runtimePropValue().removePropertyListener(contentChangedListener);
        model_widget.runtimePropCrosshair().removePropertyListener(crosshairChangedListener);
        model_widget.runtimePropConfigure().removePropertyListener(runtimeConfigChangedListener);
        super.unregisterListeners();
    }

    private void addAxisListener(final AxisWidgetProperty axis)
    {
        axis.visible().addUntypedPropertyListener(configChangedListener);
        axis.title().addUntypedPropertyListener(configChangedListener);
        axis.minimum().addUntypedPropertyListener(configChangedListener);
        axis.maximum().addUntypedPropertyListener(configChangedListener);
        axis.titleFont().addUntypedPropertyListener(configChangedListener);
        axis.scaleFont().addUntypedPropertyListener(configChangedListener);
    }

    private void removeAxisListener(final AxisWidgetProperty axis)
    {
        axis.visible().removePropertyListener(configChangedListener);
        axis.title().removePropertyListener(configChangedListener);
        axis.minimum().removePropertyListener(configChangedListener);
        axis.maximum().removePropertyListener(configChangedListener);
        axis.titleFont().removePropertyListener(configChangedListener);
        axis.scaleFont().removePropertyListener(configChangedListener);
    }

    private void positionChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_position.mark();
        toolkit.scheduleUpdate(this);
    }

    /** Changes that affect the coloring of the image but not the zoom, size */
    private void coloringChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        image_plot.setInterpolation(Interpolation.values()[model_widget.propDataInterpolation().getValue().ordinal()]);
        final ColorMap colormap = model_widget.propDataColormap().getValue();

        final ColorMappingFunction map_function;
        if (colormap instanceof PredefinedColorMaps.Predefined)
            map_function = NamedColorMappings.getMapping(((PredefinedColorMaps.Predefined)colormap).getName());
        else
            map_function = value ->  ColorMappingFunction.getRGB(colormap.getColor(value));
        image_plot.setColorMapping(map_function);
    }

    /** Changes that affect the coloring of the image but not the zoom, size */
    private void rangeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        if (changing_range)
            return;
        image_plot.setAutoscale(model_widget.propDataAutoscale().getValue());
        image_plot.setLogscale(model_widget.propDataLogscale().getValue());
        image_plot.setValueRange(model_widget.propDataMinimum().getValue(),
                                 model_widget.propDataMaximum().getValue());
    }

    /** Changes that affect size of the data, resulting in a reset of the image's zoom & pan,
     *  or other operations that should be rare at runtime
     */
    private void configChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final Color fg = JFXUtil.convert(model_widget.propForegroundColor().getValue());

        image_plot.showToolbar(model_widget.propToolbar().getValue());
        image_plot.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
        image_plot.showColorMap(model_widget.propColorbar().visible().getValue());
        image_plot.setColorMapSize(model_widget.propColorbar().barSize().getValue());
        image_plot.setColorMapFont(JFXUtil.convert(model_widget.propColorbar().scaleFont().getValue()));
        image_plot.setColorMapForeground(fg);
        image_plot.showCrosshair(model_widget.propCursorCrosshair().getValue());
        image_plot.getXAxis().setColor(fg);
        image_plot.getYAxis().setColor(fg);

        if (! changing_axis_range)
        {
            image_plot.setAxisRange(model_widget.propXAxis().minimum().getValue(),
                                    model_widget.propXAxis().maximum().getValue(),
                                    model_widget.propYAxis().minimum().getValue(),
                                    model_widget.propYAxis().maximum().getValue());
            axisChanged(model_widget.propXAxis(), image_plot.getXAxis());
            axisChanged(model_widget.propYAxis(), image_plot.getYAxis());
        }
    }

    private void axisChanged(final AxisWidgetProperty property, final Axis<Double> axis)
    {
        axis.setVisible(property.visible().getValue());
        axis.setName(property.title().getValue());
        axis.setLabelFont(JFXUtil.convert(property.titleFont().getValue()));
        axis.setScaleFont(JFXUtil.convert(property.scaleFont().getValue()));
    }

    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final VType value = model_widget.runtimePropValue().getValue();
        if (value instanceof VNumberArray)
            image_plot.setValue(model_widget.propDataWidth().getValue(),
                                model_widget.propDataHeight().getValue(),
                                ((VNumberArray) value).getData(),
                                model_widget.propDataUnsigned().getValue(),
                                model_widget.propDataColorMode().getValue());
        else if (value instanceof VImage)
        {
            final VImage image = (VImage) value;
            boolean isUnsigned;
            switch (image.getDataType())
            {
	            case pvUByte:
	            case pvUShort:
	            case pvUInt:
	            case pvULong:
	            	isUnsigned = true;
	            	break;
            	default:
            		isUnsigned = false;
            }
            image_plot.setValue(image.getWidth(), image.getHeight(), image.getData(),
                                isUnsigned, image.getVImageType());
        }
        else if (value != null)
            logger.log(Level.WARNING, "Cannot draw image from {0}", value);
        // else: Ignore null values
    }

    private void crosshairChanged(final WidgetProperty<Double[]> property, final Double[] old_value, final Double[] new_value)
    {
        image_plot.setCrosshairLocation(new_value[0], new_value[1]);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_position.checkAndClear())
        {
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            image_plot.resize(w, h);
        }
        image_plot.requestUpdate();
    }

    @Override
    public void dispose()
    {
        image_plot.dispose();
        image_plot = null;
        super.dispose();
    }
}
