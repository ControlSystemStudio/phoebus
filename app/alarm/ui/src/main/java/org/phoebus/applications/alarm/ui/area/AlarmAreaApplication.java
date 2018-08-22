/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.area;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.net.URI;
import java.net.URL;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.AlarmURI;
import org.phoebus.framework.spi.AppResourceDescriptor;

/** Application descriptor for "Alarm Area Panel"
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmAreaApplication implements AppResourceDescriptor
{
    public static final String NAME = "alarm_area";
    public static final String DISPLAY_NAME = "Alarm Area Panel";

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
        return AlarmUI.class.getResource("/icons/areapanel.png");
    }

    @Override
    public AlarmAreaInstance create()
    {
        return create(AlarmURI.createURI(AlarmSystem.server, AlarmSystem.config_name));
    }

    @Override
    public AlarmAreaInstance create(final URI resource)
    {
        try
        {
            return new AlarmAreaInstance(this, resource);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot create alarm area panel for " + resource, ex);
        }
        return null;
    }
}
