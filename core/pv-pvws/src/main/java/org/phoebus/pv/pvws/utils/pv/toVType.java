package org.phoebus.pv.pvws.utils.pv;

import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.*;
import org.phoebus.pv.pvws.models.pv.PvwsData;

import java.text.NumberFormat;
import java.time.Instant;


public class toVType {

    public static VType convert(PvwsData pvData) {
        if (pvData == null || pvData.getValue() == null) {
            System.out.println("PV has null value: " + pvData.getPv());
            return null;
        }


        //meta data
        String pvName = pvData.getPv();
        String severityStr = pvData.getSeverity();
        String description = pvData.getDescription();
        String units = pvData.getUnits();
        String vType =  pvData.getVtype();

        //time stamp
        int precision = pvData.getPrecision();
        int seconds = pvData.getSeconds();
        int nanos = pvData.getNanos();

        //alarm data
        double min = pvData.getMin();
        double max = pvData.getMax();
        double warnLow = pvData.getWarn_low();
        double warnHigh = pvData.getWarn_high();
        double alarmLow = pvData.getAlarm_low();
        double alarmHigh = pvData.getAlarm_high();

        //PV value that was converted to type by json converter
        //this value was converted from string to might be incorrect
        Object value = pvData.getValue();

        Object typedValue = convertValueByVtype(value, vType);


        Alarm alarm = createAlarm(severityStr, description);
        Time time = createTime(seconds, nanos);
        Display display = createDisplay(min, max, warnLow, warnHigh, alarmLow, alarmHigh, precision, description, units);


        try {
            VType vValue = VType.toVType(typedValue, alarm, time, display);
            return vValue;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    public static Alarm createAlarm(String severityStr,  String description) {


        AlarmSeverity severity = (severityStr != null) ? AlarmSeverity.valueOf(severityStr) : AlarmSeverity.NONE;
        return Alarm.of(severity, AlarmStatus.NONE, description != null ? description : "");
    }

    public static Time createTime(int seconds, int nanos) {
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        return Time.of(instant);
    }

    public static Display createDisplay(double min, double max, double warnLow, double warnHigh, double alarmLow, double alarmHigh, int precision, String description, String units)
    {
        Range displayRange = Range.of(min, max);
        Range warningRange = Range.of(warnLow, warnHigh);
        Range alarmRange = Range.of(alarmLow, alarmHigh);
        Range controlRange = displayRange;
        NumberFormat numberFormat = NumberFormats.precisionFormat(precision != 0 ? precision : 2);
        String unitStr = (units != null) ? units : "";
        return Display.of(displayRange, alarmRange, warningRange, controlRange, unitStr, numberFormat, description);



    }

    public static Object convertValueByVtype(Object rawValue, String vtype) {
        if (rawValue == null || vtype == null) return null;

        try {
            switch (vtype) {
                case "VDouble":
                    if (rawValue instanceof Number) return ((Number) rawValue).doubleValue();
                    return Double.parseDouble(rawValue.toString());

                case "VFloat":
                    if (rawValue instanceof Number) return ((Number) rawValue).floatValue();
                    return Float.parseFloat(rawValue.toString());

                case "VInt":
                    if (rawValue instanceof Number) return ((Number) rawValue).intValue();
                    return Integer.parseInt(rawValue.toString());

                case "VLong":
                    if (rawValue instanceof Number) return ((Number) rawValue).longValue();
                    return Long.parseLong(rawValue.toString());

                case "VShort":
                    if (rawValue instanceof Number) return ((Number) rawValue).shortValue();
                    return Short.parseShort(rawValue.toString());

                case "VByte":
                    if (rawValue instanceof Number) return ((Number) rawValue).byteValue();
                    return Byte.parseByte(rawValue.toString());

                case "VBoolean":
                    if (rawValue instanceof Boolean) return rawValue;
                    return Boolean.parseBoolean(rawValue.toString());

                case "VString":
                    return rawValue.toString();

                // Add arrays here, e.g. VDoubleArray, VStringArray etc.

                default:
                    return rawValue;
            }
        } catch (Exception e) {
            return rawValue;
        }
    }






}
