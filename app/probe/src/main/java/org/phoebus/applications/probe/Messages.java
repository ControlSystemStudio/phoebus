/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.probe;

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
    public static String Alarm;
    public static String Alarms;
    public static String ControlRange;
    public static String Copy;
    public static String DisplayRange;
    public static String EnumLbls;
    public static String Format;
    public static String Precision;
    public static String Metadata;
    public static String Probe;
    public static String ProbeMenuPath;
    public static String PromptTxt;
    public static String PvNameLbl;
    public static String Search;
    public static String TimeStamp;
    public static String Units;
    public static String Value;
    public static String Warnings;
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
