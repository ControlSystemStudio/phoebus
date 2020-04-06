/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Activator;
import org.phoebus.ui.javafx.PlatformInfo;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Scene;

/** Cursor support for plots
 *
 *  <p>Maps {@link MouseMode}s to cursors.
 *
 *  <p>Also helps with SWT FXCanvas:
 *  The JFX node already supports <code>setCursor()</code>,
 *  But that has no affect when JFX is hosted
 *  inside an SWT FXCanvas.
 *  (https://bugs.openjdk.java.net/browse/JDK-8088147)
 *
 *  <p>We set the cursor of the _scene_, and monitor
 *  the scene's cursor in the RCP code that creates the
 *  FXCanvas to then update the SWT cursor.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotCursors
{
    private static Cursor cursor_pan, cursor_zoom_in, cursor_zoom_out;

    static
    {
        try
        {
            // cursor_cross = new ImageCursor(Activator.getIcon("cursor_cross"), 17, 17);
            cursor_pan = new ImageCursor(Activator.getIcon("cursor_pan"), 8, 8);
            cursor_zoom_in = new ImageCursor(Activator.getIcon("cursor_zoom_in"), 5, 5);
            cursor_zoom_out = new ImageCursor(Activator.getIcon("cursor_zoom_out"), 5, 5);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error loading cursors", ex);
            // cursor_cross = Cursor.CROSSHAIR;
            cursor_pan = Cursor.HAND;
            cursor_zoom_in = Cursor.DEFAULT;
            cursor_zoom_out = Cursor.DEFAULT;
        }
    }

    /** Set cursor.
     *
     *  @param node {@link Node} on which to set the cursor
     *  @param mode {@link MouseMode}
     */
    public static void setCursor(final Node node, final MouseMode mode)
    {
        final Cursor cursor = getCursor(mode);
        setCursor(node, cursor);
    }

    /** Set cursor.
     *
     *
     *  @param node {@link Node} on which to set the cursor
     *  @param cursor {@link Cursor}
     */
    public static void setCursor(final Node node, Cursor cursor)
    {
        final Cursor current = node.getCursor();
        if (cursor == current)
            return;

        // On Mac OS X, custom cursors turn into random noise after some time.
        // Details unclear, but creating a new ImageCursor seems to
        // avoid the issue.
        if (PlatformInfo.is_mac_os_x  &&  cursor instanceof ImageCursor)
        {
            final ImageCursor orig = (ImageCursor) cursor;
            cursor = new ImageCursor(orig.getImage(), orig.getHotspotX(), orig.getHotspotY());
        }

        node.setCursor(cursor);
        final Scene scene = node.getScene();
        if (scene != null)
            scene.setCursor(cursor);
    }

    private static Cursor getCursor(final MouseMode mode)
    {
        switch (mode)
        {
        case PAN:
        case PAN_X:
        case PAN_Y:
        case PAN_PLOT:
            return cursor_pan;
        case ZOOM_IN:
        case ZOOM_IN_PLOT:
        case ZOOM_IN_X:
        case ZOOM_IN_Y:
            return cursor_zoom_in;
        case ZOOM_OUT:
            return cursor_zoom_out;
        default:
             return Cursor.DEFAULT;
        }
    }
}


