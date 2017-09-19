/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.utility;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

/** Application descriptor for the logging configuration */
@SuppressWarnings("nls")
public class LoggingConfigurationApplication implements AppDescriptor
{
    public static final String NAME = "log_config";
    public static final String DISPLAY_NAME = "Logging Config";

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
        if (LoggingConfiguration.INSTANCE == null)
            LoggingConfiguration.INSTANCE = new LoggingConfiguration(this);
        else
            LoggingConfiguration.INSTANCE.raise();
        return LoggingConfiguration.INSTANCE;
    }
}
