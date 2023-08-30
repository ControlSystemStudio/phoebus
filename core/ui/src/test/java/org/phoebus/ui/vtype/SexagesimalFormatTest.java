/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.vtype;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/** JUnit test of {@link SexagesimalFormat}
 *  @author Kay Kasemir
 *  @author lcavalli provided original implementation for BOY,
 *  <a href='https://github.com/ControlSystemStudio/cs-studio/pull/1978'>see this reference</a>.
 */
@SuppressWarnings("nls")
public class SexagesimalFormatTest
{

    @Test
    public void testSexagesimal() throws Exception
    {
        double number = 12.5824414;
        String text = SexagesimalFormat.format(number, 7);
        double parsed = SexagesimalFormat.parse(text);
        System.out.println(number + " -> '" + text + "' -> " + parsed);
        assertThat(text, equalTo("12:34:56.789"));
        assertEquals(parsed, number, 0.0000001);

        number = -12.5824414;
        text = SexagesimalFormat.format(number, 7);
        parsed = SexagesimalFormat.parse(text);
        System.out.println(number + " -> '" + text + "' -> " + parsed);
        assertThat(text, equalTo("-12:34:56.789"));
        assertEquals(parsed, number, 0.0000001);

        number = 12.9999999;
        text = SexagesimalFormat.format(number, 7);
        parsed = SexagesimalFormat.parse(text);
        System.out.println(number + " -> '" + text + "' -> " + parsed);
        assertThat(text, equalTo("13:00:00.000"));
        assertEquals(parsed, number, 0.0000001);

        text = SexagesimalFormat.format(number, 8);
        parsed = SexagesimalFormat.parse(text);
        System.out.println(number + " -> '" + text + "' -> " + parsed);
        assertThat(text, equalTo("12:59:59.9996"));
        assertEquals(parsed, number, 0.0000001);

        text = "12:30";
        parsed = SexagesimalFormat.parse(text);
        System.out.println("'" + text + "' -> " + parsed);
        assertEquals(parsed, 12.5, 0.1);

        text = "12:30:01";
        parsed = SexagesimalFormat.parse(text);
        System.out.println("'" + text + "' -> " + parsed);
        assertEquals(parsed, 12.5 + 1.0/60/60, 0.000001);
    }
}
