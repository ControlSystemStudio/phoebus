/**
 * Copyright (C) 2024 European Spallation Source ERIC.
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

@SuppressWarnings("unused")
public class Messages {

    public static String actionOpenFilterDescription;
    public static String actionOpenNodeDescription;
    public static String alertContinue;
    public static String alertAddingPVsToConfiguration;
    public static String archiver;
    public static String authenticationFailed;
    public static String baseSetpoint;
    public static String buttonSearch;
    public static String cannotCompareHeader;
    public static String cannotCompareTitle;
    public static String closeConfigurationWarning;
    public static String closeCompositeSnapshotWarning;
    public static String closeTabPrompt;
    public static String compositeSnapshotConsistencyCheckFailed;
    public static String contextMenuAddTag;
    @Deprecated
    public static String contextMenuAddTagWithComment;
    public static String contextMenuCreateSnapshot;
    public static String contextMenuCompareSnapshots;
    public static String contextMenuCompareSnapshotWithArchiverData;
    public static String contextMenuDelete;
    public static String copy;

    public static String contextMenuAddToCompositeSnapshot;
    public static String contextMenuNewFolder;
    public static String contextMenuNewCompositeSnapshot;
    public static String contextMenuNewConfiguration;

    public static String contextMenuNoTagWithComment;
    public static String contextMenuRename;
    public static String contextMenuRemoveGoldenTag;
    public static String contextMenuTagAsGolden;
    public static String contextMenuTags;
    @Deprecated
    public static String contextMenuTagsWithComment;
    public static String contextMenuOpenCompositeSnapshotForRestore;

    public static String copyOrMoveNotAllowedBody;
    public static String copyOrMoveNotAllowedHeader;
    public static String copyUniqueIdToClipboard;
    public static String copyUniqueIdAsResourceToClipboard;

    public static String createNewTagDialogHeader;
    public static String createNewTagDialogTitle;
    public static String createCompositeSnapshotFailed;
    public static String createConfigurationFailed;
    public static String createNodeFailed;
    public static String currentPVValue;
    public static String currentReadbackValue;
    public static String currentSetpointValue;
    public static String dateTimePickerTitle;
    public static String deleteFilter;
    public static String deleteFilterFailed;

    public static String duplicatePVNamesAdditionalItems;
    public static String duplicatePVNamesCheckFailed;
    public static String duplicatePVNamesFoundInSelection;
    public static String duplicatePVNamesNotSupported;
    public static String Edit;
    public static String editFilter;
    public static String errorActionFailed;
    public static String errorAddTagFailed;
    public static String errorCreateFolderFailed;
    public static String errorCreateConfigurationFailed;
    public static String errorDeleteNodeFailed;
    public static String errorDeleteTagFailed;
    public static String errorGeneric;
    public static String errorUnableToRetrieveData;
    public static String exportConfigurationLabel;
    public static String exportSnapshotLabel;
    public static String exportSnapshotFailed;
    public static String failedDeleteFilter;
    public static String failedGetFilters;
    public static String failedGetSpecificFilter;
    public static String failedSaveFilter;
    public static String failedToPasteObjects;
    public static String filterNotFound;
    public static String findSnapshotReferences;
    public static String importConfigurationLabel;
    public static String importSnapshotLabel;
    public static String includeThisPV;
    public static String inverseSelection;
    public static String liveReadbackVsSetpoint;
    public static String liveSetpoint;
    public static String login;
    public static String loggingFailedTitle;
    public static String loggingFailed;
    public static String loggingFailedCauseUnknown;
    public static String makeRestorable;
    public static String makeReadOnly;
    public static String manageFilters;
    public static String menuItemDeleteSelectedPVs;
    public static String noFilter;
    public static String openResourceFailedTitle;
    public static String openResourceFailedHeader;

    public static String overwrite;
    public static String paste;
    public static String promptCloseSnapshotTabContent;
    public static String promptDeleteSelectedTitle;
    public static String promptDeleteSelectedHeader;
    public static String promptDeleteSelectedContent;
    public static String promptNewFolder;

    public static String promptRenameNodeTitle;
    public static String promptRenameNodeContent;

    public static String pvName;
    public static String nodeSelectionForConfiguration;
    public static String noValueAvailable;
    public static String readbackPVName;
    public static String restore;
    public static String restoreFailed;
    public static String restoreFailedPVs;
    public static String restoreFromClient;
    public static String restoreFromService;
    public static String saveFilter;

    public static String saveFilterConfirmOverwrite;
    public static String saveFilterFailed;

    public static String saveSnapshotErrorContent;
    public static String saveSnapshotFailed;
    public static String saveTagButtonLabel;
    public static String search;
    public static String searchEntryToolTip;
    public static String searchErrorBody;
    public static String searchFailed;
    public static String setpoint;
    public static String setpointPVWhen;
    public static String severity;
    public static String snapshotFromArchiver;
    public static String snapshotFromPvs;
    public static String status;
    public static String storedReadbackValue;
    public static String storedValues;
    public static String tableColumnDeltaValue;
    public static String tagAddFailed;
    public static String tagNameLabel;
    public static String tagCommentLabel;
    public static String tagRemoveConfirmationTitle;
    public static String tagRemoveConfirmationContent;
    public static String takeSnapshotFailed;
    public static String timestamp;

    public static String toolTipTableColumnPVName;
    public static String toolTipTableColumnReadbackPVName;
    public static String toolTipTableColumIndex;
    public static String toolTipTableColumnTimestamp;
    public static String toolTipTableColumnSetpointPVValue;
    public static String toolTipTableColumnPVValues;
    public static String toolTipUnionOfSetpointPVNames;
    public static String toolTipTableColumnBaseSetpointValue;
    public static String toolTipConfigurationExists;
    public static String toolTipConfigurationExistsOption;
    public static String toolTipMultiplierSpinner;
    public static String unnamedSnapshot;

    public static String updateCompositeSnapshotFailed;
    public static String updateConfigurationFailed;

    public static String updateNodeFailed;

    static {
        NLS.initializeMessages(Messages.class);
    }

    private Messages() {
        // Prevent instantiation
    }

}
