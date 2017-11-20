/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tracker;

import javafx.geometry.Point2D;

/** Constraint on the movement of the Tracker
 *  @author Kay Kasemir
 */
abstract public class TrackerConstraint
{
    private boolean enabled = false;

    /** @param enabled Enable this constraint? */
    public void setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
    }

    /** @return Enable this constraint? */
    public boolean isEnabled()
    {
        return enabled;
    }

    /** Constrain the movement of the tracker
     *
     *  <p>Called with an x/y coordinate, implementation
     *  may return a modified coordinate to restrict the valid
     *  coordinate space.
     *
     *  @param x Requested X position
     *  @param y Requested Y position
     *
     *  @return Constrained coordinate
     */
    abstract public Point2D constrain(double x, double y);
}
