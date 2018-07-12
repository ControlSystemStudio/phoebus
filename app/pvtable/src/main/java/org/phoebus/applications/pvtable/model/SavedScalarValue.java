/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import java.util.concurrent.TimeUnit;

import org.phoebus.pv.PV;
import org.phoebus.vtype.VDouble;
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VFloat;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VString;
import org.phoebus.vtype.VType;

/** Saved value of a scalar table item
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SavedScalarValue extends SavedValue
{
    final private String saved_value;

    /**
     * Initialize from text
     *
     * @param value_text
     */
    public SavedScalarValue(final String value_text)
    {
        saved_value = value_text;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEqualTo(final VType current_value, final double tolerance) throws Exception
    {
        if (current_value == null)
            return true;
        if (current_value instanceof VNumber)
        {
            final double v1 = ((VNumber) current_value).getValue().doubleValue();
            final double v2 = getSavedNumber(saved_value).doubleValue();
            return Math.abs(v2 - v1) <= tolerance;
        }
        if (current_value instanceof VString)
        {
            final String v1 = ((VString) current_value).getValue();
            return v1.equals(saved_value);
        }
        if (current_value instanceof VEnum)
        {
            final int v1 = ((VEnum) current_value).getIndex();
            final int v2 = Integer.parseInt(saved_value);
            return Math.abs(v2 - v1) <= tolerance;
        }
        throw new Exception("Cannot compare against unhandled type " + current_value.getClass().getName());
    }

    /** {@inheritDoc} */
    @Override
    public void restore(final PV pv, long completion_timeout_secs) throws Exception
    {
        // Determine what type to write based on current value of the PV
        final VType pv_type = pv.read();

        if ((pv_type instanceof VDouble) || (pv_type instanceof VFloat))
        {
            if (completion_timeout_secs > 0)
                pv.asyncWrite(Double.parseDouble(saved_value)).get(completion_timeout_secs, TimeUnit.SECONDS);
            else
                pv.write(Double.parseDouble(saved_value));
        }
        else if (pv_type instanceof VNumber  ||  pv_type instanceof VEnum)
        {
            if (completion_timeout_secs > 0)
                pv.asyncWrite(getSavedNumber(saved_value).longValue()).get(completion_timeout_secs, TimeUnit.SECONDS);
            else
                pv.write(getSavedNumber(saved_value).longValue());
        }
        else
        {
            // Write as text
            if (completion_timeout_secs > 0)
                pv.asyncWrite(saved_value).get(completion_timeout_secs, TimeUnit.SECONDS);
            else
                pv.write(saved_value);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return saved_value;
    }
}
