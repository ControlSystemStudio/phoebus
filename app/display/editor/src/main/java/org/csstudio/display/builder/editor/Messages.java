/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import org.phoebus.framework.nls.NLS;

/** Externalized Strings
 *  @author Kay Kasemir
 */
public class Messages
{
    // Keep in alphabetical order, synchronized with messages.properties
    /** Externalized Strings */
    public static String AbortSave,
                         AddElement,
                         AddWidget,
                         Align,
                         AlignBottom,
                         AlignCenter,
                         AlignGrid,
                         AlignLeft,
                         AlignMiddle,
                         AlignRight,
                         AlignTop,
                         CollapseTree,
                         Copy,
                         CreateGroup,
                         Cut,
                         Delete,
                         Display,
                         DisplayApplicationMissingRight,
                         DisplayApplicationName,
                         Distribute,
                         DistributeGapTitle,
                         DistributeGapMessage,
                         DistributeHorizontally,
                         DistributeVertically,
                         DistributeHorizontallyGap,
                         DistributeVerticallyGap,
                         Duplicate,
                         DownloadPromptFMT,
                         DownloadTitle,
                         EditEmbededDisplay,
                         ExpandTree,
                         FileChangedHdr,
                         FileChangedDlg,
                         FindWidget,
                         Grid,
                         LoadDisplay,
                         LoadDisplay_TT,
                         MacroEditButton,
                         MatchHeight,
                         MatchWidth,
                         MoveDown,
                         MoveToBack,
                         MoveToFront,
                         MoveUp,
                         NewDisplay,
                         NewDisplayFailed,
                         NewDisplayTargetFolderWriteProtected,
                         NewDisplaySelectionEmpty,
                         NewDisplayOverwriteExisting,
                         NewDisplayOverwriteExistingTitle,
                         OpenInExternalEditor,
                         Order,
                         Paste,
                         PointCount_Fmt,
                         PropertyFilterTT,
                         ReloadClasses,
                         ReloadDisplay,
                         ReloadWarning,
                         RemoveElement,
                         RemoveGroup,
                         RemoveWidgets,
                         ReplaceWith,
                         RuleCountFMT,
                         Run,
                         SaveDisplay,
                         SaveDisplayErrorFMT,
                         SaveDisplay_TT,
                         ScriptCountFMT,
                         SearchTextField,
                         SelectZoomLevel,
                         SetDisplaySize,
                         SetPropertyFmt,
                         SetWidgetPoints,
                         ShowCoordinates,
                         ShowCrosshair,
                         ShowProperties,
                         ShowWidgetTree,
                         Size,
                         Snap,
                         UpdateWidgetLocation,
                         UpdateWidgetOrder,
                         UseWidgetClass_TT,
                         UsingWidgetClass_TT,
                         WidgetFilterTT,
                         WT_FromString_dialog_content,
                         WT_FromString_dialog_headerFMT,
                         WT_FromString_dialog_title,
                         WT_FromString_multipleFMT,
                         WT_FromString_singleFMT,
                         WT_FromURL_dialog_content,
                         WT_FromURL_dialog_headerFMT,
                         WT_FromURL_dialog_title;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }
}
