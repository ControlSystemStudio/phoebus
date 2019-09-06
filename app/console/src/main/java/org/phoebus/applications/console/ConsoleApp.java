/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.console;

import java.net.URL;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

/** Console App Descriptor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ConsoleApp implements AppDescriptor
{
    static final String NAME = "console";
    static final String DISPLAY_NAME = "Console";

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
    public URL getIconURL()
    {
        return ConsoleApp.class.getResource("/icons/console_view.png");
    }

    @Override
    public AppInstance create()
    {
        return new ConsoleInstance(this);
    }
}