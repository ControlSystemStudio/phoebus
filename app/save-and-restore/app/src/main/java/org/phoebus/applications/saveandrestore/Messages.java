/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore;

import org.phoebus.framework.nls.NLS;

public class Messages {

    public static String alertContinue;
    public static String alertAddingPVsToSaveset;
    public static String buttonRefresh;
    public static String buttonSearch;
    public static String contextMenuAddTagWithComment;
    public static String contextMenuCreateSnapshot;
    public static String contextMenuCompareSnapshots;
    public static String contextMenuDelete;
    public static String contextMenuEdit;
    public static String contextMenuNewFolder;
    public static String contextMenuNewSaveSet;
    public static String contextMenuNoTagWithComment;
    public static String contextMenuRename;
    public static String contextMenuRemoveGoldenTag;
    public static String contextMenuTagAsGolden;
    public static String contextMenuTagsWithComment;
    public static String copyOrMoveNotAllowedBody;
    public static String copyOrMoveNotAllowedHeader;
    public static String copyUniqueIdToClipboard;
    public static String createNewTagDialogTitle;
    public static String deletionNotAllowed;
    public static String deletionNotAllowedHeader;
    public static String errorActionFailed;
    public static String errorCreateFolderFailed;
    public static String errorCreateSaveSetFailed;
    public static String errorDeleteNodeFailed;
    public static String errorGeneric;
    public static String exportSaveSetLabel;
    public static String exportSnapshotLabel;
    public static String importSaveSetLabel;
    public static String importSnapshotLabel;
    public static String jmasarServiceUnavailable;
    public static String labelMultiplier;
    public static String labelThreshold;
    public static String menuItemDeleteSelectedPVs;
    public static String openResourceFailedTitle;
    public static String openResourceFailedHeader;
    public static String promptCloseSnapshotTabTitle;
    public static String promptCloseSnapshotTabContent;
    public static String promptDeletePVTitle;
    public static String promptDeletePVFromSaveSet;
    public static String promptDeleteSelectedTitle;
    public static String promptDeleteSelectedHeader;
    public static String promptDeleteSelectedContent;
    public static String promptNewFolder;
    public static String promptNewSaveSetTitle;
    public static String promptNewSaveSetContent;
    public static String promptRenameNodeTitle;
    public static String promptRenameNodeContent;
    public static String restoreErrorTitle;
    public static String restoreErrorContent;
    public static String saveSnapshotErrorContent;
    public static String saveTagButtonLabel;
    public static String searchEntryToolTip;
    public static String searchKeywordLabel;
    public static String searchOptionSnapshotName;
    public static String searchOptionSnapshotComment;
    public static String searchOptionTagName;
    public static String searchOptionTagComment;
    public static String searchTableEntryTypeColumn;
    public static String searchTableNameColumn;
    public static String searchTableCommentColumn;
    public static String searchTableCreatedColumn;
    public static String searchTableCreatorColumn;
    public static String searchWindowLabel;
    public static String snapshotModifiedText;
    public static String tagNameLabel;
    public static String tagCommentLabel;
    public static String tagRemoveConfirmationTitle;
    public static String tagRemoveConfirmationContent;
    public static String toolTipShowLiveReadback;
    public static String toolTipShowStoredReadback;
    public static String toolTipShowTreeTable;
    public static String toolTipShowHideEqualToggleButton;
    public static String toolTipShowHideDeltaPercentageToggleButton;
    public static String toolTipTableColumnPVName;
    public static String toolTipTableColumnReadbackPVName;
    public static String toolTipTableColumIndex;
    public static String toolTipTableColumnTimestamp;
    public static String toolTipTableColumnAlarmStatus;
    public static String toolTipTableColumnAlarmSeverity;
    public static String toolTipTableColumnSetpointPVValue;
    public static String toolTipTableColumnPVValues;
    public static String toolTipUnionOfSetpointPVNames;
    public static String toolTipTableColumnBaseSetpointValue;
    public static String toolTipSaveSetExists;
    public static String toolTipSaveSetExistsOption;
    public static String toolTipMultiplierSpinner;
    public static String unnamedSnapshot;

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation
    }

}
