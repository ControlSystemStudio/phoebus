/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.epics.vtype.VType;

/** One input to the formula: Model item that provides data, Variable name
 *  for use in the formula
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaInput
{
    /** The model item used as input. */
    final private ModelItem item;

    /** The variable name for this input. */
    final private String variable_name;

    /** Index of the sample that next() will return or -1 when 'done' */
    private int index = -1;

    /** Constructor
     *  @param item ModelItem that provides the input data
     *  @param variable_name Name used in formula for this input
     */
    public FormulaInput(final ModelItem item, final String variable_name)
    {
        this.item = item;
        this.variable_name = variable_name;
    }

    /** @return ModelItem that this input reads */
    public ModelItem getItem()
    {
        return item;
    }

    /** @return Name of the variable for this input */
    public String getVariableName()
    {
        return variable_name;
    }

    /** Reset the sample iterator to the first sample
     *  @see #next()
     *  @return First sample or <code>null</code>
     *  @throws Exception on error
     */
    public VType first() throws Exception
    {
        final PlotSamples samples = item.getSamples();
        if (! samples.getLock().tryLock(10, TimeUnit.SECONDS))
            throw new TimeoutException("Cannot lock " + samples);
        try
        {
            if (samples.size() > 0)
                index = 0;
            else
                index = -1;
            return next();
        }
        finally
        {
            samples.getLock().unlock();
        }
    }

    /** Iterate over the samples of the input's ModelItem
     *  @return Next value or <code>null</code>
     *  @throws Exception
     */
    public VType next() throws Exception
    {
        if (index < 0)
            return null;
        final VType result;
        final PlotSamples samples = item.getSamples();
        if (! samples.getLock().tryLock(10, TimeUnit.SECONDS))
            throw new TimeoutException("Cannot lock " + samples);
        try
        {
            if (index < samples.size())
                result = samples.get(index++).getVType();
            else
            {
                result = null;
                index = -1;
            }
        }
        finally
        {
            samples.getLock().unlock();
        }
        return result;
    }

    /** Check for new samples.
     *  Note that this will not reset the new-sample flag,
     *  so its use has to be coordinated with the overall mechanism
     *  that checks for the need to redraw.
     *  @return <code>true</code> if ModelItem has new samples
     */
    public boolean hasNewSamples()
    {
        return item.getSamples().hasNewSamples();
    }

    /** @return Debug text */
    @Override
    public String toString()
    {
        return "FormulaInput '" + variable_name + "': " + item.getName();
    }
}
