/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log.derby;

import java.util.List;

import org.csstudio.scan.info.Scan;
import org.csstudio.scan.server.log.DataLog;
import org.csstudio.scan.server.log.DataLogFactorySPI;

/** {@link DataLogFactorySPI} for the {@link DerbyDataLog}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DerbyDataLogFactory implements DataLogFactorySPI
{
    @Override
    public void startup(final String location) throws Exception
    {
        DerbyDataLogger.startup(location);
    }

	@Override
	public Scan createDataLog(final String scan_name) throws Exception
	{
	    try
	    (
            final DerbyDataLogger log = new DerbyDataLogger();
        )
        {
            return log.createScan(scan_name);
        }
	}

	@Override
	public List<Scan> getScans() throws Exception
	{
        try
        (
            final DerbyDataLogger log = new DerbyDataLogger();
        )
        {
            return log.getScans();
        }
	}

	@Override
	public DataLog getDataLog(final Scan scan) throws Exception
	{
        try
        (
            final DerbyDataLogger log = new DerbyDataLogger();
        )
        {
            if (log.getScan(scan.getId()) != null)
                return new DerbyDataLog(scan.getId());
        }
        return null;
	}

	@Override
	public void deleteDataLog(final Scan scan) throws Exception
	{
        try
        (
            final DerbyDataLogger log = new DerbyDataLogger();
        )
        {
            log.deleteDataLog(scan.getId());
        }
	}

	@Override
    public void shutdown() throws Exception
	{
        DerbyDataLogger.shutdown();
	}

    @Override
    public String toString()
    {
        return "Derby Data Log";
    }
}
