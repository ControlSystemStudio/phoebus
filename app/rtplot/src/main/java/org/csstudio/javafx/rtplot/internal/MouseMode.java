/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

/** How the plot reacts to mouse move/click
 *  @author Kay Kasemir
 */
public enum MouseMode
{
    /** Do not react to clicks */
    NONE,
    /** Allow panning, but currently idle */
    PAN,
    /** Allow zooming in, currently idle */
    ZOOM_IN,
    /** Zoom out where clicked */
    ZOOM_OUT,

    /** Modes from here on are used within the {@link Plot},
     *  they are sub-states of the above.
     */
    INTERNAL_MODES,

    /** Panning X axis */
    PAN_X,
    /** Panning Y axis */
    PAN_Y,
    /** Panning complete plot */
    PAN_PLOT,
    /** Zoom into X axis */
    ZOOM_IN_X,
    /** Zoom into Y axis */
    ZOOM_IN_Y,
    /** Zoom into the plot */
    ZOOM_IN_PLOT;
}