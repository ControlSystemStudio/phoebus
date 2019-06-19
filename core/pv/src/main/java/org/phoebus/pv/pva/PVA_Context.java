/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import org.epics.pva.client.PVAClient;

/** Singleton that maintains the {@link PVAClient}
 *  @author Kay Kasemir
 */
public class PVA_Context
{
    private static PVA_Context instance;

    private final PVAClient client;

    private PVA_Context() throws Exception
    {
        client = new PVAClient();
    }

    public static synchronized PVA_Context getInstance() throws Exception
    {
        if (instance == null)
            instance = new PVA_Context();
        return instance;
    }

    PVAClient getClient()
    {
        return client;
    }
}
