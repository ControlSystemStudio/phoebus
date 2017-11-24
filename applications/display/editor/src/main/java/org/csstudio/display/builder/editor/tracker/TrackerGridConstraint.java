/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tracker;


import org.csstudio.display.builder.model.DisplayModel;

import javafx.geometry.Point2D;


/** Constraint on the movement of the Tracker that snaps to a gird
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
public class TrackerGridConstraint extends TrackerConstraint
{
    private volatile DisplayModel model = null;

    public void configure(final DisplayModel model)
    {
        this.model = model;
    }

    @Override
    public Point2D constrain (final double x, final double y)
    {
        final DisplayModel copy = model;

        if (copy == null)
            return new Point2D(x, y);

        final int grid_x = copy.propGridStepX().getValue(), grid_y = copy.propGridStepY().getValue();

        return new Point2D(
            Math.floor(( x + grid_x / 2 ) / grid_x) * grid_x,
            Math.floor(( y + grid_y / 2 ) / grid_y) * grid_y
        );
    }

    public DisplayModel getModel()
    {
        return model;
    }
}
