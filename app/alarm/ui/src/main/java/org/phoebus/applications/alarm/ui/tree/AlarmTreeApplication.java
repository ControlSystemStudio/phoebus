/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.net.URL;

import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.framework.spi.AppDescriptor;

/** Application descriptor for "Alarm Tree"
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTreeApplication implements AppDescriptor
{
    public static final String NAME = "alarm_tree";
    public static final String DISPLAY_NAME = "Alarm Tree";

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
        return AlarmUI.class.getResource("/icons/alarmtree.png");
    }

    @Override
    public AlarmTreeInstance create()
    {
        if (AlarmTreeInstance.INSTANCE == null)
            AlarmTreeInstance.INSTANCE = new AlarmTreeInstance(this);
        else
            AlarmTreeInstance.INSTANCE.raise();
        return AlarmTreeInstance.INSTANCE;
    }
}
