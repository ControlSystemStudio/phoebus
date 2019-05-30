/*******************************************************************************
 * Copyright (c) 2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.VLineTo;
import javafx.stage.Stage;

public class ThermoDemo extends ApplicationWrapper
{
    private double myHeight = 200;
    private double myWidth = 100;

    public static void main(final String[] args)
    {
        launch(ThermoDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Thermo thermo = new Thermo();
        thermo.setMin(0);
        thermo.setMax(100);
        thermo.setValue(50);

        final Slider value_slider = new Slider(0, 100, 50);
        value_slider.valueProperty().addListener((obs, oldv, newv) -> thermo.setValue(newv != null ? (Double) newv : 0));
        final Slider width_slider = new Slider(0, 500, myHeight);
        width_slider.valueProperty().addListener((obs, oldv, newv) ->
        {
            myWidth = (Double) newv;
            thermo.requestLayout();
        });
        final Slider height_slider = new Slider(0, 500, myWidth);
        height_slider.valueProperty().addListener((obs, oldv, newv) ->
        {
            myHeight = (Double) newv;
            thermo.requestLayout();
        });

        final Pane root = new Pane(new HBox(new VBox(value_slider, width_slider, height_slider), thermo));
        final Scene scene = new Scene(root, 400, 400);

        stage.setScene(scene);
        stage.show();
    }

    private class Thermo extends Region
    {
        private final Ellipse ellipse = new Ellipse();
        private final Rectangle fill = new Rectangle();

        private final MoveTo move = new MoveTo();
        private final VLineTo left = new VLineTo();
        private final ArcTo arc = new ArcTo();
        private final Path border = new Path(move, new VLineTo(0), new HLineTo(0), left, arc);

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

            fill.setArcHeight(3);
            fill.setArcWidth(3);
            fill.setManaged(false);
            border.setFill(new LinearGradient(.3, 0, .7, 0, true, CycleMethod.NO_CYCLE,
                                            new Stop(0, Color.LIGHTGRAY),
                                            new Stop(.3, Color.WHITESMOKE),
                                            new Stop(1, Color.LIGHTGRAY)));
            border.setStroke(Color.BLACK);
            arc.setLargeArcFlag(true);

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
            final double w = clamp(width / 2, 0, 20);
            final double d = clamp(width, w, Math.min(height, 2 * w));
            final double h = height - d;

            fill.setWidth(Math.max(w - 4, 0));
            ellipse.setRadiusX(Math.max(d / 2 - 2, 0));
            ellipse.setRadiusY(Math.max(d / 2 - 2, 0));

            move.setX(w);
            move.setY(h);
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
            return myWidth;
        }

        @Override
        protected double computePrefHeight(double width)
        {
            return myHeight;
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

        public void setMax(double val)
        {
            max = val;
            adjustFill();
        }

        public void setMin(double val)
        {
            min = val;
            adjustFill();
        }

        public void setValue(double val)
        {
            value = val;
            adjustFill();
        }

        private void adjustFill()
        {
            adjustFill(computePrefWidth(0), computePrefHeight(0), ellipse.getRadiusX() * 2);
        }

        private void adjustFill(double width, double height, double d)
        {
            fill.setHeight(d / 2 + (height - d) * (value - min) / (max - min));
            layoutInArea(fill, 0.0, -d / 2, width, height, 0, HPos.CENTER, VPos.BOTTOM);
        }
    }
}
