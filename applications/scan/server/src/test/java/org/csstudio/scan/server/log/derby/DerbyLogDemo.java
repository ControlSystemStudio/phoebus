/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log.derby;

import java.util.logging.LogManager;

import org.csstudio.scan.info.Scan;
import org.csstudio.scan.server.ScanServerInstance;
import org.junit.Test;

@SuppressWarnings("nls")
public class DerbyLogDemo
{
    @Test
    public void demoDerbyLog() throws Exception
    {
        LogManager.getLogManager().readConfiguration(ScanServerInstance.class.getResourceAsStream("/logging.properties"));
        DerbyDataLogger.startup();

        final RDBDataLogger datalog = new DerbyDataLogger();

        System.out.println("Creating scans...");

        datalog.createScan("Test 1");
        datalog.createScan("Test 2");
        datalog.createScan("Test 3");

        for (Scan scan : datalog.getScans())
            System.out.println(scan);

        datalog.close();

        DerbyDataLogger.shutdown();
    }
}
