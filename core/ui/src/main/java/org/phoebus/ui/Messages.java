/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/ep8l-v10.html
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
    public static String DoNotShow;
    public static String Format_Binary;
    public static String Format_Compact;
    public static String Format_Decimal;
    public static String Format_Default;
    public static String Format_Engineering;
    public static String Format_Exponential;
    public static String Format_Hexadecimal;
    public static String Format_Sexagesimal;
    public static String Format_SexagesimalDMS;
    public static String Format_SexagesimalHMS;
    public static String Format_String;
    public static String InstallExamples;
    public static String MagicLastRow;
    public static String MoveColumnLeft;
    public static String MoveColumnRight;
    public static String MoveRowDown;
    public static String MoveRowUp;
    public static String NumberInputHdr;
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
