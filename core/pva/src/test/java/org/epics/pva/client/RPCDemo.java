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
    static
    {
        try
        {
            LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/logging.properties"));
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

        // Assemble request parameters
        final PVAStructure request = new PVAStructure("", "",
                                                      new PVAString("a", "11"),
                                                      new PVADouble("b", 3.14));
        System.out.println("Request:\n" + request);

        // Invoke RPC, get response
        final PVAStructure response = ch.invoke(request).get(10, TimeUnit.SECONDS);
        System.out.println("Response:\n" + response);

        // Decode some element from response
        PVADouble sum = response.get("c");
        System.out.println("Sum: " + sum.get());

        // Close Channel
        ch.close();

        // Close client
        pva.close();
    }
}
