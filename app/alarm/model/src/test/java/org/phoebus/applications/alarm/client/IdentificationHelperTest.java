/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

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
        assertThat(user, equalTo("???"));
        assertThat(host, equalTo("???"));

        // On successful initilization, there should soon be information
        IdentificationHelper.initialize();
        int wait = 0;
        while (host.equals("???"))
        {
            ++wait;
            assertThat("Timed out waiting for information", wait < 10, equalTo(true));
            TimeUnit.SECONDS.sleep(1);
            user = IdentificationHelper.getUser();
            host = IdentificationHelper.getHost();
            System.out.println("User: '" + user + "' at host: '" + host + "'");
        }
    }
}
