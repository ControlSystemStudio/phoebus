/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.print.ModelPrinter;

/** Demo of the {@link AlarmClient}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmClientModelDemo
{
    @Test
    public void testClientModel() throws Exception
    {
        final AlarmClient client = new AlarmClient(AlarmDemoSettings.SERVERS, AlarmDemoSettings.ROOT);
        client.start();
        TimeUnit.SECONDS.sleep(4);

        System.out.println("Snapshot after 4 seconds:");
        ModelPrinter.print(client.getRoot());

        System.out.println("Monitoring changes...");
        client.addListener(new AlarmClientListener()
        {
            @Override
            public void serverStateChanged(final boolean alive)
            {
                System.out.println("Server alive: " + alive);
            }

            @Override
            public void serverModeChanged(final boolean maintenance_mode)
            {
                System.out.println(maintenance_mode ? "MAINTENANCE mode" : "NORMAL mode");
            }

	     @Override
            public void serverDisableNotifyChanged(final boolean disable_notify)
            {
                System.out.println(disable_notify ? "DISABLED notify" : "ENABLED notify");
            }

            @Override
            public void itemAdded(final AlarmTreeItem<?> item)
            {
                System.out.println("Added " + item.getPathName());
                ModelPrinter.print(client.getRoot());
            }

            @Override
            public void itemRemoved(final AlarmTreeItem<?> item)
            {
                System.out.println("Removed " + item.getPathName());
                ModelPrinter.print(client.getRoot());
            }

            @Override
            public void itemUpdated(final AlarmTreeItem<?> item)
            {
                System.out.println("Updated " + item.getPathName());
            }
        });
        TimeUnit.SECONDS.sleep(4);


        client.shutdown();
    }
}
