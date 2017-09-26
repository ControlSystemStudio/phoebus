/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

/** 'Help' application descriptor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class HelpApplication implements AppDescriptor
{
    @Override
    public String getName()
    {
        return "help";
    }

    @Override
    public String getDisplayName()
    {
        return "Help";
    }

    @Override
    public AppInstance create()
    {
        if (HelpBrowser.INSTANCE == null)
            HelpBrowser.INSTANCE = new HelpBrowser(this);
        else
            HelpBrowser.INSTANCE.raise();
        return HelpBrowser.INSTANCE;
    }
}
