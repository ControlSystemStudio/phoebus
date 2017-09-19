/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtable;

import org.phoebus.framework.spi.ToolbarEntry;
import org.phoebus.framework.workbench.ApplicationService;

/** Toolbar entry that starts PV Table Application
 *  @author Kay Kasemir
 */
public class PVTableToolbarEntry implements ToolbarEntry
{
    @Override
    public String getName()
    {
        return PVTableApplication.DISPLAY_NAME;
    }

    @Override
    public void call() throws Exception
    {
        ApplicationService.findApplication(PVTableApplication.NAME).create();
    }
}
