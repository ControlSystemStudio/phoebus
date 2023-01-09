package org.epics.pva.data.nt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.junit.jupiter.api.Test;

public class PVAAlarmTest {

    @Test
    void testConstructor() {
        PVAAlarm alarm = new PVAAlarm(5, 1, "alarmMessage");
        assertEquals(new PVAInt("severity", 5), alarm.get("severity"));
        assertEquals(new PVAInt("status",  1), alarm.get("status"));
        assertEquals(new PVAString("message", "alarmMessage"), alarm.get("message"));
    }

    @Test
    public void testSet() {
        PVAAlarm alarm = new PVAAlarm(1, 2, "test message");

        PVAStructure clone = alarm.cloneData();

        assertEquals(alarm, clone);

        alarm.set(0, 0, "test message 2");
        assertNotEquals(alarm, clone);
    }
}
