/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;


/** JUnit test of Version
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VersionTest
{
    @Test
    public void testParse()
    {
        assertThat(Version.parse("2.0.1").getMajor(), equalTo(2));
        assertThat(Version.parse("2.0.1").getMinor(), equalTo(0));
        assertThat(Version.parse("2.0.1").getPatch(), equalTo(1));
    }

    @Test
    public void testShort()
    {
        assertThat(Version.parse("2.0").getMajor(), equalTo(2));
        assertThat(Version.parse("2.0").getMinor(), equalTo(0));
        assertThat(Version.parse("2.1").getMinor(), equalTo(1));
    }

    @Test
    public void testFormat()
    {
        assertThat(Version.parse("2.0.1").toString(), equalTo("2.0.1"));
    }

    @Test
    public void testCompare()
    {
        assertThat(Version.parse("2.0.1").compareTo(Version.parse("2.0.0")), equalTo(1));
        assertThat(Version.parse("2.0.1").compareTo(Version.parse("3.0.0")), equalTo(-1));
    }

    @Test
    public void testError()
    {
        try
        {
            Version.parse("2");
            fail("Didn't detect invalid version");
        }
        catch (IllegalArgumentException ex)
        {
            assertThat(ex.getMessage(), containsString("Invalid version string"));
        }
        try
        {
            Version.parse("2.1.2.3");
            fail("Didn't detect invalid version");
        }
        catch (IllegalArgumentException ex)
        {
            assertThat(ex.getMessage(), containsString("Invalid version string"));
        }
    }
}
