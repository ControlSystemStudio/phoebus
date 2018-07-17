/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.datatable;

import static org.csstudio.scan.ScanSystem.logger;

import java.net.URI;
import java.util.logging.Level;

import org.csstudio.scan.ui.ScanURI;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;

/** Application for Scan data table
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanDataTableApplication implements AppResourceDescriptor
{
    public static final String NAME = "scan_table";
    public static final String DISPLAY_NAME = "Scan Data Table";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public AppInstance create()
    {
        logger.log(Level.WARNING, "Scan data table can only be opened with URL for scan");
        return null;
    }

    @Override
    public ScanDataTableInstance create(final URI resource)
    {
        try
        {
            return new ScanDataTableInstance(this, ScanURI.getScanID(resource));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot open scan data table for " + resource, ex);
        }
        return null;
    }
}
