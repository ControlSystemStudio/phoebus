/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.representation.FontCalibration;

/** Java AWT calibration
 *
 *  <p>Can also be executed as demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AWTFontCalibration implements Runnable, FontCalibration
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    final Font font = new Font(FontCalibration.FONT, Font.PLAIN, FontCalibration.SIZE);
	private int text_width, text_height;

    @Override
    public double getCalibrationFactor()
    {
        final BufferedImage buf = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        final Graphics2D gc = buf.createGraphics();
        gc.setFont(font);
        final FontMetrics metrics = gc.getFontMetrics();
        text_width = metrics.stringWidth(FontCalibration.TEXT);
        text_height = metrics.getHeight();
        gc.dispose();

        logger.log(Level.FINE,
                   "Font calibration measure: " + text_width + " x " + text_height);
        final double factor = FontCalibration.PIXEL_WIDTH / text_width;
        logger.log(Level.CONFIG, "AWT font calibration factor: {0}", factor);
        return factor;
    }

    @Override
    public void run()
    {
        Logger.getLogger("").setLevel(Level.CONFIG);
        for (Handler handler : Logger.getLogger("").getHandlers())
            handler.setLevel(Level.CONFIG);

        final double factor = getCalibrationFactor();

        final Frame frame = new Frame("Java AWT: Calibration factor " + factor);
        frame.setSize(text_width, text_height);
        
        // Would like to use TextField or Label, but:
        // "Peered AWT components, such as Label and TextField,
        //  can only use logical fonts." (Javadoc for 'Font')
        // Sure enough at least on Windows the font family is
        // ignored, only the style and size are honored
        // by Label.setFont() or TextField.setFont()
        // --> Use canvas and draw the text with font.
        final Canvas text = new Canvas()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(final Graphics gc)
			{
				super.paint(gc);
				gc.setFont(font);
				final FontMetrics metrics = gc.getFontMetrics();
				
				// drawString x/y is 'baseline' of text
				final int y = metrics.getLeading() + metrics.getAscent();
				gc.drawString(FontCalibration.TEXT, 0, y);
				
				// Show baseline and 'leading'
				gc.setColor(Color.RED);
				gc.drawLine(0, y, text_width, y);
				gc.setColor(Color.GREEN);
				gc.drawLine(0, metrics.getLeading(), text_width, metrics.getLeading());
			}
		};
        text.setSize(text_width, text_height);
        
        frame.add(text);
        
        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent windowEvent)
            {
                System.exit(0);
            }
        });

        frame.pack();
        frame.setVisible(true);

        if (Math.abs(factor - 1.0) > 0.01)
            System.err.println("Calibration is not 1.0 but " + factor);
    }

    public static void main(final String[] args)
    {
        new AWTFontCalibration().run();
    }
}