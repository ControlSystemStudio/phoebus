/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
        try
        (
            // Create PVA Server (auto-closed)
            final PVAServer server = new PVAServer();
        )
        {
            // Create data structure to serve
            final PVATimeStamp time = new PVATimeStamp();
            final PVABool value = new PVABool("value", false);
            final PVAStructure data = new PVAStructure("demo", "epics:nt/NTScalar:1.0",
                                                       value,
                                                       time);

            // Create PV
            final ServerPV pv = server.createPV("bool", data);
            System.out.println("Check PV   '" + pv.getName() + "'");

            // Update value and timestamp
            while (true)
            {
                TimeUnit.SECONDS.sleep(1);
                value.set(! value.get());
                time.set(Instant.now());
                pv.update(data);
            }
        }
    }
}
