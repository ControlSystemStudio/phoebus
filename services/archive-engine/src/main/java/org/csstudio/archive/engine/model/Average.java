/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

/** Running (exponential) average
 *  @author Kay Kasemir
 */
public class Average
{
    /** Exponential weight of current size */
    private static final double WEIGHT = 0.1;

    private double average = 0.0;

    /** Reset to 0 */
    public void reset()
    {
        average = 0.0;
    }

    /** @return Average value */
    public double get()
    {
        return average;
    }

    /** Update average by including given value */
    public void update(double value)
    {
        // Idea: Jump-start the average by using first sample
        // instead of approaching real average from null.
        // Using 0.0 as indicator for 'no previous value',
        // which might be wrong...
        if (average == 0.0)
            average = value;
        else
            average = average + WEIGHT*(value-average);
    }
}
