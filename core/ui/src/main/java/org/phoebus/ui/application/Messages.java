/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import org.phoebus.framework.nls.NLS;

public class Messages
{
    // Keep in alphabetical order and aligned with messages.properties
    public static String AllFiles;
    public static String AlwaysShowTabs;
    public static String Applications;
    public static String Enjoy;
    public static String Exit;
    public static String ExitContent;
    public static String ExitHdr;
    public static String ExitTitle;
    public static String File;
    public static String FileExists;
    public static String Help;
    public static String HomeTT;
    public static String ImagePng;
    public static String LayoutTT;
    public static String LoadLayout;
    public static String MonitorTaskApps;
    public static String MonitorTaskCmdl;
    public static String MonitorTaskPers;
    public static String MonitorTaskSave;
    public static String MonitorTaskStarting;
    public static String MonitorTaskTabs;
    public static String MonitorTaskUi;
    public static String Open;
    public static String OpenHdr;
    public static String OpenTitle;
    public static String OpenWith;
    public static String ProgressTitle;
    public static String Save;
    public static String SaveAs;
    public static String SaveDlgErrHdr;
    public static String SaveDlgHdr;
    public static String SaveLayoutAs;
    public static String SaveSnapshot;
    public static String SaveSnapshotSelectFilename;
    public static String ScreenshotErrHdr;
    public static String ScreenshotErrMsg;
    public static String ShowToolbar;
    public static String TopResources;
    public static String Window;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
    }
}
