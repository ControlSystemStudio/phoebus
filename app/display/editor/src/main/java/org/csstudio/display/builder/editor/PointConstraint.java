/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import javafx.geometry.Point2D;

/** Point constraint
 *
 *  <p>Called with an x/y coordinate, implementation
 *  may return a modified coordinate to restrict the valid
 *  coordinate space.
 *
 * @author Kay Kasemir
 */
public interface PointConstraint
{
    /** Constrain a point to certain coordinates, for example on a grid
     *  @param x Original X
     *  @param y Original Y
     *  @return Constrained coordinate
     */
    public Point2D constrain(double x, double y);
}
