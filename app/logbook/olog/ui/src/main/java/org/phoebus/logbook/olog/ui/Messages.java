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
            AttachmentsDirectoryFailedCreate,
            AttachmentsDirectoryNotWritable,
            AttachmentsFileNotDirectory,
            AttachmentsNoStorage,
            AvailableTemplates,
            Back,
            CloseRequestHeader,
            CloseRequestButtonContinue,
            CloseRequestButtonDiscard,
            DownloadSelected,
            DownloadingAttachments,
            EditLogEntry,
            EmbedImageDialogTitle,
            File,
            FileSave,
            FileSaveFailed,
            FileTooLarge,
            Forward,
            GroupingFailed,
            GroupSelectedEntries,
            JumpToLogEntry,
            Level,
            Logbook,
            LogbookNotSupported,
            LogbooksSearchFailTitle,
            LogbookServiceUnavailableTitle,
            LogbookServiceHasNoLogbooks,
            LogEntryID,
            NewLogEntry,
            NoAttachments,
            NoClipboardContent,
            NoSearchResults,
            PreviewOpenErrorBody,
            PreviewOpenErrorTitle,
            ReplyToLogEntry,
            RequestTooLarge,
            SearchFailed,
            SelectFile,
            SelectFolder,
            ShowHideDetails,
            SizeLimitsText,
            TextAreaContextMenuCopy,
            TextAreaContextMenuCut,
            TextAreaContextMenuDelete,
            TextAreaContextMenuPaste,
            TextAreaContextMenuPasteURLAsMarkdown,
            TextAreaContextMenuRedo,
            TextAreaContextMenuSelectAll,
            TextAreaContextMenuUndo,
            UnsupportedFileType,
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
