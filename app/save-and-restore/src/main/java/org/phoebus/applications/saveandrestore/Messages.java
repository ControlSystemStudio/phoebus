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
    public static String restoreErrorTitle;
    public static String restoreErrorContent;
    public static String menuItemDeleteSelectedPVs;
    public static String menuItemEditPVName;
    public static String promptRenamePVTitle;
    public static String promptRenamePVContent;
    public static String contextMenuNewFolder;
    public static String contextMenuNewSaveSet;
    public static String contextMenuRename;
    public static String contextMenuDelete;
    public static String contextMenuEdit;
    public static String contextMenuOpen;
    public static String contextMenuCompareSnapshots;
    public static String contextMenuTagAsGolden;
    public static String contextMenuRemoveGoldenTag;
    public static String errorGeneric;
    public static String errorActionFailed;
    public static String jmasarServiceConnectionFailure;
    public static String jmasarServiceUnavailable;
    public static String promptDeleteSnapshotTitle;
    public static String promptDeleteSnapshotContent;
    public static String promptDeleteSaveSetTitle;
    public static String promptDeleteSaveSetHeader;
    public static String promptDeleteSaveSetContent;
    public static String promptNewFolder;
    public static String promptDeleteFolderTitle;
    public static String promptDeleteFolderHeader;
    public static String promptDeleteFolderContent;
    public static String errorDeleteFolderTitle;
    public static String errorDeleteFolderContent;
    public static String promptNewSaveSetTitle;
    public static String promptNewSaveSetContent;
    public static String promptRenameNodeTitle;
    public static String promptRenameNodeContent;
    public static String buttonReconnect;
    public static String labelCreateFolderEmptyTree;

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation
    }

}
