/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

/** Application descriptor for "Jobs"
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JobViewerApplication implements AppDescriptor
{
    static final String DISPLAY_NAME = "Active Jobs";
    public static final String NAME = "jobs";

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
        if (JobViewer.INSTANCE == null)
            JobViewer.INSTANCE = new JobViewer(this);
        else
            JobViewer.INSTANCE.raise();
        return JobViewer.INSTANCE;
    }
}
