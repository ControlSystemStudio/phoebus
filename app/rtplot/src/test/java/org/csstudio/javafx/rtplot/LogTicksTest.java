/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

//
//import org.csstudio.javafx.rtplot.internal.LinearTicks;
import org.csstudio.javafx.rtplot.internal.LogTicks;
import org.junit.Test;

/** JUnit test
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LogTicksTest extends TicksTestBase
{
    @Test
    public void testLogTicks()
    {
        final LogTicks ticks = new LogTicks();

        // 'Normal' log scale with majors at 1E0, 1E1, 1E2, ..
        double start = 1.0,  end = 10000.0;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        String text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'1E0' '3E3' '5E3' '8E3' '1E4' "));

        // Wider log scale with majors at 1E0, 1E2, 1E4, ..
        start = 1.0;  end = 1e8;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'1E0' 1E1 2E1 3E1 4E1 5E1 6E1 7E1 8E1 9E1 '1E2' 1E3 2E3 3E3 4E3 5E3 6E3 7E3 8E3 9E3 '1E4' 1E5 2E5 3E5 4E5 5E5 6E5 7E5 8E5 9E5 '1E6' 1E7 2E7 3E7 4E7 5E7 6E7 7E7 8E7 9E7 '1E8' "));

        // Log scale with same exponents for all ticks
        start = 1001.0;  end = 1234.0;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        // Needs to show significant detail in mantissa
        assertThat(text, equalTo("'1.001E3' '1.059E3' '1.118E3' '1.176E3' '1.234E3' "));

    }
}
