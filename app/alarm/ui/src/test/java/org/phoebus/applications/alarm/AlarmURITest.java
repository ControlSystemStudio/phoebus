/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.Test;
import org.phoebus.applications.alarm.ui.AlarmURI;

/** {@link AlarmURI} Tests
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmURITest
{
    @Test
	public void testCreateURI()
	{
        final URI uri = AlarmURI.createURI("localhost:9092", "Accelerator");
        System.out.println(uri);
		assertThat(uri.toString(), equalTo("alarm://localhost:9092/Accelerator"));
	}

    @Test
    public void testParseURI() throws Exception
    {
        String[] parsed = AlarmURI.parseAlarmURI(URI.create("alarm://localhost:9092/Accelerator"));
        assertThat(parsed.length, equalTo(2));
        assertThat(parsed[0], equalTo("localhost:9092"));
        assertThat(parsed[1], equalTo("Accelerator"));

        // Default port
        parsed = AlarmURI.parseAlarmURI(URI.create("alarm://host.my.site/Test"));
        assertThat(parsed.length, equalTo(2));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));

        try
        {
            AlarmURI.parseAlarmURI(URI.create("alarm://server_but_no_config"));
            fail("Didn't catch missing config name");
        }
        catch (Exception ex)
        {
            // Expected
            assertThat(ex.getMessage(), containsString("expecting"));
        }
    }
}
