/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.workbench;

import static org.phoebus.framework.workbench.WorkbenchPreferences.logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;

/** Information about all available applications
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ApplicationService
{
    public static final ApplicationService INSTANCE = new ApplicationService();

    /** All applications by name */
    private final Map<String, AppDescriptor> apps = new HashMap<>();

    /** All applications that handle a resource by extension */
    private final Map<String, List<AppResourceDescriptor>> extensions = new HashMap<>();

    private ApplicationService()
    {
        // SPI-provided applications that handle a resource
        for (AppResourceDescriptor app : ServiceLoader.load(AppResourceDescriptor.class))
        {
            logger.log(Level.INFO, "Resource Application '" + app.getName() + "' (" + app.getDisplayName() + ") " +
                                   app.supportedFileExtentions());
            registerResource(app);
        }

        // SPI-provided applications without resource
        for (AppDescriptor app : ServiceLoader.load(AppDescriptor.class))
        {
            logger.log(Level.INFO, "Application '" + app.getName() + "' (" + app.getDisplayName() + ")");
            register(app);
        }

        // External applications, defined in preferences
        for (String definition : WorkbenchPreferences.external_apps)
        {
            if (definition.isEmpty())
                continue;
            final String[] items = definition.split(",");
            if (items.length != 3)
            {
                logger.log(Level.WARNING, "external_apps missing name, ext, command for " + definition + "'");
                continue;
            }
            final String name = items[0].trim();
            final List<String> ext = List.of(items[1].split("\\|"));
            final String command = items[2].trim();
            logger.log(Level.INFO, "External Application '" + name + "' (" + command + ") " + ext);
            registerResource(new ExternalApplication(name, ext, command));
        }
    }

    private void registerResource(final AppResourceDescriptor app)
    {
        for (String ext : app.supportedFileExtentions())
        {
            if (!extensions.containsKey(ext))
                extensions.put(ext, new ArrayList<AppResourceDescriptor>());
            extensions.get(ext).add(app);
        }
        register(app);
    }

    /** @param app Application to register */
    private void register(final AppDescriptor app)
    {
        final AppDescriptor previous = apps.put(app.getName(), app);
        if (previous != null)
            logger.log(Level.SEVERE, "Multiple implementations for Application '" + app.getName() + "'");
    }

    /** @return All known applications */
    public static Collection<AppDescriptor> getApplications()
    {
        return INSTANCE.apps.values();
    }

    /** Find applications for this resource string
     *
     *  @param resource Resource URI
     *  @return List of Applications that can open this resource
     */
    public static List<AppResourceDescriptor> getApplications(final URI resource)
    {
        final String path = resource.getPath();
        final String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
        if (INSTANCE.extensions.containsKey(ext))
            return INSTANCE.extensions.get(ext);
        else
            return Collections.emptyList();
    }

    /** Find application by name
     *
     *  @param name Application name
     *  @return {@link AppDescriptor} or <code>null</code> if not found
     */
    @SuppressWarnings("unchecked")
    public static <AD extends AppDescriptor> AD findApplication(final String name)
    {
        return (AD) INSTANCE.apps.get(name);
    }

    /** Create instance of an application
     *
     *  @param name Application name
     *  @return {@link AppInstance} or <code>null</code> if not found
     */
    @SuppressWarnings("unchecked")
    public static <AI extends AppInstance> AI createInstance(final String name)
    {
        final AppDescriptor app = findApplication(name);
        if (app == null)
        {
            logger.log(Level.SEVERE, "Unknown application '" + name + "'");
            return null;
        }
        return (AI) app.create();
    }

    /** Create instance of an application that handles a resource
     *
     *  @param name Application name
     *  @param resource Resource to open in the application
     *  @return {@link AppInstance} or <code>null</code> if not found
     */
    @SuppressWarnings("unchecked")
    public static <AI extends AppInstance> AI createInstance(final String name, final URI resource)
    {
        final AppResourceDescriptor app = findApplication(name);
        if (app == null)
        {
            logger.log(Level.SEVERE, "Unknown application '" + name + "'");
            return null;
        }
        return (AI) app.create(resource);
    }
}
