/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import static org.epics.pva.PVASettings.logger;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** PVA Type Registry
 *
 *  <p>Decodes field descriptions from buffer.
 *  When a type ID is provided, the type is remembered
 *  for later re-use.
 *
 *  <p>Type IDs are specific to each TCP connection
 *  between PVA server and client.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVATypeRegistry
{
    private final ConcurrentHashMap<Short, PVAData> types = new ConcurrentHashMap<>();

    /** Decode a 'Field'
     *  @param name Name for the decoded field
     *  @param buffer Buffer that's positioned on field encoding
     *  @return PVAData
     *  @throws Exception on error
     */
    public PVAData decodeType(final String name, final ByteBuffer buffer) throws Exception
    {
        final byte field_desc = buffer.get();

        if (field_desc == PVAFieldDesc.FULL_WITH_ID_TYPE_CODE)
        {
            final short type_id = buffer.getShort();
            final PVAData type = decodeType(name, buffer);
            if (type instanceof PVADataWithID)
                ((PVADataWithID)type).setTypeID(type_id);
            logger.log(Level.FINEST, "Type ID " + type_id + ": " + type.formatType());
            // Remember type
            types.put(type_id, type.cloneType(name));
            return type;
        }
        else if (field_desc == PVAFieldDesc.FULL_TAGGED_ID_TYPE_CODE)
        {
            final short type_id = buffer.getShort();
            final int tag = buffer.getInt();
            // Type_id and tag could be used to check if this
            // combination is known, eliminating need to decode type.
            // For now ignoring the tag, always decoding the type.
            final PVAData type = decodeType(name, buffer);
            if (type instanceof PVADataWithID)
                ((PVADataWithID)type).setTypeID(type_id);
            logger.log(Level.FINEST, "Type ID " + type_id + ", tag " + tag + ": " + type.formatType());
            // Remember type
            types.put(type_id, type.cloneType(name));
            return type;
        }
        else if (field_desc == PVAFieldDesc.ONLY_ID_TYPE_CODE)
        {
            final short type_id = buffer.getShort();
            final PVAData type = types.get(type_id);
            if (type == null)
                throw new Exception("Unknown FieldDesc Type ID " + type_id);
            if (type instanceof PVADataWithID)
                ((PVADataWithID)type).setTypeID(type_id);
            logger.log(Level.FINEST, "Re-using Type ID " + type_id);
            return type.cloneType(name);
        }
        else if (field_desc == PVAFieldDesc.NULL_TYPE_CODE)
            return null;
        else if (Byte.toUnsignedInt(field_desc) - Byte.toUnsignedInt(PVAFieldDesc.FULL_TYPE_CODE_MAX) <= 0)
        {   // FULL_TYPE_CODE
            final byte type = (byte) (field_desc & 0b11100000);
            if (type == PVAComplex.FIELD_DESC_TYPE)
                return PVAComplex.decodeType(this, name, field_desc, buffer);
            else if (type == PVAFloat.FIELD_DESC_TYPE)
                return PVAFloat.decodeType(name, field_desc, buffer);
            else if (type == PVAInt.FIELD_DESC_TYPE)
                return PVAInt.decodeType(name, field_desc, buffer);
            else if (type == PVAString.FIELD_DESC_TYPE)
                return PVAString.decodeType(name, field_desc, buffer);
            else if (type == PVABool.FIELD_DESC_TYPE)
                return PVABool.decodeType(name, field_desc, buffer);
        }

        throw new Exception("Cannot decode " + name + " FieldDesc 0x" + String.format("%02X ", field_desc));
    }
}
