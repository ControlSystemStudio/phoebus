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
public class PVALong extends PVANumber
{
    private final boolean unsigned;
    private volatile long value;

    public PVALong(final String name)
    {
        this(name, false);
    }

    public PVALong(final String name, final boolean unsigned)
    {
        this(name, unsigned, 0);
    }

    public PVALong(final String name, final boolean unsigned, final long value)
    {
        super(name);
        this.unsigned = unsigned;
        this.value = value;
    }

    /** @return Is value unsigned? */
    public boolean isUnsigned()
    {
        return unsigned;
    }

    @Override
    public Number getNumber()
    {
        return value;
    }

    /** @return Current value */
    public long get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final long value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVANumber)
            set(((PVANumber) new_value).getNumber().longValue());
        else if (new_value instanceof Number)
            set(((Number) new_value).longValue());
        else if (new_value instanceof String)
            set(parseString(new_value.toString()).longValue());
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVALong cloneType(final String name)
    {
        return new PVALong(name, unsigned);
    }

    @Override
    public PVALong cloneData()
    {
        return new PVALong(name, unsigned, value);
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        if (unsigned)
            buffer.put((byte) 0b00100111);
        else
            buffer.put((byte) 0b00100011);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        value = buffer.getLong();
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        buffer.putLong(value);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVALong)
        {
            final PVALong other = (PVALong) new_value;
            if (other.value != value)
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
        if (unsigned)
            buffer.append('u');
        buffer.append("long ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append(" ");
        if (unsigned)
            buffer.append(Long.toUnsignedString(value));
        else
            buffer.append(value);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVALong))
            return false;
        final PVALong other = (PVALong) obj;
        return other.value == value;
    }
}
