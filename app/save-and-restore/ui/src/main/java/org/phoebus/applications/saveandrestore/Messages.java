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

    public static String unnamedSnapshot;
    public static String promptCloseSnapshotTabTitle;
    public static String promptCloseSnapshotTabContent;
    public static String promptDeletePVFromSaveSet;
    public static String restoreErrorTitle;
    public static String restoreErrorContent;
    public static String menuItemDeleteSelectedPVs;
    public static String promptRenamePVTitle;
    public static String contextMenuNewFolder;
    public static String contextMenuNewTopLevelFolder;
    public static String contextMenuNewSaveSet;
    public static String contextMenuRename;
    public static String contextMenuDelete;
    public static String contextMenuEdit;
    public static String contextMenuOpen;
    public static String contextMenuCompareSnapshots;
    public static String contextMenuTagAsGolden;
    public static String contextMenuRemoveGoldenTag;
    public static String errorActionFailed;
    public static String jmasarServiceUnavailable;
    public static String deletionNotAllowedHeader;
    public static String deletionNotAllowed;
    public static String promptDeleteSelectedTitle;
    public static String promptDeleteSelectedHeader;
    public static String promptDeleteSelectedContent;
    public static String promptNewFolder;
    public static String promptNewSaveSetTitle;
    public static String promptNewSaveSetContent;
    public static String promptRenameNodeTitle;
    public static String promptRenameNodeContent;
    public static String buttonRefresh;
    public static String labelCreateFolderEmptyTree;
    public static String toolTipShowLiveReadback;
    public static String toolTipShowStoredReadback;
    public static String toolTipShowHideEqualToggleButton;
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

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation
    }

}
