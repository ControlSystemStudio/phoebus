/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import static org.phoebus.pv.PV.logger;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;

import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayShort;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VByte;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VShort;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.pv.TimeHelper;

import gov.aps.jca.dbr.CTRL;
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
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;

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

    private static Alarm convertAlarm(final DBR dbr)
    {
        if (dbr == null)
        {
            // Not expected, but null indicates
            // that there is no value, i.e. disconnected.
            return Alarm.disconnected();
        }
        else if (! (dbr instanceof STS))
        {
            // Called with a valid DBR that carries no alarm information: OK.
            // Example scenario is reading record.RTYP, which sends plain DBR_STRING.
            return Alarm.none();
        }

        final STS sts = (STS) dbr;

        final AlarmSeverity severity;

        if (sts.getSeverity() == Severity.NO_ALARM)
            severity = AlarmSeverity.NONE;
        else if (sts.getSeverity() == Severity.MINOR_ALARM)
            severity = AlarmSeverity.MINOR;
        else if (sts.getSeverity() == Severity.MAJOR_ALARM)
            severity = AlarmSeverity.MAJOR;
        else if (sts.getSeverity() == Severity.INVALID_ALARM)
            severity = AlarmSeverity.INVALID;
        else
            severity = AlarmSeverity.UNDEFINED;

        return Alarm.of(severity, AlarmStatus.NONE, sts.getStatus().getName());
    }

    private static Time convertTime(final DBR dbr)
    {
        if (! (dbr instanceof TIME))
            return Time.nowInvalid();

        final TimeStamp epics_time = ((TIME)dbr).getTimeStamp();
        if (epics_time == null)
            return Time.nowInvalid();

        final Instant instant = Instant.ofEpochSecond(epics_time.secPastEpoch() + 631152000L,  (int) epics_time.nsec());
        if (epics_time.secPastEpoch() <= 0)
            return Time.of(instant, 0, false);

        return TimeHelper.fromInstant(instant);
    }

    private static Display convertDisplay(final Object dbr)
    {
        if (! (dbr instanceof GR))
            return Display.none();

        final GR metadata = (GR) dbr;

        final NumberFormat format;
        if (dbr instanceof PRECISION)
        {
            final int precision = ((PRECISION) dbr).getPrecision();
            if (precision >= 0)
                format = NumberFormats.precisionFormat(precision);
            else
                format = NumberFormats.toStringFormat();
        }
        else
            format = NumberFormats.precisionFormat(0);

        final Range display = Range.of(metadata.getLowerDispLimit().doubleValue(),
                                       metadata.getUpperDispLimit().doubleValue());
        final Range control;
        if (dbr instanceof CTRL)
            control = Range.of(((CTRL) dbr).getLowerCtrlLimit().doubleValue(),
                               ((CTRL) dbr).getUpperCtrlLimit().doubleValue());
        else
            control = display;

        return Display.of(display,
                Range.of(metadata.getLowerAlarmLimit().doubleValue(), metadata.getUpperAlarmLimit().doubleValue()),
                Range.of(metadata.getLowerWarningLimit().doubleValue(), metadata.getUpperWarningLimit().doubleValue()),
                control,
                metadata.getUnits(),
                format);
    }

    public static VType decodeValue(final boolean is_array, final Object metadata, final DBR dbr) throws Exception
    {
        // Rough guess, but somewhat in order of most frequently used type
        if (dbr instanceof DBR_TIME_Double)
        {
            final DBR_TIME_Double xx = (DBR_TIME_Double) dbr;
            if (is_array)
                return VDoubleArray.of(ArrayDouble.of(xx.getDoubleValue()), convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
            return VDouble.of(xx.getDoubleValue()[0], convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
        }

        if (dbr instanceof DBR_String)
        {
            final DBR_String xx = (DBR_String) dbr;
            if (is_array)
                return VStringArray.of(Arrays.asList(xx.getStringValue()), convertAlarm(xx), convertTime(xx));
            else
                return VString.of(xx.getStringValue()[0], convertAlarm(xx), convertTime(xx));
        }

        if (dbr instanceof DBR_TIME_Enum)
        {
            final DBR_TIME_Enum xx = (DBR_TIME_Enum) dbr;
            final EnumDisplay enum_meta;
            if (metadata instanceof LABELS)
                enum_meta = EnumDisplay.of(((LABELS) metadata).getLabels());
            else
                enum_meta = EnumDisplay.of();
            try
            {
                if (is_array)
                    return VEnumArray.of(ArrayShort.of(xx.getEnumValue()), enum_meta, convertAlarm(dbr), convertTime(dbr));
                return VEnum.of(xx.getEnumValue()[0], enum_meta, convertAlarm(dbr), convertTime(dbr));
            }
            catch (IndexOutOfBoundsException ex)
            {
                final short index = xx.getEnumValue()[0];
                logger.log(Level.WARNING, "Invalid enum index " + index + " for PV with enum options " + enum_meta.getChoices());
                return VShort.of(index, convertAlarm(dbr), convertTime(dbr), Display.none());
            }
        }

        // Metadata monitor will provide DBR_CTRL_Enum, which is a DBR_STS_Enum, but lacks time stamp
        if (dbr instanceof DBR_STS_Enum)
        {
            final DBR_STS_Enum have = (DBR_STS_Enum) dbr;
            final DBR_TIME_Enum need = new DBR_TIME_Enum(have.getEnumValue());
            need.setStatus(have.getStatus());
            need.setSeverity(have.getSeverity());

            final EnumDisplay enum_meta = (metadata instanceof LABELS)
                ? EnumDisplay.of(((LABELS) metadata).getLabels())
                : EnumDisplay.of();

            if (is_array)
                return VEnumArray.of(new ArrayInteger(ArrayShort.of(need.getEnumValue())), enum_meta, convertAlarm(need), convertTime(need));
            return VEnum.of(need.getEnumValue()[0], enum_meta, convertAlarm(need), convertTime(need));
        }

        if (dbr instanceof DBR_TIME_Float)
        {
            final DBR_TIME_Float xx = (DBR_TIME_Float) dbr;
            if (is_array)
                return VFloatArray.of(ArrayFloat.of(xx.getFloatValue()), convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
            return VFloat.of(xx.getFloatValue()[0], convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
        }

        if (dbr instanceof DBR_TIME_Int)
        {
            final DBR_TIME_Int xx = (DBR_TIME_Int) dbr;
            if (is_array)
                return VIntArray.of(ArrayInteger.of(xx.getIntValue()), convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
            return VInt.of(xx.getIntValue()[0], convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
        }

        if (dbr instanceof DBR_TIME_Short)
        {
            final DBR_TIME_Short xx = (DBR_TIME_Short) dbr;
            if (is_array)
                return VShortArray.of(ArrayShort.of(xx.getShortValue()), convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
            return VShort.of(xx.getShortValue()[0], convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
        }

        if (dbr instanceof DBR_TIME_Byte)
        {
            final DBR_TIME_Byte xx = (DBR_TIME_Byte) dbr;
            if (is_array)
                return VByteArray.of(ArrayByte.of(xx.getByteValue()), convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
            return VByte.of(xx.getByteValue()[0], convertAlarm(dbr), convertTime(dbr), convertDisplay(metadata));
        }

        throw new Exception("Cannot handle " + dbr.getClass().getName());
    }
}
