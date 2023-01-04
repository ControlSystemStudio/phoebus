package org.epics.pva.data.nt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVALong;
import org.junit.jupiter.api.Test;

public class PVATimeStampTest {
    @Test
    void testConstructor() {
        Instant time = Instant.ofEpochSecond(2, 1);
        PVATimeStamp timeStamp = new PVATimeStamp(time);
        assertEquals(new PVALong("secondsPastEpoch", false, 2), timeStamp.get("secondsPastEpoch"));
        assertEquals(new PVAInt("nanoseconds", false, 1), timeStamp.get("nanoseconds"));
        assertEquals(new PVAInt("userTag", 0), timeStamp.get("userTag"));

        assertEquals(time, timeStamp.instant());
    }
}
