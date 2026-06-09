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
    public static String automatedActions;
    public static String commands;
    public static String configure;
    public static String delayTooltip0;
    public static String delayTooltip1;
    public static String delayTooltip2;
    public static String disableAlarmFailed;
    public static String disableAlarms;
    public static String disabled;
    public static String disabledUntil;
    public static String displays;
    public static String enableAlarmFailed;
    public static String enableAlarms;
    public static String guidance;
    public static String headerAlreadyDisabled;
    public static String headerAlreadyEnabled;
    public static String headerConfirmDisable;
    public static String headerConfirmDisableWithEnableDate;
    public static String headerConfirmEnable;
    public static String moveItemFailed;
    public static String partlyDisabled;
    public static String promptTitle;
    public static String promptContent;
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
