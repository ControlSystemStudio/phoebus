/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/** @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MeterDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        Logger.getLogger("").setLevel(Level.FINE);
        for (Handler handler : Logger.getLogger("").getHandlers())
            handler.setLevel(Level.FINE);

        final RTMeter meter = new RTMeter();

        final Pane root = new Pane(meter);
		final ChangeListener<? super Number> resize_listener = (p, o, n) -> meter.setSize(root.getWidth(), root.getHeight());
        root.widthProperty().addListener(resize_listener);
        root.heightProperty().addListener(resize_listener);

        final Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Meter Demo");
        stage.show();

        // Thread that periodically updates meter
        final AtomicBoolean run = new AtomicBoolean(true);
        final Runnable updates = () ->
        {
            try
            {
                while (run.get())
                {
                    TimeUnit.SECONDS.sleep(2);
                    meter.setValue(10.0*Math.random(), "");
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        };
        final Thread thread = new Thread(updates);
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
            meter.dispose();
        });
    }

    public static void main(String[] args)
    {
        launch(MeterDemo.class, args);
    }
}