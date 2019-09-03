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
public class PVAInt extends PVANumber
{
    public static final byte FIELD_DESC_TYPE = (byte)0b00100000;

    static PVAData decodeType(final String name, final byte field_desc, final ByteBuffer buffer) throws Exception
    {
        final PVAFieldDesc.Array array = PVAFieldDesc.Array.forFieldDesc(field_desc);
        final boolean unsigned = (field_desc & 0b100) != 0;
        final byte actual = (byte) (field_desc & 0b11);
        switch (actual)
        {
        case 3:
            if (array == PVAFieldDesc.Array.VARIABLE_SIZE)
                return new PVALongArray(name, unsigned);
            if (array == PVAFieldDesc.Array.SCALAR)
                return new PVALong(name, unsigned);
            throw new Exception("Cannot handle long " + array);
        case 2:
            if (array == PVAFieldDesc.Array.VARIABLE_SIZE)
                return new PVAIntArray(name, unsigned);
            else if (array == PVAFieldDesc.Array.SCALAR)
                return new PVAInt(name, unsigned);
            throw new Exception("Cannot handle int " + array);
        case 1:
            if (array == PVAFieldDesc.Array.VARIABLE_SIZE)
                return new PVAShortArray(name, unsigned);
            else if (array == PVAFieldDesc.Array.SCALAR)
                return new PVAShort(name, unsigned);
            throw new Exception("Cannot handle short " + array);
        case 0:
            if (array == PVAFieldDesc.Array.VARIABLE_SIZE)
                return new PVAByteArray(name, unsigned);
            else if (array == PVAFieldDesc.Array.SCALAR)
                return new PVAByte(name, unsigned);
                throw new Exception("Cannot handle byte " + array);
        default:
            throw new Exception("Cannot decode integer encoding " + String.format("%02X ", field_desc));
        }
    }

    private final boolean unsigned;
    private volatile int value;

    public PVAInt(final String name)
    {
        this(name, false, 0);
    }

    public PVAInt(final String name, final int value)
    {
        this(name, false, value);
    }

    public PVAInt(final String name, final boolean unsigned)
    {
        this(name, unsigned, 0);
    }

    public PVAInt(final String name, final boolean unsigned, final int value)
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
    public int get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final int value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVANumber)
            set(((PVANumber) new_value).getNumber().intValue());
        else if (new_value instanceof Number)
            set(((Number) new_value).intValue());
        else if (new_value instanceof String)
            set(parseString(new_value.toString()).intValue());
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVAInt cloneType(final String name)
    {
        return new PVAInt(name, unsigned);
    }

    @Override
    public PVAInt cloneData()
    {
        return new PVAInt(name, unsigned, value);
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        if (unsigned)
            buffer.put((byte) 0b00100110);
        else
            buffer.put((byte) 0b00100010);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        value = buffer.getInt();
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        buffer.putInt(value);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAInt)
        {
            final PVAInt other = (PVAInt) new_value;
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
        buffer.append("int ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append(" ");
        if (unsigned)
            buffer.append(Integer.toUnsignedLong(value));
        else
            buffer.append(value);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVAInt))
            return false;
        final PVAInt other = (PVAInt) obj;
        return other.value == value;
    }
}
