/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAUnion;
import org.junit.Test;

/** Demo using 'IMAGE' PV from
 *  https://github.com/kasemir/EPICSV4Sandbox/tree/master/ntndarrayServer
 *
 *  Run ./ntndarrayServerMain IMAGE
 *
 *
 *  Can also be used with Area Detector 'sim':
 *  In areaDetector/ADCore/iocBoot/commonPlugins.cmd, load the 'NDPluginPva' plugin
 *
 *  cd areaDetector/ADSimDetector/iocs/simDetectorIOC/iocBoot/iocSimDetector
 *  ../../bin/linux-x86_64/simDetectorApp st.cmd
 *
 *  caput 13SIM1:cam1:Acquire 1
 *  caput 13SIM1:image1:EnableCallbacks 1
 *  caput 13SIM1:Pva1:EnableCallbacks 1
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageDemo
{
    // "IMAGE" for ntndarrayServer, "13SIM1:Pva1:Image" for Area Detector
    private static final String PV_NAME = "IMAGE"; // "13SIM1:Pva1:Image";

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
        final Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
    }

    @Test
    public void testImage() throws Exception
    {
        // Create a client
        final PVAClient pva = new PVAClient();

        // Connect
        final ClientChannelListener channel_listener = (channel, state) -> System.out.println(channel);
        final PVAChannel ch = pva.getChannel(PV_NAME, channel_listener);
        ch.connect().get(5, TimeUnit.SECONDS);

        // Read value, show type (value could be too large)
        System.out.println(ch.read("").get().formatType());

        // Monitor updates
        final MonitorListener monitor_listener = (channel, changed, overruns, data) ->
        {
            final PVAUnion value = data.get("value");
            if (value.get() == null)
                System.out.println("value: nothing");
            else if (value.get() instanceof PVAShortArray)
            {
                final PVAShortArray array = value.get();
                System.out.println("value: " + array.get().length + " short elements");
            }
            else if (value.get() instanceof PVAByteArray)
            {
                final PVAByteArray array = value.get();
                System.out.println("value: " + array.get().length + " byte elements");
            }
        };
        final AutoCloseable subscription = ch.subscribe("value, dimension, timeStamp", monitor_listener);
        TimeUnit.SECONDS.sleep(3000);
        subscription.close();

        // Close channels
        ch.close();

        // Close the client
        pva.close();
    }
}
