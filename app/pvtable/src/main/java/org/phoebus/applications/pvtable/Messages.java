/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable;

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
    public static String MenuPath;
    public static String PVTable;
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
