/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import org.epics.pva.PVASettings;

/** Start PVA Server Demo using IPv6
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IPv6ServerDemo
{
    public static void main(String[] args) throws Exception
    {
        // Use IPv6 'localhost'
        PVASettings.EPICS_PVAS_INTF_ADDR_LIST = "::1";

        ServerDemo.main(args);
    }
}
