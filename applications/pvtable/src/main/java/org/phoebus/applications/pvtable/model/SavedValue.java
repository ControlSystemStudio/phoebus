/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.diirt.util.array.IteratorDouble;
import org.diirt.util.array.IteratorNumber;
import org.diirt.vtype.VDoubleArray;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VNumber;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VString;
import org.diirt.vtype.VStringArray;
import org.diirt.vtype.VType;
import org.phoebus.pv.PV;

/** Base for saved values of a table item
 *
 *  <p>Values are always saved as String. When reading autosave-files, the PV's data
 *  type is unknown until the channel connects, and a channel may never connect.
 *  To allow reading and writing files without changing the exact value format,
 *  the text is kept.
 *
 *  <p>Derived implementations provide support for scalar (String) and array (List
 *  <String>)
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class SavedValue
{
    /** @param current_value Current value of PV
     *  @return {@link SavedValue} that contains current value
     *  @throws Exception on error
     */
    public static SavedValue forCurrentValue(final VType value) throws Exception
    {
        // Scalars
        if (value instanceof VNumber)
            return new SavedScalarValue(((VNumber) value).getValue().toString());
        if (value instanceof VEnum)
            return new SavedScalarValue(Integer.toString(((VEnum) value).getIndex()));
        if (value instanceof VString)
            return new SavedScalarValue(((VString) value).getValue());

        // Arrays
        final List<String> texts = new ArrayList<>();
        if (value instanceof VDoubleArray)
        {
            // Format double as double
            final IteratorDouble values = ((VDoubleArray) value).getData().iterator();
            while (values.hasNext())
                texts.add(Double.toString(values.nextDouble()));
            return new SavedArrayValue(texts);
        }
        if (value instanceof VNumberArray)
        {
            // Format other numbers as integer
            final IteratorNumber values = ((VNumberArray) value).getData().iterator();
            while (values.hasNext())
                texts.add(Long.toString(values.nextLong()));
            return new SavedArrayValue(texts);
        }
        if (value instanceof VStringArray)
            return new SavedArrayValue(((VStringArray) value).getData());
        if (value instanceof VEnumArray)
        {
            // Save indices
            final IteratorNumber values = ((VEnumArray) value).getIndexes().iterator();
            while (values.hasNext())
                texts.add(Long.toString(values.nextLong()));
            return new SavedArrayValue(texts);
        }
        throw new Exception("Cannot handle " + value);
    }

    /** @param saved_value Saved text, must not be <code>null</code>
     *  @return Converted to {@link Number}
     */
    protected Number getSavedNumber(final String saved_value)
    {
        Objects.requireNonNull(saved_value);
        if (saved_value.startsWith("0x"))
            return Long.decode(saved_value);

        // Treat other numbers as double to allow "1e2" even for integer
        return Double.parseDouble(saved_value);
    }

    /** Compare saved value against current value of a PV
     *
     *  @param current_value Value to compare against
     *  @param tolerance  Tolerance to use for numeric values
     *  @return <code>true</code> if values match within tolerance
     *  @throws Exception on error
     */
    abstract public boolean isEqualTo(final VType current_value, final double tolerance) throws Exception;

    /** Restore saved value to PV
     *
     *  @param pv PV to write
     *  @param completion_timeout_secs Timeout for completion (put-callback) or 0 to not use completion
     *  @throws Exception on error, including timeout
     */
    abstract public void restore(PV pv, long completion_timeout_secs) throws Exception;

    /** @return String representation for display purpose */
    @Override
    abstract public String toString();
}
