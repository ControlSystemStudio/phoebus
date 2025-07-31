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


        //Extract fields
        String pvName = pvData.getPv();
        String severityStr = pvData.getSeverity();
        String description = pvData.getDescription();
        String units = pvData.getUnits();
        int precision = pvData.getPrecision();
        int seconds = pvData.getSeconds();
        int nanos = pvData.getNanos();
        double min = pvData.getMin();
        double max = pvData.getMax();
        double warnLow = pvData.getWarn_low();
        double warnHigh = pvData.getWarn_high();
        double alarmLow = pvData.getAlarm_low();
        double alarmHigh = pvData.getAlarm_high();


        Object value = pvData.getValue();

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

        if (true)
            return VType.toVType(value);


        try {
            VType vValue = VType.toVType(value, alarm, time, display);
            return vValue;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }




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
