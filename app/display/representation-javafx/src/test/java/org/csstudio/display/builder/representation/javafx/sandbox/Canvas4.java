/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.awt.image.BufferedImage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/** Draw AWT image in background, then display in Canvas.
 *
 *  <p>On Linux,
 *  with 10000 circles, drawing takes ~36ms, the canvas update takes ~2ms.
 *
 *  <p>On Mac OS X,
 *  with 10000 circles, drawing takes ~30ms, the canvas update takes ~1ms.
 *  With 50000 circles, drawing takes ~150ms, the canvas update takes ~1ms.
 *
 *  --> AWT drawing off the UI thread, with fast update of Canvas.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Canvas4 extends ApplicationWrapper
{
    final private Canvas canvas = DemoHelper.createCanvas();
    private final AtomicLong counter = new AtomicLong();
    private final Text updates = new Text("0");

    private volatile long draw_ms = -1;
    private volatile long update_ms = -1;

    public static void main(final String[] args)
    {
        launch(Canvas4.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Label label1 = new Label("Canvas:");
        final Label label2 = new Label("Updates:");
        final VBox root = new VBox(label1, canvas, label2, updates);

        final Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Drawing AWT Image");

        stage.show();

        final Thread thread = new Thread(this::thread_main);
        thread.setName("Redraw");
        thread.setDaemon(true);
        thread.start();
    }

    private void thread_main()
    {
        final Semaphore done = new Semaphore(0);
        int to_refresh = 1;
        try
        {
            final BufferedImage buf = new BufferedImage((int)canvas.getWidth(), (int)canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
            final WritableImage image = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
            while (true)
            {
                // Prepare AWT image
                long start = System.currentTimeMillis();
                DemoHelper.refresh(buf);
                long ms = System.currentTimeMillis() - start;
                if (draw_ms < 0)
                    draw_ms = ms;
                else
                    draw_ms = (draw_ms * 9 + ms) / 10;

                counter.incrementAndGet();

                // Draw into Canvas on UI thread
                start = System.currentTimeMillis();
                Platform.runLater(() ->
                {
                	SwingFXUtils.toFXImage(buf, image);
                    canvas.getGraphicsContext2D().drawImage(image, 0, 0);
                    updates.setText(Long.toString(counter.get()));
                    done.release();
                });

                // Wait for UI thread
                done.acquire();
                ms = System.currentTimeMillis() - start;
                if (update_ms < 0)
                    update_ms = ms;
                else
                    update_ms = (update_ms * 9 + ms) / 10;

                to_refresh = 1 - to_refresh;
                Thread.sleep(20);

                if ((counter.get() % 50) == 0)
                {
                    System.out.println("Drawing: " + draw_ms + " ms");
                    System.out.println("Update : " + update_ms + " ms");
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
