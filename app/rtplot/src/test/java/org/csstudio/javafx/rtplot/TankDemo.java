/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TankDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        Logger.getLogger("").setLevel(Level.FINE);
        for (Handler handler : Logger.getLogger("").getHandlers())
            handler.setLevel(Level.FINE);

        final RTTank tank = new RTTank();

		final Pane root = new Pane(tank);
		tank.widthProperty().bind(root.widthProperty());
		tank.heightProperty().bind(root.heightProperty());

		tank.setFont(Font.font("Liberation Mono", 50.0));
		tank.setRange(0, 120);

        final Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Basic Tank Demo");
        stage.show();

        final AtomicBoolean run = new AtomicBoolean(true);
        // Update data at 50Hz
        final int PERIOD_MS = 10000;
        final Thread update_data = new Thread(() ->
        {
            while (run.get())
            {
                tank.setValue(100.0 * (System.currentTimeMillis() % PERIOD_MS) / PERIOD_MS);
                try
                {
                    Thread.sleep(1000/200);
                }
                catch (Exception e)
                {
                    // NOP
                }
            }
        }, "DemoDataUpdate");
        update_data.start();


        stage.setOnCloseRequest((event) ->
        {
            run.set(false);
            tank.dispose();
        });
    }

    public static void main(final String[] args)
    {
        launch(TankDemo.class, args);
    }
}