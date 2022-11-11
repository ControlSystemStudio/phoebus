/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.time.Instant;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVABool;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;

/** PVA Server Demo for "bool" PV
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BoolDemo
{
    public static void main(String[] args) throws Exception
    {
        // Log everything
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
        PVASettings.logger.setLevel(Level.FINE);

        try
        (
            // Create PVA Server (auto-closed)
            final PVAServer server = new PVAServer();
        )
        {
            // Create data structure to serve that claims to be an NTScalar
            final PVATimeStamp time = new PVATimeStamp();
            final PVABool value = new PVABool("value", false);
            final PVAStructure data = new PVAStructure("demo", "epics:nt/NTScalar:1.0",
                                                       value,
                                                       time);

            // Create custom structure
            final PVABool flag1 = new PVABool("flag1", false);
            final PVABool flag2 = new PVABool("flag2", false);
            final PVABool flag3 = new PVABool("flag3", false);
            final PVAStructure struct = new PVAStructure("demo2", "CustomStruct",
                    flag1, flag2, flag3);


            // Create PVs
            final ServerPV pv1 = server.createPV("bool", data);
            final ServerPV pv2 = server.createPV("struct", struct, (ServerPV pv, BitSet changes, PVAStructure written) ->
            {
                System.out.println("Somebody wrote this to '" + pv.getName() + "':\n" + written);
                pv.update(written);
            });
            System.out.println("Check PV   '" + pv1.getName() + "' or '" + pv2.getName() + "'");
            System.out.println("May also try to write:   pvput struct 'flag2=true'");

            // Update value and timestamp
            while (true)
            {
                TimeUnit.SECONDS.sleep(1);
                value.set(! value.get());
                time.set(Instant.now());
                pv1.update(data);

                flag1.set(value.get());
                flag2.set(! flag1.get());
                flag3.set(! flag2.get());
                pv2.update(struct);
            }
        }
    }
}
