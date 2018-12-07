/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.monitoring;

import org.phoebus.ui.spi.MenuEntry;

/** Menu entry to freeze UI for testing {@link ResponsivenessMonitor}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FreezeUI implements MenuEntry
{
    @Override
    public String getName()
    {
        return "Freeze UI";
    }

    @Override
    public String getMenuPath()
    {
        return "Debug";
    }

    @Override
    public Void call() throws Exception
    {
        Thread.sleep(2000);
        return null;
    }
}
