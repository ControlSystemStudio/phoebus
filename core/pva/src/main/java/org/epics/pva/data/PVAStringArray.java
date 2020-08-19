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
import java.util.Objects;

import org.epics.pva.PVASettings;

/** 'Primitive' PV Access data type
 *   @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAStringArray extends PVAData implements PVAArray
{
    private volatile String[] value = new String[0];

    /** Construct variable-size string array
     *  @param name Data item name
     *  @param value Initial value
     */
    public PVAStringArray(final String name, final String... value)
    {
        super(name);
        this.value = value;
    }

    /** @return Current value */
    public String[] get()
    {
        return value;
    }

    /** @param value Desired new value */
    public void set(final String[] value)
    {
        this.value = value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value instanceof PVAStringArray)
        {
            final String[] other = ((PVAStringArray) new_value).value;
            value = Arrays.copyOf(other, other.length);
        }
        else if (new_value instanceof String[])
            value = (String[]) new_value;
        else if (new_value instanceof List)
        {
            @SuppressWarnings("rawtypes")
            final List<?> list = (List)new_value;
            value = new String[list.size()];
            for (int i=0; i<value.length; ++i)
                value[i] = Objects.toString(list.get(i));
        }
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVAStringArray cloneType(final String name)
    {
        return new PVAStringArray(name);
    }

    @Override
    public PVAStringArray cloneData()
    {
        return new PVAStringArray(name, value.clone());
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
    {
        buffer.put((byte) (PVAString.FIELD_DESC_TYPE | PVAFieldDesc.Array.VARIABLE_SIZE.mask));
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        final int size = PVASize.decodeSize(buffer);
        String[] new_value = value;
        if (new_value == null  ||  new_value.length != size)
            new_value = new String[size];
        for (int i=0; i<size; ++i)
            new_value[i] = PVAString.decodeString(buffer);
        value = new_value;
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        final String[] copy = value;
        PVASize.encodeSize(copy.length, buffer);
        for (String s : copy)
            PVAString.encodeString(s, buffer);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVALongArray)
        {
            final PVAStringArray other = (PVAStringArray) new_value;
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
        buffer.append("string[] ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        final String[] safe = value;
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
        if (! (obj instanceof PVAStringArray))
            return false;
        final PVAStringArray other = (PVAStringArray) obj;
        return Arrays.equals(other.value, value);
    }
}
