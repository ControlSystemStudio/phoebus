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
import org.phoebus.ui.application.Messages;

/** 'Help' application descriptor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class HelpApplication implements AppDescriptor
{
    public static final String NAME = "help";
    public static final String DISPLAY_NAME = Messages.Help;

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
        if (HelpBrowser.INSTANCE == null)
            HelpBrowser.INSTANCE = new HelpBrowser(this);
        else
            HelpBrowser.INSTANCE.raise();
        return HelpBrowser.INSTANCE;
    }
}
