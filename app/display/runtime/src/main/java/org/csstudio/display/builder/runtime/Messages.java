/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import org.phoebus.framework.nls.NLS;

public class Messages
{
    // Keep in alphabetical order and aligned with messages.properties
    public static String NavigateBack_TT;
    public static String NavigateForward_TT;
    public static String OpenDataBrowser;
    public static String OpenInEditor;
    public static String ReloadDisplay;
    public static String SendToLogbook;
    public static String Toolbar_Hide;
    public static String Toolbar_Show;
    public static String WidgetInformationHdr;
    public static String WidgetInformationRo;
    public static String WidgetInformationWr;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
    }
}
