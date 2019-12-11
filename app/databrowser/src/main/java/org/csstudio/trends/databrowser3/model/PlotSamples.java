/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.data.InstrumentedReadWriteLock;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;

/** Base for classes that hold plot samples
 *  in a way accessible as {@link PlotDataProvider}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class PlotSamples implements PlotDataProvider<Instant>
{
    protected final ReentrantReadWriteLock lock = new InstrumentedReadWriteLock();

    /** To be set when samples change
     *  @see #testAndClearNewSamplesFlag()
     */
    final protected AtomicBoolean have_new_samples = new AtomicBoolean();

    /** Lock for writing */
    public boolean lockForWriting()
    {
        try
        {
            if (lock.writeLock().tryLock(10, TimeUnit.SECONDS))
                return true;
            throw new TimeoutException();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot lock " + this, ex);
        }
        return false;
    }

    /** Un-lock after writing */
    public void unlockForWriting()
    {
        lock.writeLock().unlock();
        have_new_samples.set(true);
    }

    /** {@inheritDoc} */
    @Override
    public Lock getLock()
    {
        return lock.readLock();
    }

    /** {@inheritDoc} */
    @Override
    abstract public int size();

    /** {@inheritDoc} */
    @Override
    abstract public PlotSample get(int index);

    /** Test if samples changed since the last time
     *  <code>testAndClearNewSamplesFlag</code> was called.
     *  @return <code>true</code> if there were new samples
     */
    public boolean hasNewSamples()
    {
        return have_new_samples.get();
    }

    /** Test if samples changed since the last time this method was called.
     *  @return <code>true</code> if there were new samples
     */
    public boolean testAndClearNewSamplesFlag()
    {
        return have_new_samples.getAndSet(false);
    }

    /** @return Info text about PlotSamples for debugging */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Plot Samples, lock state: ").append(lock);
        if (getLock().tryLock())
        {
            try
            {
                final int n = size();
                buf.append(", ").append(n).append(" samples");
                if (n < 100)
                    for (int i=0; i<n; ++i)
                        buf.append(String.format("\n%3d: ", i)).append(get(i));
            }
            finally
            {
                getLock().unlock();
            }
        }
        return buf.toString();
    }
}
