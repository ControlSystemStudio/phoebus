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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/** PV Access Union
 *
 *  <p>Holds one or more {@link PVAData} elements.
 *  Often named as for example a normative type.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAUnion extends PVADataWithID
{
    static PVAUnion decodeType(final PVATypeRegistry types, final String name, final ByteBuffer buffer) throws Exception
    {
        final String union_name = PVAString.decodeString(buffer);

        // number of elements
        final int size = PVASize.decodeSize(buffer);

        // (name, FieldDesc)[]
        final List<PVAData> values = new ArrayList<>(size);
        for (int i=0; i<size; ++i)
        {
            final String element = PVAString.decodeString(buffer);
            final PVAData value = types.decodeType(element, buffer);
            values.add(value);
        }

        return new PVAUnion(name, union_name, -1, values);
    }

    /** Union type name */
    private final String union_name;

    /** Unmodifiable list of elements.
     *
     *  <p>The value of each element may be updated, but
     *  no elements can be added, removed, replaced.
     */
    private final List<PVAData> elements;

    private volatile int selected;

    /** @param name Variable name
     *  @param struct_name Type name
     *  @param elements Element definitions (typically without data)
     */
    public PVAUnion(final String name, final String struct_name, final PVAData... elements)
    {
        this(name, struct_name, -1, Arrays.asList(elements));
    }

    /** @param name Variable name
     *  @param struct_name Type name
     *  @param selected Selected union element
     *  @param elements Element definitions (typically without data except for 'selected' entry)
     */
    public PVAUnion(final String name, final String struct_name, int selected, final PVAData... elements)
    {
        this(name, struct_name, selected, Arrays.asList(elements));
    }

    /** @param name Variable name
     *  @param struct_name Type name
     *  @param selected Selected union element
     *  @param elements Element definitions (typically without data except for 'selected' entry)
     */
    public PVAUnion(final String name, final String struct_name, int selected, final List<PVAData> elements)
    {
        super(name);
        this.union_name = struct_name;
        this.selected = selected;
        this.elements = Collections.unmodifiableList(elements);
    }

    /** @return Selected element of the union, <code>null</code> if none
     *  @param <PVA> PVAData or subclass
     */
    @SuppressWarnings("unchecked")
    public <PVA extends PVAData> PVA get()
    {
        final int safe_sel = selected;
        if (safe_sel < 0)
            return null;
        return (PVA) elements.get(safe_sel);
    }

    /** @return Index of selection option, -1 if none */
    public int getSelector()
    {
        return selected;
    }

    /** @return Options of the union */
    public List<PVAData> getOptions()
    {
        return elements;
    }

    /** @param option Option of the union to set, -1 for none
     *  @param new_value Value for that option
     *  @throws Exception on error
     */
    public void set(final int option, final Object new_value) throws Exception
    {
        if (option >= 0)
            elements.get(option).setValue(new_value);
        selected = option;
    }

    @Override
    public void setValue(final Object new_value) throws Exception
    {
        elements.get(selected).setValue(new_value);
    }

    /** @return Union type name */
    public String getUnionName()
    {
        return union_name;
    }

    @Override
    public PVAUnion cloneType(final String name)
    {
        final List<PVAData> copy = new ArrayList<>(elements.size());
        for (PVAData element : elements)
            copy.add(element.cloneType(element.getName()));
        final PVAUnion clone = new PVAUnion(name, union_name, -1, copy);
        clone.type_id = type_id;
        return clone;
    }

    @Override
    public PVAUnion cloneData()
    {
        final List<PVAData> copy = new ArrayList<>(elements.size());
        // Deep copy
        for (PVAData element : elements)
            copy.add(element.cloneData());
        final PVAUnion clone = new PVAUnion(name, union_name, selected, copy);
        clone.type_id = type_id;
        return clone;
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

        // Encode 'UNION' type, name
        buffer.put((byte) (PVAComplex.FIELD_DESC_TYPE | PVAComplex.UNION));
        PVAString.encodeString(union_name, buffer);

        // Encode options
        PVASize.encodeSize(elements.size(), buffer);
        for (PVAData element : elements)
        {
            PVAString.encodeString(element.getName(), buffer);
            element.encodeType(buffer, described);
        }
    }

    @Override
    public void decode(final PVATypeRegistry types, final ByteBuffer buffer) throws Exception
    {
        selected = PVASize.decodeSize(buffer);
        if (selected < 0)
        {   // -1 (we're treating any negative number like that) selects no value
            logger.log(Level.FINER, () -> "Union element selector is " + selected + ", no value");
            return;
        }
        if (selected < 0  ||  selected >= elements.size())
            throw new Exception("Invalid union selector " + selected + " for " + formatType());
        final PVAData element = elements.get(selected);
        logger.log(Level.FINER, () -> "Getting data for union element " + selected + ": " + element.formatType());
        element.decode(types, buffer);
    }

    @Override
    public void encode(final ByteBuffer buffer) throws Exception
    {
        final int safe_sel = selected;
        // What's selected might now change, we encode what was in safe_sel
        PVASize.encodeSize(safe_sel, buffer);
        if (safe_sel >= 0)
            elements.get(safe_sel).encode(buffer);
    }

    @Override
    protected int update(final int index, final PVAData new_value, final BitSet changes) throws Exception
    {
        if (new_value instanceof PVAUnion)
        {
            final PVAUnion other = (PVAUnion) new_value;
            if (! Objects.equals(other.get(), get()))
            {
                if (other.elements.size() != elements.size())
                    throw new Exception("Incompatible unions");
                selected = other.selected;
                if (selected > 0)
                    elements.get(selected).setValue(other.get());
                changes.set(index);
            }
        }
        return index + 1;
    }

    @Override
    public void formatType(int level, StringBuilder buffer)
    {
        indent(level, buffer);
        if (union_name.isEmpty())
            buffer.append("union ");
        else
            buffer.append(union_name).append(" ");
        buffer.append(name);
        if (type_id > 0)
            buffer.append(" [#").append(type_id).append("]");
        for (PVAData element : elements)
        {
            buffer.append("\n");
            element.formatType(level+1, buffer);
        }
    }

    @Override
    protected void format(final int level, final StringBuilder buffer)
    {
        indent(level, buffer);
        if (union_name.isEmpty())
            buffer.append("union ");
        else
            buffer.append(union_name).append(" ");
        buffer.append(name);
        if (selected < 0)
        {
            buffer.append("\n");
            indent(level+1, buffer);
            buffer.append("- nothing selected -");
        }
        else
        {
            buffer.append("\n");
            elements.get(selected).format(level+1, buffer);
        }
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof PVAUnion))
            return false;
        final PVAUnion other = (PVAUnion) obj;
        // Compare the selected element, which may be null.
        // Equality of non-selected elements is ignored.
        return Objects.equals(other.get(), get());
    }
}
