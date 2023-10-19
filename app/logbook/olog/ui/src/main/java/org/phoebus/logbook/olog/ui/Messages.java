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
    public static String
            AdvancedSearchOpen,
            AdvancedSearchHide,
            Apply,
            ArchivedDownloadFailed,
            ArchivedLaunchExternalAppFailed,
            ArchivedNoEntriesFound,
            ArchivedSaveFailed,
            CloseRequestHeader,
            CloseRequestButtonContinue,
            CloseRequestButtonDiscard,
            DownloadSelected,
            DownloadingAttachments,
            EmbedImageDialogTitle,
            File,
            FileSave,
            FileSaveFailed,
            FileTooLarge,
            GroupingFailed,
            GroupSelectedEntries,
            Level,
            LogbooksSearchFailTitle,
            LogbookServiceUnavailableTitle,
            LogbookServiceHasNoLogbooks,
            NewLogEntry,
            NoAttachments,
            NoClipboardContent,
            NoSearchResults,
            PreviewOpenErrorBody,
            PreviewOpenErrorTitle,
            RequestTooLarge,
            SelectFile,
            SelectFolder,
            ShowHideDetails,
            SizeLimitsText,
            UpdateLogEntry;

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
