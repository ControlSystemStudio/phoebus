/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.nio.ByteBuffer;
import java.util.BitSet;

/** 'Primitive' PV Access data type
 *   @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVADouble extends PVANumber
{
    private volatile double value;

    public PVADouble(final String name)
    {
        this(name, Double.NaN);
    }

    public PVADouble(final String name, final double value)
    {
        super(name);
        this.value = value;
    }

    @Override
    public Number getNumber()
    {
        return value;
    }

    /** @return Current value */
    public double get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final double value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVANumber)
            set(((PVANumber) new_value).getNumber().doubleValue());
        else if (new_value instanceof Number)
            set(((Number) new_value).doubleValue());
        else if (new_value instanceof String)
                set(parseString(new_value.toString()).doubleValue());
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVADouble cloneType(final String name)
    {
        return new PVADouble(name);
    }

    @Override
    public PVADouble cloneData()
    {
        return new PVADouble(name, value);
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        buffer.put((byte) 0b01000011);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        value = buffer.getDouble();
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        buffer.putDouble(value);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVADouble)
        {
            final PVADouble other = (PVADouble) new_value;
            if (Double.doubleToRawLongBits(other.value) !=
                Double.doubleToRawLongBits(value))
            {
                value = other.value;
                changes.set(index);
            }
        }
        return index + 1;
    }

    @Override
    protected void formatType(final int level, final StringBuilder buffer)
    {
        indent(level, buffer);
        buffer.append("double ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append(" ").append(value);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVADouble))
            return false;
        final PVADouble other = (PVADouble) obj;
        return Double.doubleToRawLongBits(other.value) ==
               Double.doubleToRawLongBits(value);
    }
}
