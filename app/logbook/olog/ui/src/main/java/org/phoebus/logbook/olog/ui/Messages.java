/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.olog.ui;

import org.phoebus.framework.nls.NLS;

public class Messages
{
    public static String Add_Tooltip,
                         Apply,
                         Clear,                                
                         Clear_Tooltip,
                         DownloadSelected,
                         EmbedImageDialogTitle,
                         File,
                         FileSave,
                         FileSaveFailed,
                         GroupSelectedEntries,
                         Level,
                         Logbooks,
                         LogbookServiceUnavailableTitle,
                         LogbookServiceHasNoLogbooks,
                         LogbooksTitle,
                         LogbooksTooltip,
                         NoClipboardContent,
                         Normal,
                         NoSearchResults,
                         PreviewOpenErrorBody,
                         PreviewOpenErrorTitle,
                         Remove_Tooltip,
                         SearchAvailableItems,
                         SelectFile,
                         SelectFolder,
                         Tags,
                         TagsTitle,
                         TagsTooltip;
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
