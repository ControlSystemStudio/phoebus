/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.util.concurrent.Semaphore;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/** Draws canvas on UI thread.
 *
 *  <p>Not practical for CPU-intensive drawing operations.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Canvas0 extends ApplicationWrapper
{
    private final Canvas canvas = DemoHelper.createCanvas();
    private final Text updates = new Text("0");
    final Semaphore done = new Semaphore(0);
    private long counter = 0;

    public static void main(final String[] args)
    {
        launch(Canvas0.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Label label1 = new Label("Canvas:");
        final Label label2 = new Label("Updates:");

        final VBox root = new VBox(label1, canvas, label2, updates);

        final Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Drawing Canvas on UI Thread");

        stage.show();

        final Thread thread = new Thread(this::thread_main);
        thread.setName("Redraw");
        thread.setDaemon(true);
        thread.start();
    }


    private void thread_main()
    {
        try
        {
            while (true)
            {
                Platform.runLater(this::update);
                // Wait for UI thread to perform updates
                done.acquire();
                Thread.sleep(100);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void update()
    {
        DemoHelper.refresh(canvas);
        ++counter;
        updates.setText(Long.toString(counter));
        done.release();
    }
}
