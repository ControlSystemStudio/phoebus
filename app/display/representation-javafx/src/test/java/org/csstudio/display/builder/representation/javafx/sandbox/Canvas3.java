/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/** Uses background-prepared image
 *
 *  <p>Linux: Drawing ~ 90ms, Update ~0ms.
 *  Mac OS X: Drawing ~ 70ms, Update ~0ms.
 *  --> Really drawing in background, with fast UI update.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Canvas3 extends ApplicationWrapper
{
    private final int IMAGE_WIDTH = 400;
    private final int IMAGE_HEIGHT = 400;
    private final Canvas canvas =  DemoHelper.createCanvas();
    // Need 2 images: One that is currently shown,
    // one that is updated with next content.
    // When drawing into the one shown in the canvas,
    // canvas showed some of the 'new' and some of the 'old'
    // image content.
    // Reason could be that gc.drawImage() merely schedules the image
    // to be drawn, which could happen a little later while the content
    // of the image is again updated.
    // Toggling between two images would still be vulnerable,
    // but haven't seen that in practice.
    private final WritableImage[] image =
    {
        new WritableImage(IMAGE_WIDTH, IMAGE_HEIGHT),
        new WritableImage(IMAGE_WIDTH, IMAGE_HEIGHT)
    };
    private final AtomicLong counter = new AtomicLong();
    private final Text updates = new Text("0");

    private volatile long draw_ms = -1;
    private volatile long update_ms = -1;

    public static void main(final String[] args)
    {
        launch(Canvas3.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Label label1 = new Label("Canvas:");
        final Label label2 = new Label("Updates:");
        final VBox root = new VBox(label1, canvas, label2, updates);

        final Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Background-drawn Image");

        stage.show();

        final Thread thread = new Thread(this::thread_main);
        thread.setName("Redraw");
        thread.setDaemon(true);
        thread.start();
    }

    private void thread_main()
    {
        final Semaphore done = new Semaphore(0);
        try
        {
            final double lambda = 50.0;
            int to_update = 0;
            while (true)
            {
                // Draw image off-screen
                long start = System.currentTimeMillis();
                final WritableImage active = image[to_update];
                // PixelWriter is very limited, but might be just what's needed
                // for a display.builder "IntensityGraph".
                final PixelWriter writer = active.getPixelWriter();
                final double phase = System.currentTimeMillis() / 2000.0;
                // Different regions of image could be prepared on parallel threads
                for (int x=0; x<IMAGE_WIDTH; ++x)
                {
                    final int dx = (x - IMAGE_WIDTH/2);
                    final long dx2 = dx * dx;
                    for (int y=0; y<IMAGE_HEIGHT; ++y)
                    {
                        final long dy = (y - IMAGE_HEIGHT/2);
                        final double dist = Math.sqrt(dx2 + dy*dy);
                        final double level = 0.5 + 0.5*Math.cos(2*Math.PI * (dist/lambda - phase));
                        // final Color c = Color.hsb(level*360.0, 1.0, 1.0);
                        final Color c = Color.gray(level);
                        writer.setColor(x, y, c );
                    }
                }
                long ms = System.currentTimeMillis() - start;
                if (draw_ms < 0)
                    draw_ms = ms;
                else
                    draw_ms = (draw_ms * 9 + ms) / 10;
                counter.incrementAndGet();

                // Show image in canvas on UI thread
                start = System.currentTimeMillis();
                Platform.runLater(() ->
                {
                    final GraphicsContext gc = canvas.getGraphicsContext2D();
                    gc.drawImage(active, 0, 0, canvas.getWidth(), canvas.getHeight());
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

                to_update = 1 - to_update;
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
