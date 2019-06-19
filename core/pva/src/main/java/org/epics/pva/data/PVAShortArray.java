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
public class PVAShortArray extends PVAData implements PVAArray
{
    private final boolean unsigned;
    private volatile short[] value;

    public PVAShortArray(final String name, final boolean unsigned, final short[] value)
    {
        super(name);
        this.unsigned = unsigned;
        this.value = value;
    }

    public PVAShortArray(final String name, final boolean unsigned)
    {
        this(name, unsigned, new short[0]);
    }

    /** @return Is value unsigned? */
    public boolean isUnsigned()
    {
        return unsigned;
    }

    /** @return Current value */
    public short[] get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final short[] value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof short[])
            set(((short[]) new_value));
        else if (new_value instanceof List)
        {
            @SuppressWarnings("rawtypes")
            final List<?> list = (List)new_value;
            final short[] new_items = new short[list.size()];
            for (int i=0;  i<new_items.length;  ++i)
            {
                final Object item = list.get(i);
                if (item instanceof Number)
                    new_items[i] = ((Number)item).shortValue();
                else
                    throw new Exception("Cannot set " + formatType() + " to " + new_value);
            }
            value = new_items;
        }
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVAShortArray cloneType(final String name)
    {
        return new PVAShortArray(name, unsigned);
    }

    @Override
    public PVAShortArray cloneData()
    {
        return new PVAShortArray(name, unsigned, value.clone());
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        if (unsigned)
            buffer.put((byte) 0b00101101);
        else
            buffer.put((byte) 0b00101001);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        final int size = PVASize.decodeSize(buffer);
        // Try to re-use existing array
        short[] new_value = value;
        if (new_value == null  ||  new_value.length != size)
            new_value = new short[size];
        // Considered using
        //   buffer.asShortBuffer().get(new_value);
        // but debugger shows that it ends up in the same loop:
        //   for (..) short_array[i] = buffer.get()
        // Profiler shows that it's overall slower.
        for (int i=0; i<size; ++i)
            new_value[i] = buffer.getShort();
        value = new_value;
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        final short[] copy = value;
        PVASize.encodeSize(copy.length, buffer);
        for (int i=0; i<copy.length; ++i)
            buffer.putShort(copy[i]);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAShortArray)
        {
            final PVAShortArray other = (PVAShortArray) new_value;
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
        buffer.append("short[] ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append(" [");
        final short[] safe = value;
        if (safe == null)
            buffer.append("null");
        else
        {
            for (int i=0; i<safe.length; ++i)
            {
                if (i > 0)
                    buffer.append(", ");
                if (unsigned)
                    buffer.append(Short.toUnsignedInt(safe[i]));
                else
                    buffer.append(safe[i]);
            }
        }
        buffer.append("]");
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVAShortArray))
            return false;
        final PVAShortArray other = (PVAShortArray) obj;
        return Arrays.equals(other.value, value);
    }
}
