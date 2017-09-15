/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.pv;

import org.phoebus.framework.spi.MenuEntry;

/** Menu entry that opens PV List
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVListMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return PVListApplication.NAME;
    }

    @Override
    public String getMenuPath()
    {
        return "Debug";
    }

    @Override
    public Void call() throws Exception
    {
        // TODO Get app descriptor from some global place
        new PVListApplication().create();
        return null;
    }
}
