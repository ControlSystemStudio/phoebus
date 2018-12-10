/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.perfmon;

import org.phoebus.ui.spi.MenuEntry;
import org.phoebus.ui.statusbar.StatusBar;

/** Menu entry (SPI) for adding/removing performance monitor status bar entry
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenPerfMon implements MenuEntry
{
    private static PerfMonButton button;

    @Override
    public String getName()
    {
        return "Performance Monitor";
    }

    @Override
    public String getMenuPath()
    {
        return "Debug";
    }

    @Override
    public Void call() throws Exception
    {
        if (button == null)
        {
            button = new PerfMonButton();
            StatusBar.getInstance().addItem(button);
        }
        else
        {
            button.dispose();
            StatusBar.getInstance().removeItem(button);
            button = null;
        }
        return null;
    }
}
