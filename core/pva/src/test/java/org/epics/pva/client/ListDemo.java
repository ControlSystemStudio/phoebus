/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.junit.Test;

/** Beacon demo using demo.db from test resources
 *
 *  <p>To see beacons, might need to disable firewall
 *  since broadcasts are otherwise not received.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ListDemo
{
    static
    {
        try
        {
            LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void demoPVList() throws Exception
    {
        final PVAClient client = new PVAClient();
        final Collection<ServerInfo> servers = client.list(TimeUnit.SECONDS, 3);
        client.close();
        servers.forEach(System.out::println);
    }
}
