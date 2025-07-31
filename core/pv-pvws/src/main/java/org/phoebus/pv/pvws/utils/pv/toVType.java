package org.phoebus.pv.pvws.utils.pv;

import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.*;

import java.text.NumberFormat;
import java.time.Instant;


public class toVType {

/* REFACTORED CODE STARTS HERE

    public static VType convert(PV pvObj) {
        if (pvObj == null || pvObj.getValue() == null) {
            System.out.println("PV has null value: " + pvObj.getPv());
            return null;
        }

        //Extract fields
        String pvName = pvObj.getPv();
        String severityStr = pvObj.getSeverity();
        String description = pvObj.getDescription();
        String units = pvObj.getUnits();
        int precision = pvObj.getPrecision();
        int seconds = pvObj.getSeconds();
        int nanos = pvObj.getNanos();
        double min = pvObj.getMin();
        double max = pvObj.getMax();
        double warnLow = pvObj.getWarn_low();
        double warnHigh = pvObj.getWarn_high();
        double alarmLow = pvObj.getAlarm_low();
        double alarmHigh = pvObj.getAlarm_high();


        Object value = pvObj.getValue();

        AlarmSeverity severity = (severityStr != null) ? AlarmSeverity.valueOf(severityStr) : AlarmSeverity.NONE;
        Alarm alarm = Alarm.of(severity, AlarmStatus.NONE, description != null ? description : "");

        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        Time time = Time.of(instant);

        Range displayRange = Range.of(min, max);
        Range warningRange = Range.of(warnLow, warnHigh);
        Range alarmRange = Range.of(alarmLow, alarmHigh);
        Range controlRange = displayRange;
        NumberFormat numberFormat = NumberFormats.precisionFormat(precision != 0 ? precision : 2);
        String unitStr = (units != null) ? units : "";
        Display display = Display.of(displayRange, alarmRange, warningRange, controlRange, unitStr, numberFormat, description);


        try {
            VType vValue = VType.toVType(value, alarm, time, display);
            return vValue;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }

        REFACTORED CODE ENDS HERE

        */

        /*if (vValue == null) {
            System.out.println("Could not convert PV to VType: " + pvName);
        } else {
            System.out.println("Converted to VType: " + vValue);
        }*/

        // Alarm
            /*Alarm alarm;
            try {
                AlarmSeverity severity = (severityStr != null) ? AlarmSeverity.valueOf(severityStr) : AlarmSeverity.NONE;
                alarm = Alarm.of(severity, AlarmStatus.NONE, description != null ? description : "");
            } catch (Exception e) {
                System.err.println("Alarm.none()");
                alarm = Alarm.none();
            }

            // Time
            Time time;
            try {
                Instant instant = Instant.ofEpochSecond(seconds, nanos);
                time = Time.of(instant);
            } catch (Exception e) {
                System.err.println("Time.now()");
                time = Time.now();
            }

            // Display
            Display display;
            try {
                Range displayRange = Range.of(min, max);
                Range warningRange = Range.of(warnLow, warnHigh);
                Range alarmRange = Range.of(alarmLow, alarmHigh);
                Range controlRange = displayRange;

                NumberFormat numberFormat = NumberFormats.precisionFormat(precision != 0 ? precision : 2);
                String unitStr = (units != null) ? units : "";

                display = Display.of(displayRange, alarmRange, warningRange, controlRange, unitStr, numberFormat, description);
            } catch (Exception e) {
                System.err.println("Display.none()");
                display = Display.none();
            }

            // VType Conversion
            VType vValue = VType.toVType(value, alarm, time, display);
            if (vValue == null) {
                System.out.println("Could not convert PV to VType: " + pvName);
            } else {
                System.out.println("Converted to VType: " + vValue);
            }

            return vValue;

        } catch (Exception e) {
            System.err.println("Failed to process 'update' message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }*/


    //}


}
