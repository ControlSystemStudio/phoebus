package org.phoebus.core.vtypes;

import static org.junit.Assert.*;

import org.epics.vtype.VType;
import org.epics.vtype.VInt;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.Display;

public class VTypeHelperTest {

    @org.junit.Test
    public void highestAlarmOf() {
        VType arg1 = VInt.of(0, Alarm.none(), Time.now(), Display.none());
        VType arg2 = VInt.of(0, Alarm.lolo(), Time.now(), Display.none());

        assertTrue("Failed to correctly calculate highest alarm expected LOLO, got : " + VTypeHelper.highestAlarmOf(arg1, arg2),
                Alarm.lolo().equals(VTypeHelper.highestAlarmOf(arg1, arg2)));
    }
}