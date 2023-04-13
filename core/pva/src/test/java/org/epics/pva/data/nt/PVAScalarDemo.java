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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAIntArray;
import org.junit.jupiter.api.Test;


/** Demo using demo.db from test resources:
 *    softIocPVA -m N='' -d demo.db
 */
public class PVAScalarDemo {
    
    /**
     * Tests if the ioc provides the expected formatting of PVATimeStamp,
     * PVAAlarm, PVADisplay, PVAEnum, PVAControl and PVAScalar
     * @throws Exception
     */
    @Test
    public void testPVAScalarRead() throws Exception {
        PVAClient client = new PVAClient();
        PVAChannel channel = client.getChannel("ramp");
        channel.connect().get(5, TimeUnit.SECONDS);

        channel.subscribe("", (ch, changes, overruns, data) ->
        {
            PVATimeStamp timeStamp = PVATimeStamp.getTimeStamp(data);
            assertNotNull(timeStamp);
            PVAAlarm alarm = PVAAlarm.getAlarm(data);
            assertNotNull(alarm);
            PVADisplay display = PVADisplay.getDisplay(data);
            assertNotNull(display);
            PVAControl control = PVAControl.getControl(data);
            assertNotNull(control);
            try {
				PVAScalar<PVAInt> scalar = PVAScalar.fromStructure(data);
                assertNotNull(scalar);
			} catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
				e.printStackTrace();
                fail();
			}
        });
        client.close();
    }
    /**
     * Tests if the ioc provides the expected formatting of PVATimeStamp,
     * PVAAlarm, PVADisplay, PVAEnum, PVAControl and PVAScalar
     * @throws Exception
     */
    @Test
    public void testPVAScalarArrayRead() throws Exception {
        PVAClient client = new PVAClient();
        PVAChannel channel = client.getChannel("waveform");
        channel.connect().get(5, TimeUnit.SECONDS);

        channel.subscribe("", (ch, changes, overruns, data) ->
        {
            PVATimeStamp timeStamp = PVATimeStamp.getTimeStamp(data);
            assertNotNull(timeStamp);
            PVAAlarm alarm = PVAAlarm.getAlarm(data);
            assertNotNull(alarm);
            PVADisplay display = PVADisplay.getDisplay(data);
            assertNotNull(display);
            PVAControl control = PVAControl.getControl(data);
            assertNotNull(control);
            try {
				PVAScalar<PVADoubleArray> scalar = PVAScalar.fromStructure(data);
                assertNotNull(scalar);
			} catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
				e.printStackTrace();
                fail();
			}
        });
        client.close();
    }
}
