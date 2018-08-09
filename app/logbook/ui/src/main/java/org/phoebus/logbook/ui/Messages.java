/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui;

import org.phoebus.framework.nls.NLS;

public class Messages
{
    public static String Add,                              
                         Add_Tooltip,
                         Apply,
                         Available, 
                         Clear,                                
                         Clear_Tooltip,
                         Num_Selected,
                         Remove,
                         Remove_Tooltip,
                         Search,
                         Selected;
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
