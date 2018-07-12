/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.util;

import java.time.Instant;

/** A transformation from model coordinates to display coordinates
 *
 *  @param <T> Data type, {@link Double} or {@link Instant}
 *
 *  @see LinearScreenTransform
 *  @see LogTransform
 *  @see TimeTransform
 *
 *  @author Kay Kasemir
 */
public interface ScreenTransform<T>
{
    /** Configure a transformation from x1..x2 into y1..y2
     *  <p>
     *  If the transformation is undefined (x1 == x2),
     *  or results in a==0 so that the 'inverse' won't work,
     *  the result is a 1:1 transformation. No error message.
     *
     *  @param x1 Start of 'source'
     *  @param x2 End of 'source'
     *  @param y1 Start of 'destination'
     *  @param y2 End of 'destination'
     */
    public abstract void config(T x1, T x2, double y1, double y2);

    /** @return Returns x transformed into the y range. */
    public abstract double transform(T x);

    /** @return Returns x transformed into the y range. */
    public abstract T inverse(double y);

    /** @return Copy of this transformation */
    public abstract ScreenTransform<T> copy();
}
