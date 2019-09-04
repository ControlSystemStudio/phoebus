/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.javafx.rtplot.util.RGBFactory;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;

/** Demo of {@link RTTimePlot}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimePlotDemo extends ApplicationWrapper
{
    final private static int MAX_SIZE = 10000;

    @Override
    public void start(final Stage stage) throws Exception
    {
        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.WARNING);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(logger.getLevel());

        final RTTimePlot plot = new RTTimePlot(true);
        plot.setTitle("Title of Time Demo");
        plot.setUpdateThrottle(200, TimeUnit.MILLISECONDS);
        plot.setScrollStep(Duration.ofSeconds(30));

        plot.getXAxis().setGridVisible(true);
        plot.getXAxis().setLabelFont(Font.font("Liberation Sans", 40));
        plot.getXAxis().setScaleFont(Font.font("Liberation Sans", FontPosture.ITALIC, 25));

        plot.getYAxes().get(0).setValueRange(-2.2, 3.2);
        plot.getYAxes().get(0).setGridVisible(true);

        plot.addYAxis("y2");
        plot.getYAxes().get(1).setValueRange(1.2, 6.2);
        plot.getYAxes().get(1).setLogarithmic(true);
        plot.getYAxes().get(1).setLabelFont(Font.font("Liberation Mono", 35));
        plot.getYAxes().get(1).setScaleFont(Font.font("Liberation Mono", 15));

        plot.addYAxis("Right").setOnRight(true);

        final RGBFactory colors = new RGBFactory();
        final DynamicDemoData[] data = new DynamicDemoData[]
        { new DynamicDemoData(MAX_SIZE, 5.0), new DynamicDemoData(MAX_SIZE, 10.0), new DynamicDemoData(MAX_SIZE, 20.0) };
        plot.addTrace("Fred", "socks", data[0], colors.next(), TraceType.AREA_DIRECT, 3, LineStyle.SOLID, PointType.NONE, 3, 0);
        plot.addTrace("Jane", "handbags", data[1], colors.next(), TraceType.AREA, 5, LineStyle.SOLID, PointType.NONE, 5, 1);
        plot.addTrace("Another", "mA", data[2], colors.next(), TraceType.LINES_DIRECT, 1, LineStyle.SOLID, PointType.TRIANGLES, 15, 2);

        plot.addMarker(Color.BLUE, true, Instant.now().plusSeconds(5));

        final AtomicBoolean run = new AtomicBoolean(true);
        // Update data at 50Hz
        final Thread update_data = new Thread(() ->
        {
            while (run.get())
            {
                for (DynamicDemoData trace : data)
                    trace.add();
                plot.requestUpdate();
                try
                {
                    Thread.sleep(1000/50);
                }
                catch (Exception e)
                {
                    // NOP
                }
            }
        }, "DemoDataUpdate");
        update_data.start();

        plot.showToolbar(false);

        plot.addListener(new RTPlotListener<Instant>()
        {
            @Override
            public void changedXAxis(Axis<Instant> x_axis)
            {
                System.out.println("X Axis changed: " + x_axis);
            }
            @Override
            public void changedYAxis(YAxis<Instant> y_axis)
            {
                System.out.println("Y Axis changed: " + y_axis);
            }
            @Override
            public void changedPlotMarker(final int index)
            {
                System.out.println("Moved " + plot.getMarkers().get(index));
            }
            @Override
            public void changedAnnotations()
            {
                System.out.println("Annotations changed");
            }
            @Override
            public void changedCursors()
            {
                // System.out.println("Cursors changed");
            }
        });

        final Scene scene = new Scene(plot, 600, 700);
        stage.setScene(scene);
        stage.setTitle("Time Plot Demo");
        stage.show();

        stage.setOnCloseRequest((event) ->
        {
            run.set(false);
            try
            {
                update_data.join();
            }
            catch (Exception e)
            {
                // Ignore, shutting down anyway
            }
            plot.dispose();
        });
    }
    public static void main(final String[] args)
    {
        launch(TimePlotDemo.class, args);
    }
}
