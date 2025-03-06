/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import org.phoebus.framework.nls.NLS;

public class Messages
{
    public static String error;

    public static String acknowledgeFailed;
    public static String addComponentFailed;
    public static String disableAlarmFailed;
    public static String disabled;
    public static String disabledUntil;
    public static String enableAlarmFailed;
    public static String moveItemFailed;
    public static String partlyDisabled;
    public static String removeComponentFailed;
    public static String renameItemFailed;
    public static String timer;
    public static String unacknowledgeFailed;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages() 
    {
        // prevent instantiation
    }
}
