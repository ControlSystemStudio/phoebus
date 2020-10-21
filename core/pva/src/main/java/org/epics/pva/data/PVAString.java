/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

/** 'Primitive' PV Access data type
 *   @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAString extends PVAData
{
    public static final byte FIELD_DESC_TYPE = (byte)0b01100000;

    /** @param string Text
     *  @return Encoded size of string in bytes
     */
    public static int getEncodedSize(final String string)
    {
        if (string == null)
            return PVASize.size(-1);

        final int len = string.getBytes().length;
        return PVASize.size(len) + len;
    }

    /** @param string String to encode
     *  @param buffer Buffer into which to encode string
     */
    public static void encodeString(final String string, final ByteBuffer buffer)
    {
        if (string == null)
            PVASize.encodeSize(-1, buffer);
        else
        {
            final byte[] bytes = string.getBytes();
            PVASize.encodeSize(bytes.length, buffer);
            buffer.put(bytes);
        }
    }

    /** @param buffer Buffer from which to decode string
     *  @return Decoded string
     */
    public static String decodeString(final ByteBuffer buffer)
    {
        final int size = PVASize.decodeSize(buffer);
        if (size >= 0)
        {
            byte[] bytes = new byte[size];
            buffer.get(bytes);
            return new String(bytes);
        }
        return null;
    }

    static PVAData decodeType(final String name, final byte field_desc, final ByteBuffer buffer) throws Exception
    {
        final PVAFieldDesc.Array array = PVAFieldDesc.Array.forFieldDesc(field_desc);
        if (array == PVAFieldDesc.Array.SCALAR)
            return new PVAString(name);
        else if (array == PVAFieldDesc.Array.VARIABLE_SIZE)
            return new PVAStringArray(name);
        else
            throw new Exception("Cannot handle " + array);
    }

    private volatile String value;

    public PVAString(final String name)
    {
        this(name, null);
    }

    public PVAString(final String name, final String value)
    {
        super(name);
        this.value = value;
    }

    /** @return Current value */
    public String get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final String value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVAString)
            set(((PVAString) new_value).get());
        else
            set(Objects.toString(new_value));
    }

    @Override
    public PVAString cloneType(final String name)
    {
        return new PVAString(name);
    }

    @Override
    public PVAString cloneData()
    {
        return new PVAString(name, value);
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        buffer.put(FIELD_DESC_TYPE);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        value = decodeString(buffer);
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        encodeString(value, buffer);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAString)
        {
            final PVAString other = (PVAString) new_value;
            if (! Objects.equals(other.value, value))
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
        buffer.append("string ").append(name);
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
        if (! (obj instanceof PVAString))
            return false;
        final PVAString other = (PVAString) obj;
        return Objects.equals(other.value, value);
    }
}
