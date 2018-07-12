/*******************************************************************************
 * Copyright (c) 2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

/** Interpolation from image to screen pixels
 *  @author Kay Kasemir
 */
public enum Interpolation
{
    /** No interpolation */
    NONE,

    /** Interpolate */
    INTERPOLATE,

    /** Automatically enable/disable interpolation */
    AUTOMATIC;
}
