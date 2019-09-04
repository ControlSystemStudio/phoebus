/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;

/** PVA RPC Service Demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RPCServiceDemo
{
    public static PVAStructure sum(final PVAStructure params)
    {
        PVAString a = params.get("a");
        PVAString b = params.get("b");
        double c = Double.parseDouble(a.get()) + Double.parseDouble(b.get());
        return new PVAStructure("", "", new PVADouble("c", c));
    }

    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        // Create PVA Server
        final PVAServer server = new PVAServer();

        // Create RPC PV
        final ServerPV pv = server.createPV("sum", RPCServiceDemo::sum);

        // Run nearly forever...
        TimeUnit.DAYS.sleep(2);

        // Close server (real world server tends to run forever, though)
        server.close();
    }
}
