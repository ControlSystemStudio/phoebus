package org.phoebus.archive.vtype;

import org.epics.util.stats.Range;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.*;

public class VTypeHelperTest {

    private final Range displayRange = Range.of(-1000, 1000);
    private final Range controlRange = Range.of(-1000, 1000);

    /**
     * Tests that with all four limits set to {@link Double#NaN} or zero, alarm limits are considered to not be defined.
     */
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, 0.0})
    public void testHasAlarmLimits1(double value){
        Range alarmRange = Range.of(value, value);
        Range warningRange = Range.of(value, value);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertFalse(VTypeHelper.hasAlarmLimits(vDouble));
    }

    /**
     * Tests that with at least one limit other than zero, all others may be zero and still
     * evaluate as having alarm limit.
     */
    @Test
    public void testHasAlarmLimits3(){
        Range alarmRange = Range.of(Double.NaN, 0.0);
        Range warningRange = Range.of(0.0, 0.0);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertFalse(VTypeHelper.hasAlarmLimits(vDouble));
    }

    /**
     * Tests that with at least one limit other than zero, all others may be zero and still
     * evaluate as having alarm limit.
     */
    @Test
    public void testHasAlarmLimits4(){
        Range alarmRange = Range.of(10.0, 0.0);
        Range warningRange = Range.of(0.0, 0.0);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertTrue(VTypeHelper.hasAlarmLimits(vDouble));
    }

    /**
     * Tests that if either alarm or warning range are clamped at zero and the other has limits {@link Double#NaN},
     * the check evaluates to not define alarm limits
     */
    @Test
    public void testHasAlarmLimits5(){
        Range alarmRange = Range.of(Double.NaN, Double.NaN);
        Range warningRange = Range.of(0.0, 0.0);
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.of(displayRange, alarmRange, warningRange, controlRange, "", NumberFormat.getNumberInstance()));

        assertFalse(VTypeHelper.hasAlarmLimits(vDouble));
    }

    /**
     * Tests that {@link Alarm#NONE} does not define alarm limits.
     */
    @Test
    public void testHasAlarmLimits6(){
        VDouble vDouble = VDouble.of(7.1, Alarm.none(), Time.now(), Display.none());
        assertFalse(VTypeHelper.hasAlarmLimits(vDouble));
    }
}
