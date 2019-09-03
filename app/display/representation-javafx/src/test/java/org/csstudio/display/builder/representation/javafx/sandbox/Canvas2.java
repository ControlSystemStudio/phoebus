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
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/** Uses 2 canvas widgets,
 *  updating one in background, then swapping them.
 *
 *  <p>On Linux,
 *  with 1000 circles, drawing takes ~0ms, the canvas swap takes ~11ms.
 *  With 10000 circles, drawing takes ~2ms, the canvas swap takes ~250ms.
 *  with 20000 circles, drawing takes ~5ms, the canvas swap takes ~512ms.
 *  -->
 *  The actual drawing of the canvas takes place when it's added to
 *  the scene graph. The "background" drawing doesn't help.
 *  Matches the comment "Rendering a canvas on a background thread is very slow when updating the scene graph"
 *  from https://community.oracle.com/thread/3755802
 *
 *  <p>On Mac OS X,
 *  with 10000 circles, drawing takes ~2ms, the canvas swap takes ~0ms.
 *  With 50000 circles, drawing takes ~10ms, the canvas swap takes ~0ms.
 *  --> Faster.
 *  Time to draw increases with number of elements, so seems to perform
 *  the drawing off the UI thread, with constant time for canvas swap.
 *  Faster than AWT drawing (Canvas4 example).
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Canvas2 extends ApplicationWrapper
{
    final private Canvas[] canvas = new Canvas[]
    {
        DemoHelper.createCanvas(),
        DemoHelper.createCanvas()
    };
    private final StackPane canvas_stack = new StackPane(canvas[0]);
    private final AtomicLong counter = new AtomicLong();
    private final Text updates = new Text("0");

    private volatile long draw_ms = -1;
    private volatile long update_ms = -1;

    public static void main(final String[] args)
    {
        launch(Canvas2.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Label label1 = new Label("Canvas:");
        final Label label2 = new Label("Updates:");
        final VBox root = new VBox(label1, canvas_stack, label2, updates);

        final Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Swapping two Canvases");

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
            while (true)
            {
                final Canvas prepared = canvas[to_refresh];

                // Prepare canvas off-screen
                long start = System.currentTimeMillis();
                DemoHelper.refresh(prepared);
                long ms = System.currentTimeMillis() - start;
                if (draw_ms < 0)
                    draw_ms = ms;
                else
                    draw_ms = (draw_ms * 9 + ms) / 10;

                counter.incrementAndGet();

                // Swap on UI thread
                start = System.currentTimeMillis();
                Platform.runLater(() ->
                {
                    canvas_stack.getChildren().setAll(prepared);
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
