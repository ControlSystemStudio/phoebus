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
    public PVAData cloneType(final String name)
    {
        return new PVAny(name);
    }

    @Override
    public PVAData cloneData()
    {
        return new PVAny(name, value.cloneData());
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception
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
        throw new Exception("TODO: Implement encoding 'any'");
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAny)
        {
            final PVAny other = (PVAny) new_value;
            if (! other.value.equals(value))
            {
                value = other.value.cloneData();
                changes.set(index);
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
        return other.equals(value);
    }
}
