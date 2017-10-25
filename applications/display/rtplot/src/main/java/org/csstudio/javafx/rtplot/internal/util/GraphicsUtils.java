/*******************************************************************************
 * Copyright (c) 2010-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.util;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import javafx.geometry.Rectangle2D;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/** Utility methods for drawing graphics
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class GraphicsUtils
{
    /** Convert color
     *  @param color JFX Color
     *  @return AWT Color
     */
    public static java.awt.Color convert(final javafx.scene.paint.Color color)
    {
        return new java.awt.Color((int) (color.getRed()*255),
                                  (int) (color.getGreen()*255),
                                  (int) (color.getBlue()*255));
    }

    /** Convert color
     *  @param color JFX Color
     *  @param alpha Alpha, 0 - 255
     *  @return AWT Color
     */
    public static java.awt.Color convert(final javafx.scene.paint.Color color, final int alpha)
    {
        return new java.awt.Color((int) (color.getRed()*255),
                                  (int) (color.getGreen()*255),
                                  (int) (color.getBlue()*255),
                                  alpha);
    }

    /** Convert font
     *  @param font JFX font
     *  @return AWT font
     */
    public static java.awt.Font convert(final Font font)
	{
		// JFX font is constructed with FontWeight and FontPosture constants,
		// but can only get a string back out..
		final String style_spec = font.getStyle().toLowerCase();
		int style = java.awt.Font.PLAIN;
		if (style_spec.contains("italic"))
			style |= java.awt.Font.ITALIC;
		if (style_spec.contains("bold"))
			style |= java.awt.Font.BOLD;
		return new java.awt.Font(font.getFamily(), style, (int)font.getSize());
	}

    /** Convert font
     *  @param font AWT font
     *  @return JFX font
     */
    public static Font convert(final java.awt.Font font)
    {
        final FontWeight weight = font.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
        final FontPosture posture = font.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        return Font.font(font.getFamily(), weight, posture, font.getSize());
    }

    /** Convert rectangle
     *  @param rect AWT rectangle
     *  @return JFX rectangle
     */
    public static Rectangle2D convert(final Rectangle rect)
    {
        return new Rectangle2D(rect.x, rect.y, rect.width, rect.height);
    }

    /** Measure text
     *  @param gc AWT Graphics context. Font must be set.
     *  @param text Text to measure, may be multi-line.
     *  @return Rectangle where (x, y) is the offset from the top-left
     *          corner of the text to its baseline as used for gc.drawString(),
     *          and (width, height) are the overall bounding box.
     */
    public static Rectangle measureText(final Graphics2D gc, final String text)
    {
    	final FontMetrics metrics = gc.getFontMetrics();
    	final Rectangle info = new Rectangle(0, metrics.getLeading() + metrics.getAscent(), 0, 0);
        for (String line : text.split("\n"))
        {
            final int width = metrics.stringWidth(line);
            if (width > info.width)
                info.width = width;
            info.height += metrics.getHeight();
        }
    	return info;
    }

    /** Draw multi-line text
     *  @param gc AWT Graphics context. Font must be set.
     *  @param x X position of text (left edge)
     *  @param y Y position of text's baseline
     *  @param text Text to draw, may contain '\n'
     */
    public static void drawMultilineText(final Graphics2D gc, final int x, int y, final String text)
    {
        final int line_height = gc.getFontMetrics().getHeight();
        for (String line : text.split("\n"))
        {
            gc.drawString(line, x, y);
            y += line_height;
        }
    }

    /** Draws text vertically (rotates plus or minus 90 degrees). Uses the
     *  current font, color, and background.
     *  @param gc the GC on which to draw the text
     *  @param x the x coordinate of the top left corner of the drawing rectangle
     *  @param y the y coordinate of the top left corner of the drawing rectangle
     *  @param string the text to draw
     *  @param up Draw 'up' or 'down'?
     */
    public static void drawVerticalText(final Graphics2D gc, final int x, final int y,
    									final String string, final boolean up)
    {

    	final AffineTransform transform = gc.getTransform();
    	final Rectangle metrics = measureText(gc, string);
        // Debug: Outline of text
    	// gc.drawRect(x, y, metrics.height, metrics.width);
    	if (up)
    	{
	    	gc.translate(x, y);
	    	gc.rotate(-Math.PI/2);
            gc.drawString(string, -metrics.width, metrics.y);
    	}
    	else
    	{
	    	gc.translate(x, y);
	    	gc.rotate(Math.PI/2);
	        gc.drawString(string, 0, metrics.y - metrics.height);
    	}
    	gc.setTransform(transform);
    }
}
