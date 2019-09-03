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
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.junit.Test;

/** RPC Demo for 'sum' from EPICS Base example
 *
 *  <pre>
 *  cd $EPICS_BASE/modules/pvAccess/testApp/O.$EPICS_HOST_ARCH
 *  ./rpcServiceExample
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RPCDemo
{
    private static final boolean stress = false;

    static
    {
        try
        {
            LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
            if (stress)
                Logger.getLogger("").setLevel(Level.WARNING);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void testRPC() throws Exception
    {
        // Create a client
        final PVAClient pva = new PVAClient();

        // Connect
        final PVAChannel ch = pva.getChannel("sum");
        ch.connect().get(2, TimeUnit.SECONDS);

        while (true)
        {
            // Assemble request parameters.
            // rpcServiceExample would accepts PVADouble as well as PVAString,
            // but similar pvaPy example passes strings
            final PVAStructure request = new PVAStructure("", "",
                                                          new PVAString("a", "11"),
                                                          new PVAString("b", Double.toString(Math.random())));
            System.out.println("Request:\n" + request);

            // Invoke RPC, get response
            final PVAStructure response = ch.invoke(request).get(10, TimeUnit.SECONDS);
            System.out.println("Response:\n" + response);

            // Decode some element from response
            PVADouble sum = response.get("c");
            System.out.println("Sum: " + sum.get());

            if (! stress)
                break;
        }

        // Close Channel
        ch.close();

        // Close client
        pva.close();
    }
}
