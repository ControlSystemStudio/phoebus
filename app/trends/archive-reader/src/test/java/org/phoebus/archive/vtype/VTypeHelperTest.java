package org.phoebus.archive.vtype;

import org.epics.util.stats.Range;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.junit.jupiter.api.Test;

import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.*;

public class VTypeHelperTest {

    private final Range displayRange = Range.of(-1000, 1000);
    private final Range controlRange = Range.of(-1000, 1000);

    @Test
    public void testHasAlarmLimits1(){
        Range alarmRange = Range.of(Double.NaN, Double.NaN);
        Range warningRange = Range.of(Double.NaN, Double.NaN);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertFalse(VTypeHelper.hasAlarmLimits(vDouble));
    }

    @Test
    public void testHasAlarmLimits2(){
        Range alarmRange = Range.of(0.0, 0.0);
        Range warningRange = Range.of(0.0, 0.0);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertFalse(VTypeHelper.hasAlarmLimits(vDouble));
    }

    @Test
    public void testHasAlarmLimits3(){
        Range alarmRange = Range.of(Double.NaN, 0.0);
        Range warningRange = Range.of(0.0, 0.0);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertTrue(VTypeHelper.hasAlarmLimits(vDouble));
    }

    @Test
    public void testHasAlarmLimits4(){
        Range alarmRange = Range.of(0.0, 10.0);
        Range warningRange = Range.of(0.0, 0.0);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertTrue(VTypeHelper.hasAlarmLimits(vDouble));
    }

    @Test
    public void testHasAlarmLimits5(){
        Range alarmRange = Range.of(Double.NaN, Double.NaN);
        Range warningRange = Range.of(0.0, 0.0);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertFalse(VTypeHelper.hasAlarmLimits(vDouble));
    }

    @Test
    public void testHasAlarmLimits6(){
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.none());

        assertFalse(VTypeHelper.hasAlarmLimits(vDouble));
    }
}
