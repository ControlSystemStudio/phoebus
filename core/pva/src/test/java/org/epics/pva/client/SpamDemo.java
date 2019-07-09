/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;

/** Monitor the PV served by
 *    epics7/modules/pvAccess/examples/O.linux-x86_64/spamme
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SpamDemo
{
    static
    {
        try
        {
            LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
            // Logger.getLogger("").setLevel(Level.CONFIG);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(final String[] args) throws Exception
    {
        int pipeline = 4;
        if (args.length == 1)
            pipeline = Integer.parseInt(args[0]);

        final PVAClient pva = new PVAClient();
        final PVAChannel channel = pva.getChannel("spam");
        channel.connect().get(5, TimeUnit.SECONDS);

        final AutoCloseable subscription = channel.subscribe("", pipeline, (ch, changes, overruns, data) ->
        {
            System.out.println(data);
        });

        TimeUnit.HOURS.sleep(1);

        subscription.close();
        channel.close();
        pva.close();
    }
}
