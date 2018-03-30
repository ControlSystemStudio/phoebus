/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;

/** Menu entry for job viewer
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JobViewerMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return JobViewerApplication.DISPLAY_NAME;
    }

    @Override
    public String getMenuPath()
    {
        return "Debug";
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(JobViewerApplication.NAME);
        return null;
    }
}
