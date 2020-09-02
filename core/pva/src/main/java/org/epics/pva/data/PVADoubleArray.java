/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.epics.pva.PVASettings;

/** 'Primitive' PV Access data type
 *   @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVADoubleArray extends PVAData implements PVAArray
{
    private volatile double[] value;

    /** Construct variable-size array
     *  @param name Data item name
     *  @param value Initial value
     */
    public PVADoubleArray(final String name, final double... value)
    {
        super(name);
        this.value = value;
    }

    /** @return Current value */
    public double[] get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final double[] value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVADoubleArray)
        {
            final double[] other = ((PVADoubleArray) new_value).value;
            value = Arrays.copyOf(other, other.length);
        }
        else if (new_value instanceof double[])
            set(((double[]) new_value));
        else if (new_value instanceof List)
        {
            @SuppressWarnings("rawtypes")
            final List<?> list = (List)new_value;
            final double[] new_items = new double[list.size()];
            for (int i=0;  i<new_items.length;  ++i)
            {
                final Object item = list.get(i);
                if (item instanceof Number)
                    new_items[i] = ((Number)item).doubleValue();
                else
                    throw new Exception("Cannot set " + formatType() + " to " + new_value);
            }
            value = new_items;
        }
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVADoubleArray cloneType(final String name)
    {
        return new PVADoubleArray(name);
    }

    @Override
    public PVADoubleArray cloneData()
    {
        return new PVADoubleArray(name, value.clone());
    }

    @Override
    public void encodeType(final ByteBuffer buffer, final BitSet described) throws Exception
    {
        // 010 floating-point            PVAFloat.FIELD_DESC_TYPE
        // 01  variable-size array flag  PVAFieldDesc.Array.VARIABLE_SIZE
        // 011 double binary64
        buffer.put((byte) 0b01001011);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        final int size = PVASize.decodeSize(buffer);
        final double[] new_value = new double[size];
        for (int i=0; i<size; ++i)
            new_value[i] = buffer.getDouble();
        value = new_value;
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        final double[] copy = value;
        PVASize.encodeSize(copy.length, buffer);
        for (int i=0; i<copy.length; ++i)
            buffer.putDouble(copy[i]);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVADoubleArray)
        {
            final PVADoubleArray other = (PVADoubleArray) new_value;
            // At least for open JDK11,
            // this does use Double.doubleToRawLongBits and thus handles
            // NaN == NaN
            if (! Arrays.equals(other.value, value))
            {
                value = other.value.clone();
                changes.set(index);
            }
        }
        return index + 1;
    }

    @Override
    protected void formatType(final int level, final StringBuilder buffer)
    {
        indent(level, buffer);
        buffer.append("double[] ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append(" [");
        final double[] safe = value;
        if (safe == null)
            buffer.append("null");
        else
        {
            final int show = Math.min(PVASettings.EPICS_PVA_MAX_ARRAY_FORMATTING, safe.length);
            for (int i=0; i<show; ++i)
            {
                if (i > 0)
                    buffer.append(", ");
                buffer.append(safe[i]);
            }
            if (safe.length > show)
                buffer.append(", ...");
        }
        buffer.append("]");
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVADoubleArray))
            return false;
        final PVADoubleArray other = (PVADoubleArray) obj;
        return Arrays.equals(other.value, value);
    }
}
