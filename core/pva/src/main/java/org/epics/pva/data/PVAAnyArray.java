/*
 *
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.epics.pva.data;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Level;

import static org.epics.pva.PVASettings.logger;

/** PV Access structure
 *
 *  <p>Holds one or more {@link PVAny} elements.
 */
public class PVAAnyArray extends PVADataWithID implements PVAArray{

    /**
     * Empty array constructor
     *  @param name Name
     */
    public PVAAnyArray(String name) {
        super(name);
    }

    /** Basic constructor
     *
     * @param name Name
     *  @param elements Initial elements
     */
    public PVAAnyArray(String name, final PVAny... elements) {
        super(name);
        this.elements = elements;
    }

    /** Unmodifiable list of elements.
     *
     *  <p>The value of each element may be updated, but
     *  no elements can be added, removed, replaced.
     */
    private volatile PVAny[] elements;

    /**
     * Set the array of elements
     *
     * @param elements Desired new set of elements */
    public void set(final PVAny[] elements) {
        this.elements = elements;
    }

    @Override
    public void setValue(Object new_value) throws Exception {
        if (new_value instanceof PVAAnyArray)
        {
            PVAAnyArray newValueArray = (PVAAnyArray) new_value;
            final PVAny[] other = newValueArray.elements;
            elements = Arrays.copyOf(other, other.length);
        }
        else if (new_value instanceof PVAny[])
            set(((PVAny[]) new_value));
        else
            throw new Exception("Cannot set " + formatType() + " to " + new_value);
    }

    /** @return Current value */
    public PVAny[] get()
    {
        return elements;
    }

    @Override
    public PVAData cloneType(String name) {
        return new PVAAnyArray(name);
    }

    @Override
    public PVAAnyArray cloneData() {
        final PVAny[] safe = elements;
        final PVAny[] copy = new PVAny[safe.length];
        // Deep copy
        for (int i=0; i<copy.length; ++i)
            copy[i] = safe[i].cloneData();
        return new PVAAnyArray(name, copy);
    }

    @Override
    public void encodeType(ByteBuffer buffer, BitSet described) throws Exception {
        final short type_id = getTypeID();
        if (type_id != 0) {
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

        // Encode 'any array'
        buffer.put((byte) (PVAComplex.FIELD_DESC_TYPE | PVAFieldDesc.Array.VARIABLE_SIZE.mask | PVAComplex.VARIANT_UNION));
    }

    @Override
    public void decode(PVATypeRegistry types, ByteBuffer buffer) throws Exception {

        final int count = PVASize.decodeSize(buffer);
        // Try to re-use elements
        PVAny[] new_elements = elements;
        if (new_elements == null  ||  new_elements.length != count)
            new_elements = new PVAny[count];
        for (int i=0; i<count; ++i)
        {   // Is this element non-null?
            final boolean non_null = PVABool.decodeBoolean(buffer);
            if (non_null)
            {   // Try to update existing element
                PVAny element = new_elements[i];
                if (element == null)
                    element = new PVAny("any");
                element.decode(types, buffer);
                new_elements[i] = element;
            }
            else
                new_elements[i] = null;
        }
        elements = new_elements;
        logger.log(Level.FINER, () -> "Decoded any[] element: " + elements);
    }

    @Override
    public void encode(ByteBuffer buffer) throws Exception {

        final PVAny[] copy = elements;
        PVASize.encodeSize(copy.length, buffer);
        for (PVAny pvAny : copy) {
            if (pvAny == null)
                PVABool.encodeBoolean(false, buffer);
            else {
                PVABool.encodeBoolean(true, buffer);
                pvAny.encode(buffer);
            }
        }
    }

    @Override
    protected int update(int index, PVAData new_value, BitSet changes) throws Exception {

        if (new_value instanceof PVAAnyArray)
        {
            final PVAAnyArray other = (PVAAnyArray) new_value;
            if (! Arrays.equals(other.elements, elements))
            {
                // Deep copy
                final PVAny[] copy = new PVAny[other.elements.length];
                for (int i=0; i<copy.length; ++i)
                    copy[i] = other.elements[i].cloneData();
                this.elements = copy;
                changes.set(index);
            }
        }
        return index + 1;
    }

    @Override
    protected void format(int level, StringBuilder buffer) {

        indent(level, buffer);
        buffer.append(getType());
        buffer.append(name);
        for (PVAData element : elements)
        {
            buffer.append("\n");
            element.format(level+1, buffer);
        }
    }

    @Override
    public String getType() {
        return "any[]";
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof PVAAnyArray))
            return false;
        final PVAAnyArray other = (PVAAnyArray) obj;
        return Arrays.equals(other.elements, elements);
    }
}
