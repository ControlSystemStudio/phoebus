/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import org.phoebus.vtype.VType;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_STS_Enum;
import gov.aps.jca.dbr.DBR_String;
import gov.aps.jca.dbr.DBR_TIME_Byte;
import gov.aps.jca.dbr.DBR_TIME_Double;
import gov.aps.jca.dbr.DBR_TIME_Enum;
import gov.aps.jca.dbr.DBR_TIME_Float;
import gov.aps.jca.dbr.DBR_TIME_Int;
import gov.aps.jca.dbr.DBR_TIME_Short;
import gov.aps.jca.dbr.GR;
import gov.aps.jca.dbr.LABELS;

/** Helper for handling DBR types
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DBRHelper
{
    /** @return CTRL_... type for this channel. */
    public static DBRType getCtrlType(final boolean plain, final DBRType type)
    {
        if (type.isDOUBLE())
            return plain ? DBRType.DOUBLE : DBRType.CTRL_DOUBLE;
        else if (type.isFLOAT())
            return plain ? DBRType.FLOAT : DBRType.CTRL_DOUBLE;
        else if (type.isINT())
            return plain ? DBRType.INT : DBRType.CTRL_INT;
        else if (type.isSHORT())
            return plain ? DBRType.SHORT : DBRType.CTRL_INT;
        else if (type.isBYTE())
            return plain ? DBRType.BYTE : DBRType.CTRL_BYTE;
        else if (type.isENUM())
            return plain ? DBRType.SHORT : DBRType.CTRL_ENUM;
        // default: get as string
        return plain ? DBRType.STRING : DBRType.CTRL_STRING;
    }

    /** @return TIME_... type for this channel. */
    public static DBRType getTimeType(final boolean plain, final DBRType type)
    {
        if (type.isDOUBLE())
            return plain ? DBRType.DOUBLE : DBRType.TIME_DOUBLE;
        else if (type.isFLOAT())
            return plain ? DBRType.FLOAT : DBRType.TIME_FLOAT;
        else if (type.isINT())
            return plain ? DBRType.INT : DBRType.TIME_INT;
        else if (type.isSHORT())
            return plain ? DBRType.SHORT : DBRType.TIME_SHORT;
        else if (type.isENUM())
            return plain ? DBRType.SHORT : DBRType.TIME_ENUM;
        else if (type.isBYTE())
            return plain ? DBRType.BYTE: DBRType.TIME_BYTE;
        // default: get as string
        return plain ? DBRType.STRING : DBRType.TIME_STRING;
    }

    public static VType decodeValue(final boolean is_array, final Object metadata, final DBR dbr) throws Exception
    {
        // Rough guess, but somewhat in order of most frequently used type
        if (dbr instanceof DBR_TIME_Double)
        {
            if (is_array)
                return new VTypeForDoubleArray((GR) metadata, (DBR_TIME_Double) dbr);
            return new VTypeForDouble((GR) metadata, (DBR_TIME_Double) dbr);
        }

        if (dbr instanceof DBR_String)
        {
            if (is_array)
                return new VTypeForStringArray((DBR_String) dbr);
            else
                return new VTypeForString((DBR_String) dbr);
        }

        if (dbr instanceof DBR_TIME_Enum)
        {
            final LABELS enum_meta = (metadata instanceof LABELS) ? (LABELS) metadata : null;
            if (is_array)
                return new VTypeForEnumArray(enum_meta, (DBR_TIME_Enum) dbr);
            return new VTypeForEnum(enum_meta, (DBR_TIME_Enum) dbr);
        }

        // Metadata monitor will provide DBR_CTRL_Enum, which is a DBR_STS_Enum, but lacks time stamp
        if (dbr instanceof DBR_STS_Enum)
        {
            final DBR_STS_Enum have = (DBR_STS_Enum) dbr;
            final DBR_TIME_Enum need = new DBR_TIME_Enum(have.getEnumValue());
            need.setStatus(have.getStatus());
            need.setSeverity(have.getSeverity());

            final LABELS enum_meta = (metadata instanceof LABELS) ? (LABELS) metadata : null;

            if (is_array)
                return new VTypeForEnumArray(enum_meta, need);
            return new VTypeForEnum(enum_meta, need);
        }

        if (dbr instanceof DBR_TIME_Float)
        {
            if (is_array)
                return new VTypeForFloatArray((GR) metadata, (DBR_TIME_Float) dbr);
            return new VTypeForFloat((GR) metadata, (DBR_TIME_Float) dbr);
        }

        if (dbr instanceof DBR_TIME_Int)
        {
            if (is_array)
                return new VTypeForIntArray((GR) metadata, (DBR_TIME_Int) dbr);
            return new VTypeForInt((GR) metadata, (DBR_TIME_Int) dbr);
        }

        if (dbr instanceof DBR_TIME_Short)
        {
            if (is_array)
                return new VTypeForShortArray((GR) metadata, (DBR_TIME_Short) dbr);
           return new VTypeForShort((GR) metadata, (DBR_TIME_Short) dbr);
        }

        if (dbr instanceof DBR_TIME_Byte)
        {
            if (is_array)
                return new VTypeForByteArray((GR) metadata, (DBR_TIME_Byte) dbr);
           return new VTypeForByte((GR) metadata, (DBR_TIME_Byte) dbr);
        }

        throw new Exception("Cannot handle " + dbr.getClass().getName());
    }
}
