/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.filebrowser;

import org.phoebus.framework.nls.NLS;

/** Eclipse string externalization
 *  @author Kay Kasemir
 *  @author Pavel Charvat
 */
public class Messages
{
    // ---
    // --- Keep alphabetically sorted and 'in sync' with messages.properties!
    // ---
    public static String BaseDirectorySelTT;
    public static String BaseDirectoryTT;
    public static String BrowserRootTitle;
    public static String ColName;
    public static String ColSize;
    public static String ColTime;
    public static String CopyPathClp;
    public static String CreateDirectoryErr;
    public static String CreateDirectoryHdr;
    public static String Delete;
    public static String DeleteJobName;
    public static String DeletePromptHeader;
    public static String DeletePromptTitle;
    public static String DisplayName;
    public static String Duplicate;
    public static String DuplicateAlert1;
    public static String DuplicateAlert2;
    public static String DuplicateJobName;
    public static String DuplicatePrefix;
    public static String DuplicatePromptHeader;
    public static String HomeButtonTT;
    public static String LookupJobName;
    public static String MenuPath;
    public static String MoveOrCopyAlert;
    public static String MoveOrCopyAlertTitle;
    public static String MoveOrCopyJobName;
    public static String NewFolder;
    public static String NewFolderAlert;
    public static String Open;
    public static String OpenAlert1;
    public static String OpenAlert2;
    public static String OpenWith;
    public static String Paste;
    public static String PropDlgBytes;
    public static String PropDlgDate;
    public static String PropDlgExecutable;
    public static String PropDlgPath;
    public static String PropDlgPermissions;
    public static String PropDlgSize;
    public static String PropDlgTitle;
    public static String PropDlgWritable;
    public static String Properties;
    public static String Refresh;
    public static String Rename;
    public static String RenameHdr;
    public static String RenameJobName;
    public static String SetBaseDirectory;
    // ---
    // --- Keep alphabetically sorted and 'in sync' with messages.properties!
    // ---

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation
    }
}
