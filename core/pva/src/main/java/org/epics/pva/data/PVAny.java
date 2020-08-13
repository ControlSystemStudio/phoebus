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

/** PV Access 'any'
 *
 *  <p>Also called "Variant Union",
 *  holds one {@link PVAData} element
 *  which may change type for each value update.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAny extends PVAData
{
    /** Value of 'any' type, may be <code>null</code> */
    private volatile PVAData value;

    /** @param name Name of the any (may be "")
     *  @param value Initial value
     */
    public PVAny(final String name, final PVAData value)
    {
        super(name);
        this.value = value;
    }

    /** @param name Name of the any (may be ""), initially empty */
    public PVAny(final String name)
    {
        this(name, null);
    }

    /** @param <PVA> {@link PVAData} type
     *  @return Current value
     */
    @SuppressWarnings("unchecked")
    public <PVA extends PVAData> PVA get()
    {
        return (PVA) value;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        if (new_value == null  ||  new_value instanceof PVAData)
            value = (PVAData) new_value;
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    @Override
    public PVAny cloneType(final String name)
    {
        return new PVAny(name);
    }

    @Override
    public PVAny cloneData()
    {
        PVAData safe = value;
        if (value != null)
            safe = safe.cloneData();
        return new PVAny(name, safe);
    }

    @Override
    public void encodeType(final ByteBuffer buffer, final BitSet described) throws Exception
    {
        buffer.put((byte) (PVAComplex.FIELD_DESC_TYPE | PVAComplex.VARIANT_UNION));
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        // Determine what the actual type is
        value = types.decodeType("any", buffer);
        // Unless it's the NULL_TYPE_CODE, decode value
        if (value != null)
            value.decode(types, buffer);
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        if (value == null)
            buffer.put(PVAFieldDesc.NULL_TYPE_CODE);
        else
        {
            final BitSet described = new BitSet();
            value.encodeType(buffer, described);
            value.encode(buffer);
        }
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAny)
        {
            final PVAny other = (PVAny) new_value;
            if (other.value == null)
            {
                if (value != null)
                {
                    value = null;
                    changes.set(index);
                }
                // else: No change, this.value is already null
            }
            else
            {
                if (! other.value.equals(value))
                {
                    value = other.value.cloneData();
                    changes.set(index);
                }
                // else: No change, this.value already equals other.value
            }
        }
        return index + 1;
    }

    @Override
    protected void formatType(final int level, final StringBuilder buffer)
    {
        indent(level, buffer);
        buffer.append("any ").append(name);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        formatType(level, buffer);
        buffer.append("\n");
        if (value == null)
        {
            indent(level+1, buffer);
            buffer.append("(none)");
        }
        else
            value.format(level+1, buffer);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVAny))
            return false;
        final PVAny other = (PVAny) obj;
        return Objects.equals(value, other.value);
    }
}
