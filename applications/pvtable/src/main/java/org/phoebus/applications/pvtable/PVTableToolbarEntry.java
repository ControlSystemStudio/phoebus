/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtable;

import org.phoebus.framework.spi.ToolbarEntry;

/** Toolbar entry that starts PV Table Application
 *  @author Kay Kasemir
 */
// @ProviderFor(ToolbarEntry.class)
public class PVTableToolbarEntry implements ToolbarEntry
{
    @Override
    public String getName()
    {
        return PVTableApplication.NAME;
    }

    @Override
    public void call() throws Exception
    {
        new PVTableApplication().open();
    }
}
