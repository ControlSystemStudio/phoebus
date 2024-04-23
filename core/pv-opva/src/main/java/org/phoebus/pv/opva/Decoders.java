/*******************************************************************************
 * Copyright (c) 2014-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.opva;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
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
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VShort;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;

/** Decodes {@link Time} and {@link Alarm}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class Decoders
{
    private static final Instant NO_TIME = Instant.ofEpochSecond(0, 0);
    private static final Integer NO_USERTAG = Integer.valueOf(0);

    private static final Display noDisplay = Display.none();

    /** Cache for formats */
    private static final Map<String, NumberFormat> formatterCache =
            new ConcurrentHashMap<>();

    static
    {
        // decodeAlarm() relies on org.epics.vtype and
        // org.epics.pvdata.property using the same values
        // for AlarmSeverity & AlarmStatus
        String vtype = Arrays.toString(org.epics.vtype.AlarmSeverity.values());
        String pva = Arrays.toString(org.epics.pvdata.property.AlarmSeverity.values());
        if (! vtype.equals(pva))
            throw new IllegalStateException("AlarmSeverity enum mismatch\nVType: " + vtype + "\nPVA  : " + pva);

        vtype = Arrays.toString(org.epics.vtype.AlarmStatus.values());
        pva = Arrays.toString(org.epics.pvdata.property.AlarmStatus.values());
        if (! vtype.equals(pva))
            throw new IllegalStateException("AlarmStatus enum mismatch\nVType: " + vtype + "\nPVA  : " + pva);
    }

    public static Time decodeTime(final PVStructure struct)
    {
        // Decode time_t timeStamp
        final Instant timestamp;
        final Integer usertag;

        final PVStructure time = struct.getSubField(PVStructure.class, "timeStamp");
        if (time != null)
        {
            final PVLong sec = time.getSubField(PVLong.class, "secondsPastEpoch");
            final PVInt nano = time.getSubField(PVInt.class, "nanoseconds");
            if (sec == null || nano == null)
                timestamp = NO_TIME;
            else
                timestamp = Instant.ofEpochSecond(sec.get(), nano.get());
            final PVInt user = time.getSubField(PVInt.class, "userTag");
            usertag = user == null ? NO_USERTAG : user.get();
        }
        else
        {
            timestamp = NO_TIME;
            usertag = NO_USERTAG;
        }
        return Time.of(timestamp, usertag, timestamp.getEpochSecond() > 0);
    }

    public static Alarm decodeAlarm(final PVStructure struct)
    {
        // Decode alarm_t alarm
        final AlarmSeverity severity;
        final AlarmStatus status;
        final String message;

        final PVStructure alarm = struct.getSubField(PVStructure.class, "alarm");
        if (alarm != null)
        {
            PVInt code = alarm.getSubField(PVInt.class, "severity");
            severity = code == null
                ? AlarmSeverity.UNDEFINED
                : AlarmSeverity.values()[code.get()];

            code = alarm.getSubField(PVInt.class, "status");
            status = code == null
                ? AlarmStatus.UNDEFINED
                : AlarmStatus.values()[code.get()];

            final PVString msg = alarm.getSubField(PVString.class, "message");
            message = msg == null
                ? "<null>"
                : msg.get();
        }
        else
        {
            severity = AlarmSeverity.NONE;
            status = AlarmStatus.NONE;
            message = AlarmStatus.NONE.name();
        }
        return Alarm.of(severity, status, message);
    }

    /** @param printfFormat Format from NTScalar display.format
     *  @return Suitable NumberFormat
     */
    private static NumberFormat createNumberFormat(final String printfFormat)
    {
        if (printfFormat == null ||
            printfFormat.trim().isEmpty() ||
            printfFormat.equals("%s"))
            return noDisplay.getFormat();
        else
        {
            NumberFormat formatter = formatterCache.get(printfFormat);
            if (formatter != null)
                return formatter;
            formatter = new PrintfFormat(printfFormat);
            formatterCache.put(printfFormat, formatter);
            return formatter;
        }
    }

    static class PrintfFormat extends java.text.NumberFormat
    {
        private static final long serialVersionUID = 1L;
        private final String format;
        public PrintfFormat(final String printfFormat)
        {
            // probe format
            boolean allOK = true;
            try {
                String.format(printfFormat, 0.0);
            } catch (Throwable th) {
                allOK = false;
            }
            // accept it if all is OK
            this.format = allOK ? printfFormat : null;
        }

        private final String internalFormat(double number)
        {
            if (format != null)
                return String.format(format, number);
            else
                return String.valueOf(number);
        }

        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
        {
            toAppendTo.append(internalFormat(number));
            return toAppendTo;
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
        {
            toAppendTo.append(internalFormat(number));
            return toAppendTo;
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition)
        {
            throw new UnsupportedOperationException("No parsing.");
        }
    };

    public static Display decodeDisplay(final PVStructure struct) throws Exception
    {
        String units;
        NumberFormat format;
        Range display, control, alarm, warn;

        // Decode display_t display
        PVStructure section = struct.getSubField(PVStructure.class, "display");
        if (section != null)
        {
            PVString str = section.getSubField(PVString.class, "units");
            units = str == null ? noDisplay.getUnit() : str.get();

            // Since EPICS Base 7.0.2.2, qsrv supports 'precision' and 'form'
            final PVInt prec = section.getSubField(PVInt.class, "precision");
            if (prec != null)
            {
                final PVStructure form = section.getSubField(PVStructure.class, "form");
                if (form != null)
                {
                    final PVInt pv_idx = form.getSubField(PVInt.class, "index");
                    final int idx = pv_idx == null ? 0 : pv_idx.get();
                    // idx = ["Default", "String", "Binary", "Decimal", "Hex", "Exponential", "Engineering"]
                    // XXX VType doesn't offer a good way to pass the 'form' options on.
                    //     This format is later mostly ignored, only precision is recovered.
                    switch (idx)
                    {
                    case 4:
                        format = createNumberFormat("0x%X");
                        break;
                    case 5:
                    case 6:
                        format = createNumberFormat("%." + prec.get() + "E");
                        break;
                    default:
                        format = NumberFormats.precisionFormat(prec.get());
                    }
                }
                else
                    format = NumberFormats.precisionFormat(prec.get());
            }
            else
            {
                // Earlier PV servers sent 'format' string
                str = section.getSubField(PVString.class, "format");
                format = str == null
                    ? noDisplay.getFormat()
                    : createNumberFormat(str.get());
            }

            display = Range.of(PVStructureHelper.getDoubleValue(section, "limitLow", Double.NaN),
                               PVStructureHelper.getDoubleValue(section, "limitHigh", Double.NaN));
        }
        else
        {
            units = noDisplay.getUnit();
            format = noDisplay.getFormat();
            display = Range.undefined();
        }

        // Decode control_t control
        section = struct.getSubField(PVStructure.class, "control");
        if (section != null)
            control = Range.of(PVStructureHelper.getDoubleValue(section, "limitLow", Double.NaN),
                               PVStructureHelper.getDoubleValue(section, "limitHigh", Double.NaN));
        else
            control = Range.undefined();

        // Decode valueAlarm_t valueAlarm
        section = struct.getSubField(PVStructure.class, "valueAlarm");
        if (section != null)
        {
            alarm = Range.of(PVStructureHelper.getDoubleValue(section, "lowAlarmLimit", Double.NaN),
                             PVStructureHelper.getDoubleValue(section, "highAlarmLimit", Double.NaN));
            warn = Range.of(PVStructureHelper.getDoubleValue(section, "lowWarningLimit", Double.NaN),
                            PVStructureHelper.getDoubleValue(section, "highWarningLimit", Double.NaN));
        }
        else
            alarm = warn = Range.undefined();

        return Display.of(display, alarm, warn, control, units, format);
    }


    public static VEnum decodeEnum(final PVStructure struct) throws Exception
    {
        final Alarm alarm = Decoders.decodeAlarm(struct);
        final Time time = Decoders.decodeTime(struct);

        final PVStructure section = struct.getSubField(PVStructure.class, "value");
        final int value = section.getSubField(PVInt.class, "index").get();
        final List<String> labels = PVStructureHelper.getStrings(section, "choices");

        return VEnum.of(value, EnumDisplay.of(labels), alarm, time);
    }

    public static VDouble decodeDouble(final PVStructure struct) throws Exception
    {
        return VDouble.of(PVStructureHelper.convert.toDouble(struct.getSubField(PVScalar.class, "value")),
                          decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VFloat decodeFloat(final PVStructure struct) throws Exception
    {
        return VFloat.of(struct.getSubField(PVFloat.class, "value").get(),
                         decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VLong decodeLong(final PVStructure struct) throws Exception
    {
        return VLong.of(PVStructureHelper.convert.toLong(struct.getSubField(PVScalar.class, "value")),
                       decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VInt decodeInt(final PVStructure struct) throws Exception
    {
        return VInt.of(PVStructureHelper.convert.toInt(struct.getSubField(PVScalar.class, "value")),
                       decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VShort decodeShort(final PVStructure struct) throws Exception
    {
        return VShort.of(PVStructureHelper.convert.toShort(struct.getSubField(PVScalar.class, "value")),
                         decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VByte decodeByte(final PVStructure struct) throws Exception
    {
        return VByte.of(PVStructureHelper.convert.toByte(struct.getSubField(PVScalar.class, "value")),
                        decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VString decodeString(final PVStructure struct) throws Exception
    {
        return VString.of(struct.getSubField(PVString.class, "value").get(),
                          decodeAlarm(struct), decodeTime(struct));
    }

    public static VDoubleArray decodeDoubleArray(final PVStructure struct) throws Exception
    {
        final PVScalarArray pv_array = struct.getSubField(PVScalarArray.class, "value");
        final int length = pv_array.getLength();
        final double[] data = new double[length];
        PVStructureHelper.convert.toDoubleArray(pv_array, 0, length, data, 0);
        return VDoubleArray.of(ArrayDouble.of(data),
                               decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VFloatArray decodeFloatArray(final PVStructure struct) throws Exception
    {
        final PVScalarArray pv_array = struct.getSubField(PVScalarArray.class, "value");
        final int length = pv_array.getLength();
        final float[] data = new float[length];
        PVStructureHelper.convert.toFloatArray(pv_array, 0, length, data, 0);
        return VFloatArray.of(ArrayFloat.of(data),
                              decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VLongArray decodeLongArray(final PVStructure struct) throws Exception
    {
        final PVScalarArray pv_array = struct.getSubField(PVScalarArray.class, "value");
        final int length = pv_array.getLength();
        final long[] data = new long[length];
        PVStructureHelper.convert.toLongArray(pv_array, 0, length, data, 0);
        return VLongArray.of(ArrayLong.of(data),
                             decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VIntArray decodeIntArray(final PVStructure struct) throws Exception
    {
        final PVScalarArray pv_array = struct.getSubField(PVScalarArray.class, "value");
        final int length = pv_array.getLength();
        final int[] data = new int[length];
        PVStructureHelper.convert.toIntArray(pv_array, 0, length, data, 0);
        return VIntArray.of(ArrayInteger.of(data),
                            decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VShortArray decodeShortArray(final PVStructure struct) throws Exception
    {
        final PVScalarArray pv_array = struct.getSubField(PVScalarArray.class, "value");
        final int length = pv_array.getLength();
        final short[] data = new short[length];
        PVStructureHelper.convert.toShortArray(pv_array, 0, length, data, 0);
        return VShortArray.of(ArrayShort.of(data),
                              decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VByteArray decodeByteArray(final PVStructure struct) throws Exception
    {
        final PVScalarArray pv_array = struct.getSubField(PVScalarArray.class, "value");
        final int length = pv_array.getLength();
        final byte[] data = new byte[length];
        PVStructureHelper.convert.toByteArray(pv_array, 0, length, data, 0);
        return VByteArray.of(ArrayByte.of(data),
                             decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VStringArray decodeStringArray(final PVStructure struct) throws Exception
    {
        return VStringArray.of(PVStructureHelper.getStrings(struct, "value"),
                               decodeAlarm(struct), decodeTime(struct));
    }
}
