/*******************************************************************************
 * Copyright (c) 2015-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import org.phoebus.framework.nls.NLS;

/** Localized messages */
public class Messages
{
    // Keep in alphabetical order and aligned with messages.properties
    /** Localized message */
    public static String NavigateBack_TT,
                         NavigateForward_TT,
                         OpenDataBrowser,
                         OpenInEditor,
                         PrintImage,
                         PrintPlot,
                         Refresh,
                         ReloadDisplay,
                         SaveImageSnapshot,
                         SavePlotSnapshot,
                         SendToLogbook,
                         Toolbar_Hide,
                         Toolbar_Show,
                         WidgetInformationHdr,
                         WidgetInformationRo,
                         WidgetInformationWr;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
    }
}
