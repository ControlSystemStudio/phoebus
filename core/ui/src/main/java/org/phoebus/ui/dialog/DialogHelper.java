/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Dialog;

/** Helper for dialogs
 *  @author Kay Kasemir
 */
public class DialogHelper
{
    /** Get outermost container of a node
     *  @param node JFX Node
     *  @return Outermost container
     */
    public static Node getContainer(final Node node)
    {
        Node container = node;
        while (container.getParent() != null)
            container = container.getParent();
        return container;
    }

    /** Position dialog relative to another widget
     *
     *  <p>By default, dialogs seem to pop up in the center of the first monitor.
     *  .. even if the "current" window is on a different monitor.
     *
     *  <p>This helper positions the dialog relative to the center
     *  of a node.
     *
     *  @param dialog Dialog to position
     *  @param node Node relative to which dialog should be positioned
     *  @param x_offset Offset relative to center of the node
     *  @param y_offset Offset relative to center of the node
     */
    public static void positionDialog(final Dialog<?> dialog, final Node node, final int x_offset, final int y_offset)
    {
        final Bounds pos = node.localToScreen(node.getBoundsInLocal());
        dialog.setX(pos.getMinX() + pos.getWidth()/2 + x_offset);
        dialog.setY(pos.getMinY() + pos.getHeight()/2 + y_offset);
    }
}
