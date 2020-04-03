/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import static org.epics.pva.PVASettings.logger;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Level;

/** PV Access structure
 *
 *  <p>Holds one or more {@link PVAData} elements.
 *  Often named as for example a normative type.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAStructureArray extends PVADataWithID implements PVAArray
{
    public static PVAStructureArray decodeType(final PVATypeRegistry types, final String name, final ByteBuffer buffer) throws Exception
    {
        final PVAData element_type = types.decodeType("", buffer);
        if (! (element_type instanceof PVAStructure))
            throw new Exception("Expected structure for element type of structure[] '" + name + "', got " + element_type);

        return new PVAStructureArray(name, (PVAStructure) element_type);
    }

    /** Structure type name */
    private final PVAStructure element_type;

    /** Unmodifiable list of elements.
     *
     *  <p>The value of each element may be updated, but
     *  no elements can be added, removed, replaced.
     */
    private volatile PVAStructure[] elements;

    public PVAStructureArray(final String name, final PVAStructure element_type, final PVAStructure... elements)
    {
        super(name);
        this.element_type = element_type;
        this.elements = elements;
    }

    /** @return Element type (no value) */
    public PVAStructure getElementType()
    {
        return element_type;
    }

    /** @return Array elements */
    public PVAStructure[] get()
    {
        return elements;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        // Cannot set structure, only individual elements
        throw new Exception("Cannot set " + getStructureName() + " " + name + " to " + new_value);
    }

    /** @return Structure type name */
    public String getStructureName()
    {
        return element_type.getStructureName();
    }

    @Override
    public PVAStructureArray cloneType(final String name)
    {
        return new PVAStructureArray(name, element_type);
    }

    @Override
    public PVAStructureArray cloneData()
    {
        final PVAStructure[] safe = elements;
        final PVAStructure[] copy = new PVAStructure[safe.length];
        // Deep copy
        for (int i=0; i<copy.length; ++i)
            copy[i] = safe[i].cloneData();
        return new PVAStructureArray(name, element_type, copy);
    }

    @Override
    public void encodeType(final ByteBuffer buffer, final BitSet described) throws Exception
    {
        final short type_id = getTypeID();
        if (type_id != 0)
        {
            final int u_type_id = Short.toUnsignedInt(type_id);
            if (described.get(u_type_id))
            {   // Refer to existing definition
                buffer.put(PVAFieldDesc.ONLY_ID_TYPE_CODE);
                buffer.putShort(type_id);
                // Done!
                return;
            }
            else
            {   // (Re-)define this type
                buffer.put(PVAFieldDesc.FULL_WITH_ID_TYPE_CODE);
                buffer.putShort(type_id);
                described.set(u_type_id);
            }
        }

        // Encode 'structure array' and its element definition
        buffer.put((byte) (PVAComplex.FIELD_DESC_TYPE | PVAFieldDesc.Array.VARIABLE_SIZE.mask | PVAComplex.STRUCTURE));
        element_type.encodeType(buffer, described);
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        final int count = PVASize.decodeSize(buffer);
        // Try to re-use elements
        PVAStructure[] new_elements = elements;
        if (new_elements == null  ||  new_elements.length != count)
            new_elements = new PVAStructure[count];
        for (int i=0; i<count; ++i)
        {   // Is this element non-null?
            final boolean non_null = PVABool.decodeBoolean(buffer);
            if (non_null)
            {   // Try to update existing element
                PVAStructure element = new_elements[i];
                if (element == null)
                    element = element_type.cloneType("");
                element.decode(types, buffer);
                new_elements[i] = element;
            }
            else
                new_elements[i] = null;
        }
        elements = new_elements;
        logger.log(Level.FINER, () -> "Decoded structure[] element: " + elements);
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        final PVAStructure[] copy = elements;
        PVASize.encodeSize(copy.length, buffer);
        for (int i=0; i<copy.length; ++i)
        {
            if (copy[i] == null)
                PVABool.encodeBoolean(false, buffer);
            else
            {
                PVABool.encodeBoolean(true, buffer);
                copy[i].encode(buffer);
            }
        }
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAStructureArray)
        {
            final PVAStructureArray other = (PVAStructureArray) new_value;
            if (! Arrays.equals(other.elements, elements))
            {
                // Deep copy
                final PVAStructure[] copy = new PVAStructure[other.elements.length];
                for (int i=0; i<copy.length; ++i)
                    copy[i] = other.elements[i].cloneData();
                changes.set(index);
            }
        }
        return index + 1;
    }

    @Override
    public void formatType(int level, StringBuilder buffer)
    {
        indent(level, buffer);
        if (getStructureName().isEmpty())
            buffer.append("structure[]");
        else
            buffer.append(getStructureName()).append("[] ");
        buffer.append(name);
        if (type_id > 0)
            buffer.append(" [#").append(type_id).append("]");
        buffer.append("\n");
        element_type.formatType(level + 1, buffer);
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        indent(level, buffer);
        if (getStructureName().isEmpty())
            buffer.append("structure[] ");
        else
            buffer.append(getStructureName()).append("[] ");
        buffer.append(name);
        for (PVAData element : elements)
        {
            buffer.append("\n");
            element.format(level+1, buffer);
        }
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVAStructureArray))
            return false;
        final PVAStructureArray other = (PVAStructureArray) obj;
        return other.elements.equals(elements);
    }
}
