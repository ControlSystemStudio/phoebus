/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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

/** 'Primitive' PV Access data type
 *   @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAIntArray extends PVAData implements PVAArray
{
    private final boolean unsigned;
    private volatile int[] value;

    public PVAIntArray(final String name, final boolean unsigned, final int[] value)
    {
        super(name);
        this.unsigned = unsigned;
        this.value = value;
    }

    public PVAIntArray(final String name, final boolean unsigned)
    {
        this(name, unsigned, new int[0]);
    }

    /** @return Is value unsigned? */
    public boolean isUnsigned()
    {
        return unsigned;
    }

    /** @return Current value */
    public int[] get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final int[] value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof int[])
            set(((int[]) new_value));
        else if (new_value instanceof List)
        {
            @SuppressWarnings("rawtypes")
            final List<?> list = (List)new_value;
            final int[] new_items = new int[list.size()];
            for (int i=0;  i<new_items.length;  ++i)
            {
                final Object item = list.get(i);
                if (item instanceof Number)
                    new_items[i] = ((Number)item).intValue();
                else
                    throw new Exception("Cannot set " + formatType() + " to " + new_value);
            }
            value = new_items;
        }
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVAIntArray cloneType(final String name)
    {
        return new PVAIntArray(name, unsigned);
    }

    @Override
    public PVAIntArray cloneData()
    {
        return new PVAIntArray(name, unsigned, value.clone());
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        if (unsigned)
            buffer.put((byte) 0b00101110);
        else
            buffer.put((byte) 0b00101010);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        final int size = PVASize.decodeSize(buffer);
        int[] new_value = value;
        if (new_value == null  ||  new_value.length != size)
            new_value = new int[size];
        for (int i=0; i<size; ++i)
            new_value[i] = buffer.getInt();
        value = new_value;
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        final int[] copy = value;
        PVASize.encodeSize(copy.length, buffer);
        for (int i=0; i<copy.length; ++i)
            buffer.putInt(copy[i]);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAIntArray)
        {
            final PVAIntArray other = (PVAIntArray) new_value;
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
        if (unsigned)
            buffer.append('u');
        buffer.append("int[] ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append(" [");
        final int[] safe = value;
        if (safe == null)
            buffer.append("null");
        else
        {
            for (int i=0; i<safe.length; ++i)
            {
                if (i > 0)
                    buffer.append(", ");
                if (unsigned)
                    buffer.append(Integer.toUnsignedLong(safe[i]));
                else
                    buffer.append(safe[i]);
            }
        }
        buffer.append("]");
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVAIntArray))
            return false;
        final PVAIntArray other = (PVAIntArray) obj;
        return Arrays.equals(other.value, value);
    }
}
