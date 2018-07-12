/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.util;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.ScrollPane;

/** Helper for JavaFX geometry handling
 *  @author Kay Kasemir
 */
public class JFXGeometryTools
{
    // JFXGeometryTools is specific to JavaFX.
    // Add to GeometryTools for operations on Widgets in the model.

    /** Get origin of a ScrollPane
     *
     *  <p>If the content is smaller than the scroll pane,
     *  the upper left corner of the scroll pane will show point (0, 0)
     *  of the content.
     *
     *  <p>As the content gets larger than the scroll pane,
     *  scroll bars allow panning and this method
     *  then determines which (x, y) of the content is displayed
     *  in the upper left corner of the scroll pane.
     *
     *  @param scroll_pane {@link ScrollPane}
     *  @return Coordinates content in upper left corner
     */
    public static Point2D getContentOrigin(final ScrollPane scroll_pane)
    {
        final Bounds viewport = scroll_pane.getViewportBounds();
        final Bounds content = scroll_pane.getContent().getBoundsInLocal();
        // Tried contentgetWidth() but note that content.getMinX() may be < 0.
        // Using content.getMaxX() works in all cases.
        final double x = (content.getMaxX() - viewport.getWidth()) * scroll_pane.getHvalue();
        final double y = (content.getMaxY() - viewport.getHeight()) * scroll_pane.getVvalue();
        return new Point2D(x, y);
    }
}
