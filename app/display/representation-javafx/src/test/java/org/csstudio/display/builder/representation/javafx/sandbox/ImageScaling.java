/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Image scaling demo
 *
 *  <p>Works fine on Mac OS X.
 *  On Linux, image has wrap-around type artifacts
 *  on the top and left border.
 *
 *  <p>Replacing the Canvas with an ImageView
 *  and calling setFitWidth(800), view.setFitHeight(600)
 *  experiences the same problem.
 *
 *  @author Kay Kasemir
 */
public class ImageScaling extends ApplicationWrapper
{
    private static final int HEIGHT = 25, WIDTH = 155;

    public static void main(final String[] args)
    {
        launch(ImageScaling.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        // Image with red border
        final WritableImage image = new WritableImage(WIDTH, HEIGHT);
        final PixelWriter writer = image.getPixelWriter();
        for (int x=0; x<WIDTH; ++x)
        {
            writer.setColor(x, 0, Color.RED);
            writer.setColor(x, HEIGHT-1, Color.RED);
        }
        for (int y=0; y<HEIGHT; ++y)
        {
            writer.setColor(0, y, Color.RED);
            writer.setColor(WIDTH-1, y, Color.RED);
        }

        // Draw into canvas, scaling 'up'
        final Canvas canvas = new Canvas(800, 600);
        canvas.getGraphicsContext2D().drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight());

        final Scene scene = new Scene(new Group(canvas), canvas.getWidth(), canvas.getHeight());
        stage.setScene(scene);
        stage.show();
    }
}
