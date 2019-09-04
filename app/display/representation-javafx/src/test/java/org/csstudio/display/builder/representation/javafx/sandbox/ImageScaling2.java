/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;

/** Image scaling demo using AWT to perform the scaling
 *
 *  <p>Works fine on Linux and Mac OS X.
 *
 *  @author Kay Kasemir
 */
public class ImageScaling2 extends ApplicationWrapper
{
    private static final int WIDTH = 800, HEIGHT = 600;
    private static final int IMAGE_WIDTH = 155, IMAGE_HEIGHT = 25;

    public static void main(final String[] args)
    {
        launch(ImageScaling2.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        // AWT Image with red border
        final BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D gc = image.createGraphics();
        gc.setColor(Color.WHITE);
        gc.fillRect(0, 0, IMAGE_WIDTH-1, IMAGE_HEIGHT-1);
        gc.setColor(Color.RED);
        gc.drawRect(0, 0, IMAGE_WIDTH-1, IMAGE_HEIGHT-1);

        // Scale image
        final BufferedImage scaled = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        gc = scaled.createGraphics();
        gc.drawImage(image, 0, 0, WIDTH, HEIGHT, null);

        // Draw scaled image into canvas
        final Canvas canvas = new Canvas(WIDTH, HEIGHT);
        canvas.getGraphicsContext2D().drawImage(SwingFXUtils.toFXImage(scaled, null), 0, 0);

        final Scene scene = new Scene(new Group(canvas), WIDTH, HEIGHT);
        stage.setScene(scene);
        stage.show();
    }
}
