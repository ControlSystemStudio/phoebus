/*******************************************************************************
 * Copyright (c) 2014-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;

/** Helper for creating a BufferedImage and Graphics Context
 *
 *  <p>{@link BufferedImage}s are used to prepare display content
 *  in background threads.
 *  At least on Mac OS X, <code>BufferedImage.createGraphics()</code>
 *  has deadlocked when the UI thread is concurrently creating a buffered
 *  image.
 *  This helper allows background threads to create a buffered image on the UI thread.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BufferUtil
{
    private static final Logger logger = Logger.getLogger(BufferUtil.class.getName());
    final private BufferedImage image;
    final private Graphics2D gc;

    /** Obtain buffered image and GC
     *
     *  <p>Can be called from any thread, will
     *  always create the buffered image on the UI thread.
     *
     *  @param width Width
     *  @param height Height
     *  @return {@link BufferUtil} or <code>null</code> if interrupted, error
     */
    public static BufferUtil getBufferedImage(final int width, final int height)
    {
        if (Platform.isFxApplicationThread())
            return new BufferUtil(width, height);

        final CompletableFuture<BufferUtil> result = new CompletableFuture<>();
        Platform.runLater(() ->
        {
            result.complete(new BufferUtil(width, height));
        });

        try
        {
            // Simulate frequent timeout
            // if (Math.random() > 0.5) throw new TimeoutException("Test");
            return result.get(500, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException ex)
        {
            return null;
        }
        catch (TimeoutException ex)
        {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Skipping image update", ex);
            else
                logger.log(Level.WARNING, "Skipping image update");
            return null;
        }
        catch (Exception ex)
        {
            throw new Error("Cannot create BufferedImage", ex);
        }
    }

    private BufferUtil(final int width, final int height)
    {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        gc = image.createGraphics();
    }

    public BufferedImage getImage()
    {
        return image;
    }

    public Graphics2D getGraphics()
    {
        return gc;
    }

    public void dispose()
    {
        gc.dispose();
    }
}
