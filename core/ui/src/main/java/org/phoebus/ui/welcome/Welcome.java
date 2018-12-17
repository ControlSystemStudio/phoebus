/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.welcome;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.application.Messages;

/** Welcome Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Welcome  implements AppDescriptor
{
    @Override
    public String getName()
    {
        return "welcome";
    }

    @Override
    public String getDisplayName()
    {
        return Messages.Welcome;
    }

    @Override
    public AppInstance create()
    {
        if (WelcomeInstance.INSTANCE == null)
            WelcomeInstance.INSTANCE = new WelcomeInstance(this);
        else
            WelcomeInstance.INSTANCE.raise();
        return WelcomeInstance.INSTANCE;
    }
}
