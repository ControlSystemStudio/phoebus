/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ThermometerWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.Display;
import org.epics.vtype.VType;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.VLineTo;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
public class ThermometerRepresentation extends RegionBaseRepresentation<Region, ThermometerWidget>
{
    private final DirtyFlag dirty_look = new DirtyFlag();
    private final DirtyFlag dirty_value = new DirtyFlag();
    private final UntypedWidgetPropertyListener lookListener = this::lookChanged;
    private final UntypedWidgetPropertyListener valueListener = this::valueChanged;

    private volatile double max = 0.0;
    private volatile double min = 0.0;
    private volatile double val = 0.0;

    @Override
    public Region createJFXNode() throws Exception
    {
        final Thermo thermo = new Thermo();

        // This code manages layout,
        // because otherwise for example border changes would trigger
        // expensive Node.notifyParentOfBoundsChange() recursing up the scene graph
        thermo.setManaged(false);

        return thermo;
    }

    private class Thermo extends Region
    {
        // Doesn't fully resize:
        // Width grows until certain limit
        private final Ellipse ellipse = new Ellipse();
        private final Rectangle fill = new Rectangle();

        private final MoveTo move = new MoveTo();
        private final VLineTo left = new VLineTo();
        private final ArcTo arc = new ArcTo();
        private final LineTo rightcorner = new LineTo();
        private final Path border = new Path(move, new VLineTo(3), rightcorner,
                                             new HLineTo(3), new LineTo(0, 3), left, arc);

        private double max = 0;
        private double min = 100;
        private double value = 50;

        Thermo()
        {
            this(Color.BLUE); //blue
        }

        Thermo(Color color)
        {
            setFill(color);

            fill.setArcHeight(6);
            fill.setArcWidth(6);
            fill.setManaged(false);
            border.setFill(new LinearGradient(.3, 0, .7, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.LIGHTGRAY),
                    new Stop(.3, Color.WHITESMOKE),
                    new Stop(1, Color.LIGHTGRAY)));
            border.setStroke(Color.BLACK);
            arc.setLargeArcFlag(true);
            rightcorner.setY(0);

            getChildren().add(border);
            getChildren().add(fill);
            getChildren().add(ellipse);
            setBorder(new Border(
                    new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        }

        private double clamp(double val, double min, double max)
        {
            return Math.max(Math.min(val, max), min);
        }

        @Override
        protected void layoutChildren()
        {
            final double width = computePrefWidth(0);
            final double height = computePrefHeight(0);
            // Limit width
            final double w = clamp(width / 2, 0, 20);
            final double d = clamp(width, w, Math.min(height, 2 * w));
            final double h = height - d;

            fill.setWidth(Math.max(w - 4, 0));
            ellipse.setRadiusX(Math.max(d / 2 - 2, 0));
            ellipse.setRadiusY(Math.max(d / 2 - 2, 0));

            move.setX(w);
            move.setY(h);
            rightcorner.setX(Math.max(w - 3, 0));
            left.setY(h);
            arc.setRadiusX(d / 2);
            arc.setRadiusY(d / 2);
            arc.setX(w);
            arc.setY(h);

            adjustFill(width, height, d);

            layoutInArea(ellipse, 0, -3, width, height, 0, HPos.CENTER, VPos.BOTTOM);
            layoutInArea(border, 0, 0, width, height, 0, HPos.CENTER, VPos.BOTTOM);
        }

        @Override
        protected double computePrefWidth(double height)
        {
            return model_widget.propWidth().getValue();
        }

        @Override
        protected double computePrefHeight(double width)
        {
            return model_widget.propHeight().getValue();
        }

        public void setFill(Color color)
        {
            ellipse.setFill(
                    new RadialGradient(0, 0, 0.3, 0.1, 0.4, true, CycleMethod.NO_CYCLE,
                            new Stop(0, color.interpolate(Color.WHITESMOKE, 0.8)),
                            new Stop(1, color)));
            fill.setFill(new LinearGradient(0, 0, .8, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, color),
                    new Stop(.3, color.interpolate(Color.WHITESMOKE, 0.7)),
                    new Stop(1, color)));
        }

        public void setLimits(double min, double max, double val)
        {
            this.min = min;
            this.max = max;
            this.value = val;
            adjustFill();
        }

        private void adjustFill()
        {
            adjustFill(computePrefWidth(0), computePrefHeight(0), ellipse.getRadiusX() * 2);
        }

        private void adjustFill(double width, double height, double d)
        {
            // '-4' leaves one pixel between top and border,
            // just like there's one pixel all around to the border
            fill.setHeight(d / 2 + (height - d) * (value - min) / (max - min) - 4);
            layoutInArea(fill, 0.0, -d / 2, width, height, 0, HPos.CENTER, VPos.BOTTOM);
        }
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propFillColor().addUntypedPropertyListener(lookListener);
        model_widget.propWidth().addUntypedPropertyListener(lookListener);
        model_widget.propHeight().addUntypedPropertyListener(lookListener);
        model_widget.propLimitsFromPV().addUntypedPropertyListener(valueListener);
        model_widget.propMinimum().addUntypedPropertyListener(valueListener);
        model_widget.propMaximum().addUntypedPropertyListener(valueListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(valueListener);
        valueChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propFillColor().removePropertyListener(lookListener);
        model_widget.propWidth().removePropertyListener(lookListener);
        model_widget.propHeight().removePropertyListener(lookListener);
        model_widget.propLimitsFromPV().removePropertyListener(valueListener);
        model_widget.propMinimum().removePropertyListener(valueListener);
        model_widget.propMaximum().removePropertyListener(valueListener);
        model_widget.runtimePropValue().removePropertyListener(valueListener);
        super.unregisterListeners();
    }

    private void lookChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    private void valueChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final VType vtype = model_widget.runtimePropValue().getValue();

        final boolean limits_from_pv = model_widget.propLimitsFromPV().getValue();
        double min_val = model_widget.propMinimum().getValue();
        double max_val = model_widget.propMaximum().getValue();
        if (limits_from_pv)
        {
            // Try display range from PV
            final Display display_info = Display.displayOf(vtype);
            if (display_info != null)
            {
                min_val = display_info.getDisplayRange().getMinimum();
                max_val = display_info.getDisplayRange().getMaximum();
            }
        }
        // Fall back to 0..100 range
        if (min_val >= max_val)
        {
            min_val = 0.0;
            max_val = 100.0;
        }

        final double value = VTypeUtil.getValueNumber(vtype).doubleValue();
        min = min_val;
        max = max_val;
        val = Math.min(max, value); // Avoid rendering value beyond maximum
        dirty_value.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_look.checkAndClear())
        {
            ((Thermo) jfx_node).setFill(JFXUtil.convert(model_widget.propFillColor().getValue()));
            jfx_node.resize(model_widget.propWidth().getValue(), model_widget.propHeight().getValue());
        }
        if (dirty_value.checkAndClear())
            ((Thermo) jfx_node).setLimits(min, max, val);
    }
}
