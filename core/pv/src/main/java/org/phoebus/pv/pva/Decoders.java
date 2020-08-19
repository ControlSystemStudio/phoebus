/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.epics.pva.data.PVAArray;
import org.epics.pva.data.PVAByte;
import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAFloat;
import org.epics.pva.data.PVAFloatArray;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVALong;
import org.epics.pva.data.PVALongArray;
import org.epics.pva.data.PVANumber;
import org.epics.pva.data.PVAShort;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.ArrayUByte;
import org.epics.util.array.ArrayUInteger;
import org.epics.util.array.ArrayULong;
import org.epics.util.array.ArrayUShort;
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
import org.epics.vtype.VType;
import org.epics.vtype.VUByte;
import org.epics.vtype.VUByteArray;
import org.epics.vtype.VUInt;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VULong;
import org.epics.vtype.VULongArray;
import org.epics.vtype.VUShort;
import org.epics.vtype.VUShortArray;

/** Decodes {@link Time}, {@link Alarm}, {@link Display}, ...
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Decoders
{
    private static final Instant NO_TIME = Instant.ofEpochSecond(0, 0);
    private static final Integer NO_USERTAG = Integer.valueOf(0);

    private static final Display noDisplay = Display.none();

    /** Cache for formats */
    private static final Map<String, NumberFormat> formatterCache =
            new ConcurrentHashMap<>();

    /** @param struct Structure
     *  @param name Name of Number field
     *  @param default_value Default number if field doesn't exist
     *  @return Double value
     */
    private static double getDoubleValue(final PVAStructure struct, final String name,
                                        final double default_value)
    {
        final PVANumber field = struct.get(name);
        if (field != null)
            return field.getNumber().doubleValue();
        else
            return default_value;
    }

    static Alarm decodeAlarm(final PVAStructure struct)
    {
        // Decode alarm_t alarm
        final AlarmSeverity severity;
        final AlarmStatus status;
        final String message;

        final PVAStructure alarm = struct.get("alarm");
        if (alarm != null)
        {
            PVAInt code = alarm.get("severity");
            severity = code == null
                     ? AlarmSeverity.UNDEFINED
                     : AlarmSeverity.values()[code.get()];

            code = alarm.get("status");
            status = code == null
                    ? AlarmStatus.UNDEFINED
                    : AlarmStatus.values()[code.get()];

            final PVAString msg = alarm.get("message");
            message = (msg == null || msg.get() == null) ? "<null>" : msg.get();
        }
        else
        {
            severity = AlarmSeverity.NONE;
            status = AlarmStatus.NONE;
            message = AlarmStatus.NONE.name();
        }
        return Alarm.of(severity, status, message);
    }

    static Time decodeTime(final PVAStructure struct)
    {
        // Decode time_t timeStamp
        final Instant timestamp;
        final Integer usertag;

        final PVAStructure time = struct.get("timeStamp");
        if (time != null)
        {
            final PVALong sec = time.get("secondsPastEpoch");
            final PVAInt nano = time.get("nanoseconds");
            if (sec == null || nano == null)
                timestamp = NO_TIME;
            else
                timestamp = Instant.ofEpochSecond(sec.get(), nano.get());
            final PVAInt user = time.get("userTag");
            usertag = user == null ? NO_USERTAG : user.get();
        }
        else
        {
            timestamp = NO_TIME;
            usertag = NO_USERTAG;
        }
        return Time.of(timestamp, usertag, timestamp.getEpochSecond() > 0);
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
    }

    private static Display decodeDisplay(final PVAStructure struct)
    {
        String units;
        NumberFormat format;
        Range display, control, alarm, warn;

        // Decode display_t display
        PVAStructure section = struct.get("display");
        if (section != null)
        {
            PVAString str = section.get("units");
            units = str == null ? noDisplay.getUnit() : str.get();

            // Since EPICS Base 7.0.2.2, qsrv supports 'precision' and 'form'
            final PVAInt prec = section.get("precision");
            if (prec != null)
            {
                final PVAStructure form = section.get("form");
                if (form != null)
                {
                    final PVAInt pv_idx = form.get("index");
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
                str = section.get("format");
                format = str == null
                    ? noDisplay.getFormat()
                    : createNumberFormat(str.get());
            }

            display = Range.of(getDoubleValue(section, "limitLow", Double.NaN),
                               getDoubleValue(section, "limitHigh", Double.NaN));
        }
        else
        {
            units = noDisplay.getUnit();
            format = noDisplay.getFormat();
            display = Range.undefined();
        }

        // Decode control_t control
        section = struct.get("control");
        if (section != null)
            control = Range.of(getDoubleValue(section, "limitLow", Double.NaN),
                               getDoubleValue(section, "limitHigh", Double.NaN));
        else
            control = Range.undefined();

        // Decode valueAlarm_t valueAlarm
        section = struct.get("valueAlarm");
        if (section != null)
        {
            alarm = Range.of(getDoubleValue(section, "lowAlarmLimit", Double.NaN),
                             getDoubleValue(section, "highAlarmLimit", Double.NaN));
            warn = Range.of(getDoubleValue(section, "lowWarningLimit", Double.NaN),
                            getDoubleValue(section, "highWarningLimit", Double.NaN));
        }
        else
            alarm = warn = Range.undefined();

        return Display.of(display, alarm, warn, control, units, format);
    }

    public static VType decodeString(PVAStructure struct, PVAString field)
    {
        return VString.of(field.get(), decodeAlarm(struct), decodeTime(struct));
    }

    public static VEnum decodeEnum(final PVAStructure struct) throws Exception
    {
        final Alarm alarm = decodeAlarm(struct);
        final Time time = decodeTime(struct);

        final PVAStructure section = struct.get("value");
        final int value = ((PVAInt)section.get("index")).get();
        final PVAStringArray choices = section.get("choices");

        return VEnum.of(value, EnumDisplay.of(choices.get()), alarm, time);
    }

    public static VType decodeDouble(final PVAStructure struct, final PVADouble field)
    {
        return VDouble.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeFloat(final PVAStructure struct, final PVAFloat field)
    {
        return VFloat.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeLong(final PVAStructure struct, final PVALong field)
    {
        if (field.isUnsigned())
            return VULong.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
        return VLong.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeInt(final PVAStructure struct, final PVAInt field)
    {
        if (field.isUnsigned())
            return VUInt.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
        return VInt.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeShort(final PVAStructure struct, final PVAShort field)
    {
        if (field.isUnsigned())
            return VUShort.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
        return VShort.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeByte(final PVAStructure struct, final PVAByte field)
    {
        if (field.isUnsigned())
            return VUByte.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
        return VByte.of(field.get(), decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeDoubleArray(final PVAStructure struct, final PVADoubleArray field)
    {
        return VDoubleArray.of(ArrayDouble.of(field.get()),
                               decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeFloatArray(final PVAStructure struct, final PVAFloatArray field)
    {
        return VFloatArray.of(ArrayFloat.of(field.get()),
                              decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeLongArray(final PVAStructure struct, final PVALongArray field)
    {
        if (field.isUnsigned())
            return VULongArray.of(ArrayULong.of(field.get()),
                                  decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
        else
            return VLongArray.of(ArrayLong.of(field.get()),
                                 decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeIntArray(final PVAStructure struct, final PVAIntArray field)
    {
        if (field.isUnsigned())
            return VUIntArray.of(ArrayUInteger.of(field.get()),
                                 decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
        else
            return VIntArray.of(ArrayInteger.of(field.get()),
                                decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeShortArray(final PVAStructure struct, final PVAShortArray field)
    {
        if (field.isUnsigned())
            return VUShortArray.of(ArrayUShort.of(field.get()),
                                   decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
        else
            return VShortArray.of(ArrayShort.of(field.get()),
                                  decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeByteArray(final PVAStructure struct, final PVAByteArray field)
    {
        if (field.isUnsigned())
            return VUByteArray.of(ArrayUByte.of(field.get()),
                                  decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
        else
            return VByteArray.of(ArrayByte.of(field.get()),
                                 decodeAlarm(struct), decodeTime(struct), decodeDisplay(struct));
    }

    public static VType decodeStringArray(final PVAStructure struct, final PVAStringArray field)
    {
        return VStringArray.of(Arrays.asList(field.get()), decodeAlarm(struct), decodeTime(struct));
    }

    public static VType decodeNumber(final PVAStructure struct, final PVANumber field) throws Exception
    {
        if (field instanceof PVADouble)
            return Decoders.decodeDouble(struct, (PVADouble) field);
        if (field instanceof PVAFloat)
            return Decoders.decodeFloat(struct, (PVAFloat) field);
        if (field instanceof PVALong)
            return Decoders.decodeLong(struct, (PVALong) field);
        if (field instanceof PVAInt)
            return Decoders.decodeInt(struct, (PVAInt) field);
        if (field instanceof PVAShort)
            return Decoders.decodeShort(struct, (PVAShort) field);
        if (field instanceof PVAByte)
            return Decoders.decodeByte(struct, (PVAByte) field);
        throw new Exception("Cannot handle " + field.getClass().getName());
    }

    public static VType decodeArray(final PVAStructure struct, final PVAArray field) throws Exception
    {
        if (field instanceof PVADoubleArray)
            return Decoders.decodeDoubleArray(struct, (PVADoubleArray) field);
        if (field instanceof PVAFloatArray)
            return Decoders.decodeFloatArray(struct, (PVAFloatArray) field);
        if (field instanceof PVALongArray)
            return Decoders.decodeLongArray(struct, (PVALongArray) field);
        if (field instanceof PVAIntArray)
            return Decoders.decodeIntArray(struct, (PVAIntArray) field);
        if (field instanceof PVAShortArray)
            return Decoders.decodeShortArray(struct, (PVAShortArray) field);
        if (field instanceof PVAByteArray)
            return Decoders.decodeByteArray(struct, (PVAByteArray) field);
        if (field instanceof PVAStringArray)
            return Decoders.decodeStringArray(struct, (PVAStringArray) field);
        throw new Exception("Cannot handle " + field.getClass().getName());
    }
}
