/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import org.phoebus.framework.spi.ToolbarEntry;
import org.phoebus.framework.workbench.ApplicationService;

/** Toolbar entry that starts PV Tree Application
 *  @author Kay Kasemir
 */
// @ProviderFor(ToolbarEntry.class)
public class PVTreeToolbarEntry implements ToolbarEntry
{
    @Override
    public String getName()
    {
        return PVTreeApplication.DISPLAY_NAME;
    }

    @Override
    public void call() throws Exception
    {
        ApplicationService.findApplication(PVTreeApplication.NAME).create();
    }
}
