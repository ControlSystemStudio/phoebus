/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

/** Dual {@link BufferUtil}s for double-buffering
 *  @author Kay Kasemir
 */
public class DoubleBuffer
{
    private int next = 0;
    final private BufferUtil buffers[] = new BufferUtil[] { null, null };

    /** Obtain {@link BufferUtil}
     *
     *  <p>Can be called from any thread,
     *  but must be called from the _same_ thread every time,
     *  for example always the `RTPlotUpdateThrottle`
     *  or always the UI thread.
     *
     *  <p>Will always create the buffered image on the UI thread.
     *
     *  @param width Width
     *  @param height Height
     *  @return {@link BufferUtil} or <code>null</code> if interrupted, error
     */
    public BufferUtil getBufferedImage(final int width, final int height)
    {
        final int index;
        BufferUtil buffer;
        // Atomically switch to 'other' buffer
        synchronized (this)
        {
            index = next;
            buffer = buffers[index];
            next = 1 - next;
        }
        if (buffer != null)
        {   // Have suitable buffer?
            if (buffer.getImage().getWidth()  == width  &&
                buffer.getImage().getHeight() == height)
            return buffer;
            // Wrong size, delete, then create new
            buffers[index].dispose();
        }
        // Create new buffer (had null, or was deleted)
        buffer = BufferUtil.getBufferedImage(width, height);
        buffers[index] = buffer;
        return buffer;
    }
}
