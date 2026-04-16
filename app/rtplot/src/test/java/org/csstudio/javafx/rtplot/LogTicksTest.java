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

import java.awt.FontMetrics;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/** JUnit test for {@link LogTicks}.
 *
 *  <p>Each test case prints the tick text produced by {@link TicksTestBase#ticks2text}
 *  so failures are easy to diagnose.  In the output, labels enclosed in single
 *  quotes ({@code 'label'}) are major (decade) ticks; bare values are minor ticks.
 *
 *  <p>All decade labels use compact exponential notation (e.g. {@code 1E0},
 *  {@code 1E3}) for visual consistency.  A future {@code propFormat} /
 *  {@code propPrecision} property will let users override this per-widget.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LogTicksTest extends TicksTestBase
{
    @Test
    public void testLogTicks()
    {
        final LogTicks ticks = new LogTicks();

        // Four-decade range: all decades labeled, sub-decade values shown as minor ticks.
        double start = 1.0,  end = 10000.0;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        String text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'1E0' 1E0 2E0 3E0 4E0 5E0 6E0 7E0 8E0 9E0 '1E1' 1E1 2E1 3E1 4E1 5E1 6E1 7E1 8E1 9E1 '1E2' 1E2 2E2 3E2 4E2 5E2 6E2 7E2 8E2 9E2 '1E3' 1E3 2E3 3E3 4E3 5E3 6E3 7E3 8E3 9E3 '1E4' 1E4 "));

        // Nine-decade range: all decades still fit in the 400 px test buffer
        // (font height ≈ 14 px → ~20 labels fit; 9 decades ≤ 20).
        start = 1.0;  end = 1e8;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'1E0' 1E0 2E0 3E0 4E0 5E0 6E0 7E0 8E0 9E0 '1E1' 1E1 2E1 3E1 4E1 5E1 6E1 7E1 8E1 9E1 '1E2' 1E2 2E2 3E2 4E2 5E2 6E2 7E2 8E2 9E2 '1E3' 1E3 2E3 3E3 4E3 5E3 6E3 7E3 8E3 9E3 '1E4' 1E4 2E4 3E4 4E4 5E4 6E4 7E4 8E4 9E4 '1E5' 1E5 2E5 3E5 4E5 5E5 6E5 7E5 8E5 9E5 '1E6' 1E6 2E6 3E6 4E6 5E6 6E6 7E6 8E6 9E6 '1E7' 1E7 2E7 3E7 4E7 5E7 6E7 7E7 8E7 9E7 '1E8' 1E8 "));

        // Sub-decade range: the log tick strategy yields fewer than two decade
        // ticks, so a linear spacing is used as a fallback.
        start = 1001.0;  end = 1234.0;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'1.025E3' '1.050E3' '1.075E3' '1.100E3' '1.125E3' '1.150E3' '1.175E3' '1.200E3' '1.225E3' "));

        // Regression: even number of decades must always show the top tick label.
        // 1E-3 to 1E4 spans 8 exponents {-3,-2,-1,0,1,2,3,4} — an even count.
        start = 1e-3;  end = 1e4;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        assertThat("Top tick must be present for even-decade range",
                   text.contains("'1E4'"), equalTo(true));
        assertThat("Bottom tick must be present for even-decade range",
                   text.contains("'1E-3'"), equalTo(true));
    }

    @Test
    public void testMidpointSymmetry()
    {
        // Regression for the halving-loop bias: with an 11-decade range (E0..E10)
        // and space for only 3 labels, the old halving algorithm produced the
        // labeled set {0, 4, 10} — the middle label at 40% not 50%.
        // The evenly-spaced selection must produce {0, 5, 10} — symmetric.
        //
        // We use a narrow axis so that numLabelsThatFit == 3.
        // numLabelsThatFit uses measured string widths; "1E0" and "1E10" are
        // the bottom/top labels for this range.  At 3 labels the thinned set
        // must contain the true midpoint E5.
        final FontMetrics fm = gc.getFontMetrics();
        // Make the axis just wide enough for 3 labels (with FILL_PERCENTAGE=70):
        //   width * 70/100 / labelSize == 3  →  width = 3 * labelSize * 100/70 + 1
        // Use the ticks object to format sample labels identically to production.
        final LogTicks sample = new LogTicks();
        sample.compute(1.0, 1e10, gc, 10000);   // compute into a very wide axis so all decades are labeled
        // The widest label in range 1..1E10 is "1E10" (5 chars with sign).
        final int labelSize = Math.max(
                fm.stringWidth(sample.format(1.0)),
                fm.stringWidth(sample.format(1e10)));
        final int width = 3 * labelSize * 100 / 70 + 1;

        final LogTicks ticks = new LogTicks();
        ticks.compute(1.0, 1e10, gc, width);
        System.out.println("Ticks for 1.0 .. 1E10 (width=" + width + ", labelSize=" + labelSize + "):");
        final String text = ticks2text(ticks);
        System.out.println(text);

        assertThat("Bottom label must be present", text.contains("'1E0'"),  equalTo(true));
        assertThat("Middle label must be at exponent 5 (true midpoint)",
                   text.contains("'1E5'"),  equalTo(true));
        assertThat("Top label must be present",    text.contains("'1E10'"), equalTo(true));
    }
}
