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
public class PVABool extends PVAData
{
    public static final byte FIELD_DESC_TYPE = (byte)0b00000000;

    public static PVAData decodeType(final String name, final byte field_desc, final ByteBuffer buffer) throws Exception
    {
        final PVAFieldDesc.Array array = PVAFieldDesc.Array.forFieldDesc(field_desc);
        if (array == PVAFieldDesc.Array.SCALAR)
            return new PVABool(name);
        else if (array == PVAFieldDesc.Array.VARIABLE_SIZE)
            return new PVABoolArray(name);
        throw new Exception("Cannot handle " + array);
    }

    private volatile boolean value;

    public PVABool(final String name)
    {
        this(name, false);
    }

    public PVABool(final String name, final boolean value)
    {
        super(name);
        this.value = value;
    }

    /** @return Current value */
    public boolean get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final boolean value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVABool)
            set(((PVABool) new_value).get());
        else if (new_value instanceof PVANumber)
            set(((PVANumber) new_value).getNumber().intValue() != 0);
        else if (new_value instanceof Boolean)
            set(((Boolean) new_value));
        else if (new_value instanceof Number)
            set(((Number) new_value).intValue() != 0);
        else if (new_value instanceof String)
            set(Boolean.parseBoolean(new_value.toString().toLowerCase()));
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVABool cloneType(final String name)
    {
        return new PVABool(name);
    }

    @Override
    public PVABool cloneData()
    {
        return new PVABool(name, value);
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        buffer.put(FIELD_DESC_TYPE);
    }

    /** @param buffer Buffer from which to decode boolean
     *  @return Decoded boolean
     */
    public static boolean decodeBoolean(final ByteBuffer buffer)
    {
        return buffer.get() != 0;
    }

    /** @param value Value to encode
     *  @param buffer Buffer into which to encode boolean
     */
    public static void encodeBoolean(final boolean value, final ByteBuffer buffer)
    {
        buffer.put(value ? (byte)1 : (byte) 0);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        value = decodeBoolean(buffer);
    }

    @Override
    public void encode(final ByteBuffer buffer)
    {
        encodeBoolean(value, buffer);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVABool)
        {
            final PVABool other = (PVABool) new_value;
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
        buffer.append("boolean ").append(name);
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
        if (! (obj instanceof PVABool))
            return false;
        final PVABool other = (PVABool) obj;
        return other.value == value;
    }
}
