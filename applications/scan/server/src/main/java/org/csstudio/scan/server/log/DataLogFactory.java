/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;

import org.csstudio.scan.info.Scan;
import org.csstudio.scan.server.log.derby.DerbyDataLogFactory;

/** Factory for scan {@link DataLog}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataLogFactory
{
	private static DataLogFactorySPI impl = null;

    public static void startup(final String location) throws Exception
    {
	    // Look for implementation via SPI.
	    for (DataLogFactorySPI dlf : ServiceLoader.load(DataLogFactorySPI.class))
	    {
	        if (impl != null)
	            throw new Error("Found multiple DataLogFactorySPI, " +
	                            impl + " as well as " + dlf);
	        impl = dlf;
	    }
	    if (impl == null)
	        impl = new DerbyDataLogFactory();
	    logger.log(Level.CONFIG, "Data Log: " + impl);
	    impl.startup(location);
	}

	public static Scan createDataLog(final String name) throws Exception
	{
		return impl.createDataLog(name);
	}

	public static List<Scan> getScans() throws Exception
	{
		return impl.getScans();
	}

    public static DataLog getDataLog(final Scan scan) throws Exception
    {
    	return impl.getDataLog(scan);
    }

    public static void deleteDataLog(final Scan scan) throws Exception
    {
    	impl.deleteDataLog(scan);
    }

    public static void shutdown() throws Exception
    {
        impl.shutdown();
    }
}
