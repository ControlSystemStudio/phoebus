/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.net.URI;
import java.net.URL;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.AlarmURI;
import org.phoebus.framework.spi.AppResourceDescriptor;

/** Annunciator application
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AnnunciatorTableApplication implements AppResourceDescriptor
{
    public static final String NAME = "annunciator";
    public static final String DISPLAY_NAME = "Annunciator";

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
        return AlarmUI.class.getResource("/icons/annunciator.png");
    }

    @Override
    public AnnunciatorTableInstance create()
    {
        return create(AlarmURI.createURI(AlarmSystem.server, AlarmSystem.config_name));
    }

    @Override
    public AnnunciatorTableInstance create(final URI resource)
    {
        try
        {
            return new AnnunciatorTableInstance(this, resource);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot create alarm annunciator for " + resource, ex);
        }
        return null;
    }
}
