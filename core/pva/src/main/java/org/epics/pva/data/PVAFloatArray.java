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
public class PVAFloatArray extends PVAData implements PVAArray
{
    private volatile float[] value;

    /** Construct variable-size array
     *  @param name Data item name
     *  @param value Initial value
     */
    public PVAFloatArray(final String name, final float... value)
    {
        super(name);
        this.value = value;
    }

    /** @return Current value */
    public float[] get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final float[] value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVAFloatArray)
        {
            final float[] other = ((PVAFloatArray) new_value).value;
            value = Arrays.copyOf(other, other.length);
        }
        else if (new_value instanceof float[])
            set(((float[]) new_value));
        else if (new_value instanceof List)
        {
            @SuppressWarnings("rawtypes")
            final List<?> list = (List)new_value;
            final float[] new_items = new float[list.size()];
            for (int i=0;  i<new_items.length;  ++i)
            {
                final Object item = list.get(i);
                if (item instanceof Number)
                    new_items[i] = ((Number)item).floatValue();
                else
                    throw new Exception("Cannot set " + formatType() + " to " + new_value);
            }
            value = new_items;
        }
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVAFloatArray cloneType(final String name)
    {
        return new PVAFloatArray(name);
    }

    @Override
    public PVAFloatArray cloneData()
    {
        return new PVAFloatArray(name, value.clone());
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        buffer.put((byte) 0b01001010);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        final int size = PVASize.decodeSize(buffer);
        final float[] new_value = new float[size];
        for (int i=0; i<size; ++i)
            new_value[i] = buffer.getFloat();
        value = new_value;
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        final float[] copy = value;
        PVASize.encodeSize(copy.length, buffer);
        for (int i=0; i<copy.length; ++i)
            buffer.putFloat(copy[i]);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAFloatArray)
        {
            final PVAFloatArray other = (PVAFloatArray) new_value;
            // At least for open JDK11,
            // this does use Float.floatToRawIntBits and thus handles
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
        buffer.append("float[] ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append(" [");
        final float[] safe = value;
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
        if (! (obj instanceof PVAFloatArray))
            return false;
        final PVAFloatArray other = (PVAFloatArray) obj;
        return Arrays.equals(other.value, value);
    }
}
