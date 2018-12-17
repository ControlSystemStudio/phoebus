/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.pv;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.application.Messages;

/** Application for the PV List
 *
 *  <p>Asserts a singleton instance.
 *  Trying to open more than one will activate the existing instance
 *  until that's closed.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVListApplication implements AppDescriptor
{
    public static final String NAME = "pv_list";
    public static final String DISPLAY_NAME = Messages.PVListAppName;

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
        // Create the singleton instance or show existing one
        if (PVListInstance.INSTANCE == null)
            PVListInstance.INSTANCE = new PVListInstance(this);
        else
            PVListInstance.INSTANCE.raise();
        return PVListInstance.INSTANCE;
    }
}
