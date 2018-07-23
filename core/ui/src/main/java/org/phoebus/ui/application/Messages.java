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
    public static String AlwaysShowTabs;
    public static String Applications;
    public static String Exit;
    public static String File;
    public static String Help;
    public static String Open;
    public static String OpenWith;
    public static String Save;
    public static String SaveAs;
    public static String SaveLayoutAs;
    public static String SaveSnapshot;
    public static String SaveSnapshotSelectFilename;
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
