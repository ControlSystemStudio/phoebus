/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.javafx.rtplot.data.InstrumentedReadWriteLock;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.data.SimpleDataItem;
import org.csstudio.javafx.rtplot.internal.MouseMode;
import org.csstudio.javafx.rtplot.util.RGBFactory;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link RTValuePlot}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ValuePlotDemo extends ApplicationWrapper
{
    final private static int MAX_SIZE = 10000;
    final private static boolean USE_LOG = false;

    static class DemoData implements PlotDataProvider<Double>
    {
        final private ReadWriteLock lock = new InstrumentedReadWriteLock();
        final private List<PlotDataItem<Double>> data = new ArrayList<>();
        private int calls = 0;

        @Override
        public Lock getLock()
        {
            return lock.readLock();
        }

        @Override
        public int size()
        {
            return data.size();
        }

        @Override
        public PlotDataItem<Double> get(final int index)
        {
            return data.get(index);
        }

        public void update()
        {
            lock.writeLock().lock();
            try
            {
                data.clear();

                if (USE_LOG)
                {
                    int i;
                    for (i=0; i<MAX_SIZE/2; ++i)
                    {
                        final double value = MAX_SIZE/2-i;
                        data.add(new SimpleDataItem<>(Double.valueOf(i), value));
                    }
                    for (/* */; i<MAX_SIZE; ++i)
                    {
                        final double value = i-MAX_SIZE/2;
                        data.add(new SimpleDataItem<>(Double.valueOf(i), value));
                    }
                }
                else
                {
                    final double amp = 10.0*Math.cos(2*Math.PI * (++calls) / 1000.0);
                    for (int i=0; i<MAX_SIZE; ++i)
                    {
                        final double value = amp*(Math.sin(2*Math.PI * i / (MAX_SIZE/3)) + Math.random()*0.1);
                        data.add(new SimpleDataItem<>(Double.valueOf(i), value));
                    }
                }
            }
            finally
            {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void start(final Stage stage) throws Exception
    {
        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.WARNING);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(logger.getLevel());


        final RTValuePlot plot = new RTValuePlot(true);
        plot.getXAxis().setValueRange(0.0, Double.valueOf(MAX_SIZE));
        plot.getXAxis().setGridVisible(true);
        plot.getYAxes().get(0).setGridVisible(true);

        if (USE_LOG)
        {
            plot.getYAxes().get(0).setLogarithmic(true);
            plot.getYAxes().get(0).setValueRange(0.001, 20.0);
            plot.getYAxes().get(0).setAutoscale(true);
        }
        else
        {
            plot.getYAxes().get(0).setValueRange(12.0, -12.0);
            plot.getYAxes().get(0).setAutoscale(false);
            plot.getYAxes().get(0).setAutoscale(true);
        }


        plot.setUpdateThrottle(20, TimeUnit.MILLISECONDS);

        final RGBFactory colors = new RGBFactory();
        final DemoData data = new DemoData();
        plot.addTrace("Fred", "socks", data, colors.next(), TraceType.AREA, 3, LineStyle.SOLID, PointType.NONE, 0, 0);

        final AtomicBoolean run = new AtomicBoolean(true);
        // Update data at 50Hz
        Thread update_data = new Thread(() ->
        {
            while (run.get())
            {
                data.update();
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

//        final Control menu_holder = plot.getPlotControl();
//        final MenuManager mm = new MenuManager();
//        mm.add(plot.getToolbarAction());
//        mm.add(plot.getLegendAction());
//        final Menu menu = mm.createContextMenu(menu_holder);
//        menu_holder.setMenu(menu);

        plot.setMouseMode(MouseMode.PAN);

        plot.addListener(new RTPlotListener<Double>()
        {
            @Override
            public void changedXAxis(Axis<Double> x_axis)
            {
                System.out.println("X Axis changed: " + x_axis);
            }
            @Override
            public void changedYAxis(YAxis<Double> y_axis)
            {
                System.out.println("Y Axis changed: " + y_axis);
            }
        });

        final Scene scene = new Scene(plot, 600, 700);
        stage.setScene(scene);
        stage.setTitle("ValuePlot Demo");
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
        ApplicationWrapper.launch(ValuePlotDemo.class, args);
    }
}
