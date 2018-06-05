/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.area;

import java.net.URL;

import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.framework.spi.AppDescriptor;

/** Application descriptor for "Alarm Area Panel"
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmAreaApplication implements AppDescriptor
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
        if (AlarmAreaInstance.INSTANCE == null)
            AlarmAreaInstance.INSTANCE = new AlarmAreaInstance(this);
        else
            AlarmAreaInstance.INSTANCE.raise();
        return AlarmAreaInstance.INSTANCE;
    }
}
