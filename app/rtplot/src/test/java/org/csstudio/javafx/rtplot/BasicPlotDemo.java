/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.javafx.rtplot.data.ArrayPlotDataProvider;
import org.csstudio.javafx.rtplot.data.SimpleDataItem;
import org.csstudio.javafx.rtplot.internal.MouseMode;
import org.csstudio.javafx.rtplot.internal.Plot;
import org.csstudio.javafx.rtplot.internal.TraceImpl;
import org.csstudio.javafx.rtplot.internal.YAxisImpl;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BasicPlotDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        Logger.getLogger("").setLevel(Level.FINE);
        for (Handler handler : Logger.getLogger("").getHandlers())
            handler.setLevel(Level.FINE);

        final Plot<Double> plot = new Plot<>(Double.class, true);
        plot.setTitle("Plot Demo");
        plot.getXAxis().setName("The horizontal quantities on 'X'");
        plot.addYAxis("Another Axis");
        plot.getYAxes().get(1).setOnRight(true);

        final ArrayPlotDataProvider<Double> data1 = new ArrayPlotDataProvider<>();
        final ArrayPlotDataProvider<Double> data2 = new ArrayPlotDataProvider<>();
        final ArrayPlotDataProvider<Double> data3 = new ArrayPlotDataProvider<>();
        for (double x = -10.0; x <= 10.0; x += 1.0)
            if (x == 2.0)
            {
            	data1.add(new SimpleDataItem<>(x, Double.NaN));
            	data2.add(new SimpleDataItem<>(x, Double.NaN));
                data3.add(new SimpleDataItem<>(x, Double.NaN));
            }
            else
            {
                data1.add(new SimpleDataItem<>(x, x*x - 5.0));
                data2.add(new SimpleDataItem<>(x, 2*x));
                data3.add(new SimpleDataItem<>(x, x*x + 5.0));
            }
		plot.addTrace(new TraceImpl<>("Demo Data", "socks", data1, Color.BLUE, TraceType.BARS, 0, LineStyle.SOLID, PointType.NONE, 15, 0));
        plot.addTrace(new TraceImpl<>("Demo Data", "socks", data1, Color.VIOLET, TraceType.BARS, 10, LineStyle.SOLID, PointType.NONE, 15, 0));
		plot.addTrace(new TraceImpl<>("More Data", "pants", data2, Color.RED, TraceType.AREA, 3, LineStyle.SOLID, PointType.SQUARES, 15, 1));
        plot.addTrace(new TraceImpl<>("More Data", "pants", data3, Color.GREEN, TraceType.LINES_DIRECT, 1, LineStyle.DASHDOT, PointType.XMARKS, 5, 0));
        plot.getXAxis().setValueRange(-12.0, 12.0);

        // a) Fixed range
//		plot.getYAxes().get(0).setValueRange(-10.0, 120.0);
//		plot.getYAxes().get(1).setValueRange(-25.0, 25.0);

        // b) Autoscale
//		plot.getYAxes().get(0).setAutoscale(true);
//		plot.getYAxes().get(1).setAutoscale(true);

        // c) Stagger
        plot.stagger(false);

        plot.showCrosshair(true);

        plot.setMouseMode(MouseMode.PAN);

		final Pane root = new Pane(plot);
		final ChangeListener<? super Number> resize_listener = (p, o, n) -> plot.setSize(root.getWidth(), root.getHeight());
        root.widthProperty().addListener(resize_listener);
        root.heightProperty().addListener(resize_listener);

        final Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Basic Plot Demo");
        stage.show();

        // Thread that periodically hides an axis and its traces
        final AtomicBoolean run = new AtomicBoolean(true);
        final Runnable hider = () ->
        {
            try
            {
                while (run.get())
                {
                    TimeUnit.SECONDS.sleep(2);
                    final int axis_index = 1;
                    final YAxisImpl<Double> axis = plot.getYAxes().get(axis_index);
                    final boolean visible = ! axis.isVisible();
                    for (Trace<?> trace : plot.getTraces())
                        if (trace.getYAxis() == axis_index)
                            trace.setVisible(visible);
                    axis.setVisible(visible);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        };
        final Thread thread = new Thread(hider);
        thread.setDaemon(true);
        thread.start();

        stage.setOnCloseRequest(event ->
        {
            run.set(false);
            try
            {
                thread.join();
            }
            catch (Exception ex)
            {
                // Ignore, shutting down
            }
            plot.dispose();
        });
    }

    public static void main(String[] args)
    {
        launch(BasicPlotDemo.class, args);
    }
}