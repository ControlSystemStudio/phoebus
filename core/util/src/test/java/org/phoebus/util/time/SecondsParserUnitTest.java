/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.util.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

/** JUnit test of the SecondsParser
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SecondsParserUnitTest
{
    @Before
    public void setup()
    {
        // Check if test runs OK outside of US where it was originally written
        Locale.setDefault(Locale.FRENCH);
    }

    @Test
    public void testParseSeconds() throws Exception
    {
        assertEquals(42.0, SecondsParser.parseSeconds("42"), 0.01);
        assertEquals(10.0, SecondsParser.parseSeconds(" 10  "), 0.01);
        assertEquals(3600.2, SecondsParser.parseSeconds("  3600.2 "), 0.01);

        assertEquals(60.0, SecondsParser.parseSeconds("1:0"), 0.01);
        assertEquals(120.0, SecondsParser.parseSeconds(" 02:000"), 0.01);
        assertEquals(90.0, SecondsParser.parseSeconds("   1:30"), 0.01);

        assertEquals(60.0*60.0, SecondsParser.parseSeconds(" 1: 0:0"), 0.01);
        assertEquals(60.0*60.0 + 30*60 + 20, SecondsParser.parseSeconds("1:30:20"), 0.01);
        assertEquals(24*60.0*60.0, SecondsParser.parseSeconds("24:0:  0"), 0.01);
    }

    @Test
    public void testParseOddballs() throws Exception
    {
        assertEquals(-42.0, SecondsParser.parseSeconds("-42"), 0.01);

        assertEquals(-90.0, SecondsParser.parseSeconds("-1:30"), 0.01);

        assertEquals(-24*60.0*60.0, SecondsParser.parseSeconds("-24:0:0"), 0.01);

        boolean caught = false;
        try
        {
            SecondsParser.parseSeconds("    ");
        }
        catch (Exception e)
        {
            caught = true;
        }
        assertTrue(caught);

        caught = false;
        try
        {
            SecondsParser.parseSeconds("  1:2:3:4  ");
        }
        catch (Exception e)
        {
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public void testFormat()
    {
        assertEquals("00:00:10", SecondsParser.formatSeconds(10.0));
        assertEquals("00:01:00", SecondsParser.formatSeconds(60.0));
        assertEquals("00:01:30", SecondsParser.formatSeconds(90.0));
        assertEquals("01:00:00", SecondsParser.formatSeconds(60*60.0));
        assertEquals("01:30:00", SecondsParser.formatSeconds(90*60.0));
        assertEquals("00:00:00.000", SecondsParser.formatSeconds(0.0));
        assertEquals("00:00:00.500", SecondsParser.formatSeconds(0.5));
        assertEquals("-00:00:00.500", SecondsParser.formatSeconds(-0.5));
        assertEquals("00:00:00.001", SecondsParser.formatSeconds(0.001));
    }
}
