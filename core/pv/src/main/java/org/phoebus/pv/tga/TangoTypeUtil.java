package org.phoebus.pv.tga;


import org.epics.vtype.*;

import java.util.Objects;

public class TangoTypeUtil {

    public static VType convert(final Object value, Class<? extends VType> type) throws Exception
    {

        if (type.isInstance(value))
            return (VType) value;

        if (value instanceof VType)
            return (VType) value;

        if (type == VBoolean.class){
           return VBoolean.of((Boolean) value, Alarm.none(), Time.now());
        }

        if (type == VLong.class)
        {
            if (value instanceof Number)
                return VLong.of(((Number)value).longValue(), Alarm.none(), Time.now(), Display.none());
            return parseStringToVLong(Objects.toString(value));
        }

        if (type == VShort.class)
        {
            if (value instanceof Number)
                return VShort.of(((Number)value).longValue(), Alarm.none(), Time.now(), Display.none());
            return parseStringToVShort(Objects.toString(value));
        }

        if (type == VInt.class)
        {
            if (value instanceof Number)
                return VInt.of(((Number)value).longValue(), Alarm.none(), Time.now(), Display.none());
            return parseStringToVInt(Objects.toString(value));
        }

        if (type == VFloat.class)
        {
            if (value instanceof Number)
                return VFloat.of(((Number)value).longValue(), Alarm.none(), Time.now(), Display.none());
            return parseStringToVFloat(Objects.toString(value));
        }
        if (type == VDouble.class)
        {
            if (value instanceof Number)
                return VDouble.of((Number)value, Alarm.none(), Time.now(), Display.none());

            return parseStringToVDouble(Objects.toString(value));
        }

        if (type == VString.class)
            return parseStringToVString(Objects.toString(value));

        if (type == VByte.class){
            if (value instanceof Number)
                return VByte.of(((Number)value).longValue(), Alarm.none(), Time.now(), Display.none());
            return parseStringToVByte(Objects.toString(value));
        }
        throw new Exception("Expected type " + type.getSimpleName() + " but got " + value.getClass().getName());
    }

    private static VType parseStringToVByte(String value) throws Exception {
        try
        {
            return VByte.of(Double.parseDouble(value), Alarm.none(), Time.now(), Display.none());
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse VByte from '" + value + "'");
        }
    }

    private static VType parseStringToVString(String value) {
        return VString.of(stripQuotes(value), Alarm.none(), Time.now());
    }


    private static String stripQuotes(final String text)
    {
        if (text.length() < 2)
            return text;

        if ((text.charAt(0) == '"') && (text.charAt(text.length()-1) == '"'))
            return text.substring(1,text.length()-1);

        return text;
    }

    private static VType parseStringToVFloat(String value) throws Exception {
        try
        {
            return VFloat.of(Double.parseDouble(value), Alarm.none(), Time.now(), Display.none());
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse VFloat from '" + value + "'");
        }
    }

    private static VType parseStringToVDouble(String value) throws Exception {
        try
        {
            return VDouble.of(Double.parseDouble(value), Alarm.none(), Time.now(), Display.none());
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse VDouble from '" + value + "'");
        }
    }

    private static VType parseStringToVInt(String value) throws Exception {
        try
        {
            return VInt.of(Double.parseDouble(value), Alarm.none(), Time.now(), Display.none());
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse VInt from '" + value + "'");
        }

    }

    private static VType parseStringToVShort(String value) throws Exception {
        try
        {
            return VShort.of(Double.parseDouble(value), Alarm.none(), Time.now(), Display.none());
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse VShort from '" + value + "'");
        }
    }

    private static VType parseStringToVLong(String value) throws Exception {
        try
        {
            return VLong.of(Double.valueOf(value).longValue(), Alarm.none(), Time.now(), Display.none());
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse VLong from '" + value + "'");
        }
    }

    public static String ToString(  Object value ) throws Exception{
        //VType vType = convert(value, type);
        StringBuilder sb = new StringBuilder();

        if (value instanceof VString)
        {
            return sb.append("\"").append(((VString)value).getValue()).append("\"").toString();
        }
        if (value instanceof VBoolean)
        {
            return sb.append(((VBoolean)value).getValue()).toString();
        }
        if (value instanceof VNumber)
        {
            return sb.append(((VNumber)value).getValue()).toString();
        }

        throw new Exception ("Cannot change unknown type to String " + value.getClass().getName());
    }

}
