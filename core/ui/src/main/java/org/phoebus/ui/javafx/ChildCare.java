/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

/** Helper for handling parent/child lists
 *
 *  <p>Both {@link Group} and {@link Pane}
 *  have child nodes, but different API.
 *  This helps treating both the same way.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ChildCare
{
    /** Add a child node to a parent node in scene graph
     *
     *  @param parent {@link Parent}, must be a {@link Group} or {@link Pane} that allows adding child nodes
     *  @param child Child {@link Node} to add
     *  @throws IllegalArgumentException on error
     */
    public static void addChild(final Parent parent, final Node child)
    {
        if (parent instanceof Group)
            ((Group)parent).getChildren().add(child);
        else if (parent instanceof Pane)
            ((Pane)parent).getChildren().add(child);
        else
            throw new IllegalArgumentException("Cannot add " + child + " to " + parent);
    }

    /** Remove a child node from a parent node in scene graph
     *
     *  @param parent {@link Parent}, must be a {@link Group} or {@link Pane} that allows removing child nodes
     *  @param child Child {@link Node} to remove
     *  @return <code>true</code> if child node was found and removed, <code>false</code> if child wasn't held by parent
     *  @throws IllegalArgumentException on error
     */
    public static boolean removeChild(final Parent parent, final Node child)
    {
        if (parent instanceof Group)
            return ((Group)parent).getChildren().remove(child);
        else if (parent instanceof Pane)
            return ((Pane)parent).getChildren().remove(child);
        else
            throw new IllegalArgumentException("Cannot remove " + child + " from " + parent);
    }

}
