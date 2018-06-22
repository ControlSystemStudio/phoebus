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

import java.net.URL;

import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

/** Annunciator application
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AnnunciatorTableApplication implements AppDescriptor
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
    public AppInstance create()
    {
        if (AnnunciatorTableInstance.INSTANCE == null)
            AnnunciatorTableInstance.INSTANCE = new AnnunciatorTableInstance(this);
        else
            AnnunciatorTableInstance.INSTANCE.raise();
        return AnnunciatorTableInstance.INSTANCE;
    }

}
