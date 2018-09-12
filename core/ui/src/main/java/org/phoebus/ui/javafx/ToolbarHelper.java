/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.util.concurrent.ForkJoinPool;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Helper for Toolbar
 *  @author Kay Kasemir
 */
public class ToolbarHelper
{
    // See http://www.oracle.com/technetwork/articles/java/javafxbest2-1634274.html

    /** @return 'Spring', spacer that fills available space */
    public static Node createSpring()
    {
        final Region sep = new Region();
        HBox.setHgrow(sep, Priority.ALWAYS);
        return sep;
    }

    /** @return 'Strut', a small fixed-width spacer */
    public static Node createStrut()
    {
        return createStrut(10);
    }

    /** @param width Desired width
     *  @return 'Strut' that occupies requested width
     */
    public static Node createStrut(int width)
    {
        final Region sep = new Region();
        sep.setPrefWidth(width);
        sep.setMinWidth(Region.USE_PREF_SIZE);
        sep.setMaxWidth(Region.USE_PREF_SIZE);
        return sep;
    }

    /** Hack ill-sized toolbar buttons
     *
     *  <p>When toolbar is originally hidden and then later
     *  shown, it tends to be garbled, all icons in pile at left end,
     *  Manual fix is to hide and show again.
     *  Workaround is to force another layout a little later.
     */
    public static void refreshHack(final ToolBar toolbar)
    {
        if (toolbar.getParent() == null)
            return;
        for (Node node : toolbar.getItems())
        {
            if (! (node instanceof ButtonBase))
                continue;
            final ButtonBase button = (ButtonBase) node;
            final Node icon = button.getGraphic();
            if (icon == null)
                continue;
            // Re-set the icon to force new layout of button
            button.setGraphic(null);
            button.setGraphic(icon);
            if (button.getWidth() == 0  ||  button.getHeight() == 0)
            {   // If button has no size, yet, try again later
                ForkJoinPool.commonPool().submit(() ->
                {
                    Thread.sleep(500);
                    Platform.runLater(() -> refreshHack(toolbar));
                    return null;
                });
                return;
            }
        }
        Platform.runLater(() -> toolbar.layout());
    }
}
