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
public class PVAShort extends PVANumber
{
    private final boolean unsigned;
    private volatile short value;

    public PVAShort(final String name, final boolean unsigned, final short value)
    {
        super(name);
        this.unsigned = unsigned;
        this.value = value;
    }

    public PVAShort(final String name, final boolean unsigned)
    {
        this(name, unsigned, (short)0);
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
    public short get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final short value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVANumber)
            set(((PVANumber) new_value).getNumber().shortValue());
        else if (new_value instanceof Number)
            set(((Number) new_value).shortValue());
        else if (new_value instanceof String)
            set(parseString(new_value.toString()).shortValue());
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVAShort cloneType(final String name)
    {
        return new PVAShort(name, unsigned);
    }

    @Override
    public PVAShort cloneData()
    {
        return new PVAShort(name, unsigned, value);
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        if (unsigned)
            buffer.put((byte) 0b00100101);
        else
            buffer.put((byte) 0b00100001);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        value = buffer.getShort();
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        buffer.putShort(value);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAShort)
        {
            final PVAShort other = (PVAShort) new_value;
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
        buffer.append("short ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append(" ");
        if (unsigned)
            buffer.append(Short.toUnsignedInt(value));
        else
            buffer.append(value);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVAShort))
            return false;
        final PVAShort other = (PVAShort) obj;
        return other.value == value;
    }
}
