/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** JUnit test of {@link IdentificationHelper}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IdentificationHelperTest
{
    @Test
    public void testUserHost() throws Exception
    {
        // Originally, no information
        String user = IdentificationHelper.getUser();
        String host = IdentificationHelper.getHost();
        System.out.println("User: '" + user + "' at host: '" + host + "'");
        if ("???".equals(user)  &&  "???".equals(host))
        {
            System.out.println("Initializing IdentificationHelper");
            IdentificationHelper.initialize();
        }
        // else: Some other test that ran first already triggered initialization

        // On successful initialization, there should soon be information
        int wait = 0;
        while (host.equals("???"))
        {
            ++wait;
            assertTrue(wait < 10, "Timed out waiting for information");
            TimeUnit.SECONDS.sleep(1);
            user = IdentificationHelper.getUser();
            host = IdentificationHelper.getHost();
            System.out.println("User: '" + user + "' at host: '" + host + "'");
        }
    }
}
