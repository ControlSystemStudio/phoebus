/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/** Screen helper
 *  @author Kay Kasemir
 */
public class ScreenUtil
{
    /** @param x Screen coordinates
     *  @param y Screen coordinates
     *  @return Bounds of screen that contains the point, or <code>null</code> if no screen found
     */
    public static Rectangle2D getScreenBounds(final double x, final double y)
    {
        for (Screen screen : Screen.getScreens())
        {
            final Rectangle2D check = screen.getVisualBounds();
            // contains(x, y) would include the right edge
            if (x >= check.getMinX()  &&  x <  check.getMaxX()  &&
                y >= check.getMinY()  &&  y <  check.getMaxY())
                return check;
        }
        return null;
    }
}
