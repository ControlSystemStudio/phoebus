/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log.derby;

import java.time.Instant;
import java.util.logging.LogManager;

import org.csstudio.scan.data.NumberScanSample;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.info.Scan;
import org.csstudio.scan.server.ScanServerInstance;
import org.junit.Test;

@SuppressWarnings("nls")
public class DerbyLogDemo
{
    @Test
    public void demoDerbyLog() throws Exception
    {
        LogManager.getLogManager().readConfiguration(ScanServerInstance.class.getResourceAsStream("/debug_logging.properties"));
        DerbyDataLogger.startup("/tmp/scan_log_db");

        final RDBDataLogger datalog = new DerbyDataLogger();

        System.out.println("Creating scans...");

        datalog.createScan("Test 1");
        datalog.createScan("Test 2");
        datalog.createScan("Test 3");

        long id = -1;
        for (Scan scan : datalog.getScans())
        {
        	id = scan.getId();
            System.out.println(scan);
        }

        if (id > 0)
        {
            System.out.println("Adding a sample to log " + id);
        	datalog.log(id, "test", new NumberScanSample(Instant.now(), 0, 3.14));


        	ScanDataIterator iter = new ScanDataIterator(datalog.getScanData(id));
        	System.out.println(iter.getDevices());
        	while (iter.hasNext())
        	{
        		for (ScanSample sample : iter.getSamples())
        			System.out.println(sample);
        	}
    	}

        datalog.close();

        DerbyDataLogger.shutdown();
    }
}
