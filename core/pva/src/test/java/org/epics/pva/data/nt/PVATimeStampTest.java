/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
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
