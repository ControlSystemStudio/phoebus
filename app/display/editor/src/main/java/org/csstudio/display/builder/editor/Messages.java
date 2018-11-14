/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
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
    public static String AddElement;
    public static String AddWidget;
    public static String AlignBottom;
    public static String AlignCenter;
    public static String AlignLeft;
    public static String AlignMiddle;
    public static String AlignRight;
    public static String AlignTop;
    public static String Copy;
    public static String CreateGroup;
    public static String Delete;
    public static String DistributeHorizontally;
    public static String DistributeVertically;
    public static String DownloadPromptFMT;
    public static String DownloadTitle;
    public static String EditEmbededDisplay;
    public static String FindWidget;
    public static String Grid;
    public static String LoadDisplay;
    public static String LoadDisplay_TT;
    public static String MacroEditButton;
    public static String MatchHeight;
    public static String MatchWidth;
    public static String MoveDown;
    public static String MoveToBack;
    public static String MoveToFront;
    public static String MoveUp;
    public static String NewDisplay;
    public static String PointCount_Fmt;
    public static String PropertyFilterTT;
    public static String ReloadClasses;
    public static String ReloadDisplay;
    public static String RemoveElement;
    public static String RemoveGroup;
    public static String RemoveWidgets;
    public static String ReplaceWith;
    public static String RuleCountFMT;
    public static String Run;
    public static String SaveDisplay;
    public static String SaveDisplay_TT;
    public static String ScriptCountFMT;
    public static String SearchTextField;
    public static String SetPropertyFmt;
    public static String SetWidgetPoints;
    public static String ShowCoordinates;
    public static String Snap;
    public static String UpdateWidgetLocation;
    public static String UpdateWidgetOrder;
    public static String UseWidgetClass_TT;
    public static String UsingWidgetClass_TT;
    public static String WidgetFilterTT;
    public static String WT_FromString_dialog_content;
    public static String WT_FromString_dialog_headerFMT;
    public static String WT_FromString_dialog_title;
    public static String WT_FromString_multipleFMT;
    public static String WT_FromString_singleFMT;
    public static String WT_FromURL_dialog_content;
    public static String WT_FromURL_dialog_headerFMT;
    public static String WT_FromURL_dialog_title;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }
}
