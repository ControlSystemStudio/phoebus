/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree.ui;

import org.phoebus.framework.nls.NLS;

public class Messages extends NLS
{
    public static String CollapseTT;
    public static String ExpandAlarmsTT;
    public static String ExpandAllTT;
    public static String LatchTT;
    public static String PV;
    public static String PV_Label;
    public static String PV_TT;
    public static String UnknownPVType;


    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // prevent instantiation
    }
}
