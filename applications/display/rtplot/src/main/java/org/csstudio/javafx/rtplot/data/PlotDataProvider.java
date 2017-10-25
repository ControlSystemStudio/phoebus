/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/** A series of samples. Basically one 'line' in the plot.
 *  <p>
 *  Random access to samples is an important design criterion
 *  of the plot library.
 *  User code needn't copy its data into a plot-specific array,
 *  it only has to provide sample-by-sample access.
 *
 *  <p>The plot library iterates over the sample sequence
 *  to update annotations, auto-scale, and to redraw the plot.
 *  Concurrently, the application might receive new or
 *  changed samples.
 *  All users of the sample sequence thus need to lock.
 *
 *  <p>Note that the <code>size()</code> and <code>get()</code>
 *  themselves might not lock. Practical use of the data requires
 *  accessing several elements, and the reader needs to maintain
 *  the lock for the complete operation. Locking on each <code>get()</code>
 *  will not assert consistency but only increase CPU use for
 *  needless lock/unlocks.
 *
 *  <p>To improve performance, the data provider implementation
 *  might internally use a {@link ReadWriteLock} for its write access to the data,
 *  and only pass the 'read' component to the plotting library.
 *
 *  <p>It is suggested to actually use the {@link InstrumentedReadWriteLock}
 *  and include its <code>toString()</code> info in the <code>PlotDataProvider#toString()</code>
 *  to show the owning thread etc. when debugging deadlocks.
 *
 *  @param <XTYPE> Data type used for the horizontal {@link Axis}
 *
 *  @author Kay Kasemir
 */
public interface PlotDataProvider<XTYPE extends Comparable<XTYPE>>
{
    /** @return {@link Lock} for read access */
    public Lock getLock();

    /** @return The number of samples in this sequence. */
    public int size();

    /** Random access to the samples of the sequence.
     *  <p>
     *  It is an error to use indices below 0 or
     *  &gt;= size().
     *  @return The Sample of given index.
     */
    public PlotDataItem<XTYPE> get(int index);

    //    public String toString()
    //    {
    //        // Derived class should include InstrumentedReadWriteLock#toString()
    //    }
}
