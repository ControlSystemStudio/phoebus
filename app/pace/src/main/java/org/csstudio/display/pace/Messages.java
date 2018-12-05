/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace;

import org.phoebus.framework.nls.NLS;

/** Access to messages externalized to
 *  language-specific messages*.properties files.
 *  @author Kay Kasemir
 */
public class Messages
{
    public static String ConfirmChangesFmt;
    public static String FileChangedFmt;
    public static String FileUnchangedFmt;
    public static String InstanceLabelProvider_OrigAppendix;
    public static String InstanceLabelProvider_PVValueFormat;
    public static String InstanceLabelProvider_PVCommentTipFormat;
    public static String InstanceLabelProvider_ReadOnlyAppendix;
    public static String Password;
    public static String Preferences_DefaultLogbook;
    public static String Preferences_Message;
    public static String PVWriteErrorFmt;
    public static String RestoreCell;
    public static String RestoreCell_TT;
    public static String SaveError;
    public static String SaveErrorFmt;
    public static String SaveIntro;
    public static String ELogTitleFmt;
    public static String EntryTitle;
    public static String Logbook;
    public static String SavePVInfoFmt;
    public static String SaveCommentInfoFmt;
    public static String SaveTitle;
    public static String SetColumnValue_Msg;
    public static String SetValue;
    public static String SetValue_Msg;
    public static String SetValue_Msg_WithReadonlyWarning;
    public static String SetValue_Title;
    public static String SetValue_TT;
    public static String SystemColumn;
    public static String UnknownValue;
    public static String User;

    static
    {
        NLS.initializeMessages(Messages.class);
    }
}
