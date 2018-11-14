/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui;

import org.phoebus.framework.nls.NLS;

/** Externalized Strings
 *  @author Kay Kasemir
 */
public class Messages
{
    // Keep in alphabetical order, matching the order in messages.properties
    public static String AddColumn;
    public static String AddRow;
    public static String DefaultNewColumnName;
    public static String InstallExamples;
    public static String MagicLastRow;
    public static String MoveColumnLeft;
    public static String MoveColumnRight;
    public static String MoveRowDown;
    public static String MoveRowUp;
    public static String Redo_TT;
    public static String RemoveColumn;
    public static String RemoveRow;
    public static String RenameColumn;
    public static String RenameColumnInfo;
    public static String RenameColumnTitle;
    public static String ReplaceExamplesWarningFMT;
    public static String Undo_TT;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
    }
}
