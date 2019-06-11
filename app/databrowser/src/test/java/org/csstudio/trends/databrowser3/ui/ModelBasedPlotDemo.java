/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Demo of the {@link ModelBasedPlot}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelBasedPlotDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        // Standalone item, not in Model
        final PVItem item = new PVItem("sim://sine(-10, 10, 0.2)", 0.0);
        item.setColor(Color.BLUE);
        item.start();

        // Plot for just that item
        final ModelBasedPlot plot = new ModelBasedPlot(true);
        plot.addTrace(item);

        AxisConfig axis = new AxisConfig(true, "Values", true, true, false, Color.DARKRED, -10, 10, false, false, false);
        plot.updateAxis(0, axis );

        final BorderPane layout = new BorderPane(plot.getPlot());
        final Scene scene = new Scene(layout, 800, 600);
        stage.setScene(scene);
        stage.show();

        // Instead of full Controller, simple update task
        final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        final Runnable fake_controller = () ->
        {
            Platform.runLater(() -> plot.redrawTraces());
        };
        timer.scheduleAtFixedRate(fake_controller, 500, 500, TimeUnit.MILLISECONDS);
    }

    public static void main(final String[] args)
    {
        launch(ModelBasedPlotDemo.class, args);
    }
}
