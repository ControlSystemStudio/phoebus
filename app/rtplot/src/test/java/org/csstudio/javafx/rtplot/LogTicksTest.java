/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import org.csstudio.javafx.rtplot.internal.LogTicks;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(text, equalTo("'1E0' 1E0 2E0 3E0 4E0 5E0 6E0 7E0 8E0 9E0 '1E1' 1E1 2E1 3E1 4E1 5E1 6E1 7E1 8E1 9E1 '1E2' 1E2 2E2 3E2 4E2 5E2 6E2 7E2 8E2 9E2 '1E3' 1E3 2E3 3E3 4E3 5E3 6E3 7E3 8E3 9E3 '1E4' 1E4 "));

        // Wider log scale with majors at 1E0, 1E2, 1E4, ..
        start = 1.0;  end = 1e8;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'1E0' '1E2' '1E4' '1E6' '1E8' "));

        // Log scale with same exponents for all ticks
        start = 1001.0;  end = 1234.0;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'1.05E3' '1.10E3' '1.15E3' '1.20E3' "));

    }
}
