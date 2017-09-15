/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.workbench;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppResourceDescriptor;

/** Information about all available applications
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ApplicationService
{
    public static final ApplicationService INSTANCE = new ApplicationService();

    private final Map<String, AppDescriptor> apps = new HashMap<>();

    private ApplicationService()
    {
        Logger logger = Logger.getLogger(getClass().getName());

        for (AppResourceDescriptor app : ServiceLoader.load(AppResourceDescriptor.class))
        {
            logger.log(Level.INFO, "Resource Application '" + app.getName() + "' (" + app.getDisplayName() + ")");
            final AppDescriptor previous = apps.put(app.getName(), app);
            if (previous != null)
                logger.log(Level.SEVERE, "Multiple implementations for Application '" + app.getName() + "'");
        }

        for (AppDescriptor app : ServiceLoader.load(AppDescriptor.class))
        {
            logger.log(Level.INFO, "Application '" + app.getName() + "' (" + app.getDisplayName() + ")");
            final AppDescriptor previous = apps.put(app.getName(), app);
            if (previous != null)
                logger.log(Level.SEVERE, "Multiple implementations for Application '" + app.getName() + "'");
        }
    }

    /** @return All known applications */
    public static Collection<AppDescriptor> getApplications()
    {
        return INSTANCE.apps.values();
    }

    /** Find application by name
     *
     *  @param name Application name
     *  @return {@link AppDescriptor} or <code>null</code> if not found
     */
    public static AppDescriptor findApplication(final String name)
    {
        return INSTANCE.apps.get(name);
    }
}
