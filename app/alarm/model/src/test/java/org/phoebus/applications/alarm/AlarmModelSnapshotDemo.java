/*******************************************************************************
 * Copyright (c) 2018-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmConfigMonitor;
import org.phoebus.applications.alarm.model.xml.XmlModelWriter;

/** Captures a snapshot of alarm config and writes as XML
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmModelSnapshotDemo
{
    @Test
    public void testAlarmModelWriter() throws Exception
    {
        // Get alarm configuration
        final AlarmClient client = new AlarmClient(AlarmDemoSettings.SERVERS, AlarmDemoSettings.ROOT, AlarmDemoSettings.KAFKA_PROPERTIES_FILE);
        client.start();
        
        System.out.println("Wait 10 secs for connection, then for stable configuration, i.e. no changes for 4 seconds...");
        final long start = System.currentTimeMillis();

        final AlarmConfigMonitor monitor = new AlarmConfigMonitor(10, 4, client);
        monitor.waitForPauseInUpdates(30);
        final double secs = (System.currentTimeMillis() - start) / 1000.0;
        System.out.format("Alarm configuration after %.3f seconds:\n\n", secs);

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final XmlModelWriter xmlWriter = new XmlModelWriter(buf);
        xmlWriter.write(client.getRoot());
        xmlWriter.close();
        final String xml = buf.toString();
        System.out.println(xml);

        final int changes = monitor.getCount();
        if (changes > 0)
            System.out.println("Bummer, there were " + changes + " updates to the configuration, might have to try this again...");
        monitor.dispose();
        client.shutdown();
    }
}
