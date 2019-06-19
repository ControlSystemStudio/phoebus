/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.nio.ByteBuffer;

/** 'Complex' PVA data helper
 *
 *  <p>Only used within the package to decode received data
 *  into e.g. {@link PVAStructure}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class PVAComplex
{
    public static final byte FIELD_DESC_TYPE = (byte)0b10000000;

    public static final byte BOUNDED_STRING = 0b011;
    public static final byte VARIANT_UNION  = 0b010;
    public static final byte UNION          = 0b001;
    public static final byte STRUCTURE      = 0b000;

    public static PVAData decodeType(final PVATypeRegistry types, final String name, final byte field_desc, final ByteBuffer buffer) throws Exception
    {
        final PVAFieldDesc.Array array = PVAFieldDesc.Array.forFieldDesc(field_desc);

        final byte cplx = (byte) (field_desc & 0b00000111);
        if (cplx == BOUNDED_STRING)
            throw new Exception("Cannot handle bounded string '" + name + "'");
        else if (cplx == VARIANT_UNION)
        {
            if (array == PVAFieldDesc.Array.SCALAR)
                return new PVAny(name);
            throw new Exception("Cannot handle " + array + " any '" + name + "'");
        }
        else if (cplx == UNION)
        {
            if (array == PVAFieldDesc.Array.SCALAR)
                return PVAUnion.decodeType(types, name, buffer);
            throw new Exception("Cannot handle " + array + " union '" + name + "'");
        }
        else if (cplx == STRUCTURE)
        {
            if (array == PVAFieldDesc.Array.SCALAR)
                return PVAStructure.decodeType(types, name, buffer);
            else if (array == PVAFieldDesc.Array.VARIABLE_SIZE)
                return PVAStructureArray.decodeType(types, name, buffer);
            throw new Exception("Cannot handle " + array + " structure '" + name + "'");
        }

        throw new Exception("Unknown 'complex' type " + String.format("%02X ", field_desc) + " '" + name + "'");
    }
}
