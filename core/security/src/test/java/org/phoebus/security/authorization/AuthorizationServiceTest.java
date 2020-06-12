/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.security.authorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Test;

/** JUnit test of the {@link FileBasedAuthorization}
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AuthorizationServiceTest
{
    private static String config_str =
        "alarm_ack=.*\n" +
        "alarm_config = fred, jane.*, egon\n" +
        "FULL = root\n";

    private static Authorization createAuthorization(final String user_name) throws Exception
    {
        final ByteArrayInputStream stream = new ByteArrayInputStream(config_str.getBytes());
        return new FileBasedAuthorization(stream, user_name);
    }

    @Test
    public void testAuthorizationService() throws Exception
    {
        // Acknowledge only.
        Authorization as = createAuthorization("blah");
        assertTrue(as.hasAuthorization("alarm_ack"));
        assertFalse(as.hasAuthorization("alarm_config"));

        // Acknowledge and Configure
        for (String user : List.of("fred", "jane", "janet", "egon"))
        {
            as = createAuthorization(user);
            assertTrue(as.hasAuthorization("alarm_ack"));
            assertTrue(as.hasAuthorization("alarm_config"));
        }

        assertTrue(as.isAuthorizationDefined("alarm_ack"));
        // 'bogus' is not defined, and not granted
        assertFalse(as.isAuthorizationDefined("bogus"));
        assertFalse(as.hasAuthorization("bogus"));

        // FULL
        as = createAuthorization("root");
        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        // 'bogus' is not defined, but covered by FULL=root
        assertFalse(as.isAuthorizationDefined("bogus"));
        assertTrue(as.hasAuthorization("bogus"));
    }
}
