/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Helper for drawing random canvas content
 *  @author Kay Kasemir
 */
public class DemoHelper
{
    private static final int ITEMS = 10000;

    public static Canvas createCanvas()
    {
        return new Canvas(800, 600);
    }

    public static void refresh(final Canvas canvas)
    {
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        final Bounds bounds = canvas.getBoundsInLocal();
        final double width = bounds.getWidth();
        final double height = bounds.getHeight();

        gc.clearRect(0, 0, width, height);
        gc.strokeRect(0, 0, width, height);

        for (int i=0; i<ITEMS; ++i)
        {
            gc.setFill(Color.hsb(Math.random()*360.0,
                                 Math.random(),
                                 Math.random()));
            final double size = 5 + Math.random() * 40;
            final double x = Math.random() * (width-size);
            final double y = Math.random() * (height-size);
            gc.fillOval(x, y, size, size);
        }
    }

    public static void refresh(final BufferedImage buf)
    {
        Graphics2D gc = buf.createGraphics();
        final int width = buf.getWidth();
        final int height = buf.getHeight();

        gc.clearRect(0, 0, width, height);
        gc.drawRect(0, 0, width, height);

        for (int i=0; i<ITEMS; ++i)
        {
            gc.setColor(java.awt.Color.getHSBColor((float)Math.random(),
                                                   (float)Math.random(),
                                                   (float)Math.random()));
            final int size = (int)(5 + Math.random() * 40);
            final int x = (int)(Math.random() * (width-size));
            final int y = (int)(Math.random() * (height-size));
            gc.fillOval(x, y, size, size);
        }
    }
}
