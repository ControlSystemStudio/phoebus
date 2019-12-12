/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/** Plain array implementation of PlotSamples
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotSampleArray extends PlotSamples
{
    private List<PlotSample> samples = Collections.emptyList();

    /** @param samples Samples <u>which are NOT copied</u> */
    public void set(final List<PlotSample> samples)
    {
        this.samples = samples;
    }

    /** {@inheritDoc} */
    @Override
    public int size()
    {
        return samples.size();
    }

    /** {@inheritDoc} */
    @Override
    public PlotSample get(final int index)
    {
        // For debugging, show stack trace when missing lock
        if (lock.getReadHoldCount() <= 0  && ! lock.isWriteLockedByCurrentThread())
            logger.log(Level.WARNING, "Missing lock", new Exception("Stack Trace"));

        return samples.get(index);
    }
}
