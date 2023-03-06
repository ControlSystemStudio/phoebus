/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import org.csstudio.javafx.rtplot.TicksTestBase;
import org.csstudio.javafx.rtplot.internal.LinearTicks;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/** JUnit test
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LinearTicksTest extends TicksTestBase
{
    @Override
    @Test
    public void testPrecision()
    {
        assertThat(LinearTicks.determinePrecision(100.0), equalTo(0));
        assertThat(LinearTicks.determinePrecision(5.0), equalTo(1));
        assertThat(LinearTicks.determinePrecision(0.5), equalTo(2));
        assertThat(LinearTicks.determinePrecision(2e-6), equalTo(7));
    }

    @Override
    @Test
    public void testNiceDistance()
    {
        for (double order_of_magnitude : new double[] { 1.0, 0.0001, 1000.0, 1e12, 1e-7 })
        {
            assertThat(LinearTicks.selectNiceStep(10.0*order_of_magnitude), equalTo(10.0*order_of_magnitude));
            assertThat(LinearTicks.selectNiceStep(9.0*order_of_magnitude), equalTo(10.0*order_of_magnitude));
            assertThat(LinearTicks.selectNiceStep(7.0*order_of_magnitude), equalTo(10.0*order_of_magnitude));
            assertThat(LinearTicks.selectNiceStep(5.0*order_of_magnitude), equalTo(5.0*order_of_magnitude));
            assertThat(LinearTicks.selectNiceStep(4.0*order_of_magnitude), equalTo(5.0*order_of_magnitude));
            assertThat(LinearTicks.selectNiceStep(3.0*order_of_magnitude), equalTo(5.0*order_of_magnitude));
            assertThat(LinearTicks.selectNiceStep(2.01*order_of_magnitude), equalTo(2.0*order_of_magnitude));
            assertThat(LinearTicks.selectNiceStep(1.5*order_of_magnitude), equalTo(2.0*order_of_magnitude));
        }
    }

    @Test
    public void testNormalTicks()
    {
        final LinearTicks ticks = new LinearTicks();
        double start = 1.0,  end = 100.0;
        ticks.compute(start, end, gc, buf.getWidth());

        System.out.println("Ticks for " + start + " .. " + end + ":");
        String text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("2 4 6 8 '10' 12 14 16 18 '20' 22 24 26 28 '30' 32 34 36 38 '40' 42 44 46 48 '50' 52 54 56 58 '60' 62 64 66 68 '70' 72 74 76 78 '80' 82 84 86 88 '90' 92 94 96 98 '100' "));

        start = -0.6;  end = +0.6;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("-0.6 -0.5 -0.5 -0.4 '-0.4' -0.4 -0.3 -0.3 -0.2 '-0.2' -0.2 -0.1 -0.1 -0.0 '0.0' 0.0 0.1 0.1 0.2 '0.2' 0.2 0.3 0.3 0.4 '0.4' 0.4 0.5 0.5 0.6 "));

        start = -100.0;  end = 20000.0;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'0' 1000 2000 3000 4000 '5000' 6000 7000 8000 9000 '10000' 11000 12000 13000 14000 '15000' 16000 17000 18000 19000 '20000' "));
    }

    @Test
    public void testReverseTicks()
    {
        final LinearTicks ticks = new LinearTicks();
        double start = 10000.0,  end = 1.0;
        ticks.compute(start, end, gc, buf.getWidth());
        System.out.println("Ticks for " + start + " .. " + end + ":");
        final String text = ticks2text(ticks);
        System.out.println(text);
        assertThat(text, equalTo("'10000' 9600 9200 8800 8400 '8000' 7600 7200 6800 6400 '6000' 5600 5200 4800 4400 '4000' 3600 3200 2800 2400 '2000' 1600 1200 800 400 "));
    }

    @Test
    public void testShouldUseExpNotation() {
        final LinearTicks linearTicks = new LinearTicks();
        linearTicks.setExponentialThreshold(3);

        // Small orders of magnitude:
        assertThat(linearTicks.shouldUseExpNotation(0.0, 10.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 1.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 0.1), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 0.01), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 0.001), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 0.0001), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 0.00001), equalTo(true));

        assertThat(linearTicks.shouldUseExpNotation(-10.0, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-1.0, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.1, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.01, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.001, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.0001, 0.0), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(-0.00001, 0.0), equalTo(true));

        assertThat(linearTicks.shouldUseExpNotation(-10.0, 10.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-1.0, 1.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.1, 0.1), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.01, 0.01), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.001, 0.001), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.0001, 0.0001), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(-0.00001, 0.00001), equalTo(true));

        assertThat(linearTicks.shouldUseExpNotation(-9.0, 9.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.9, 0.9), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.09, 0.09), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.009, 0.009), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.0009, 0.0009), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(-0.00009, 0.00009), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(-0.000009, 0.000009), equalTo(true));

        assertThat(linearTicks.shouldUseExpNotation(-9.999999999, 9.999999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.9999999999, 0.9999999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.09999999999, 0.09999999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.009999999999, 0.009999999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(-0.0009999999999, 0.0009999999999), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(-0.00009999999999, 0.00009999999999), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(-0.000009999999999, 0.000009999999999), equalTo(true));

        // Large orders of magnitude:
        assertThat(linearTicks.shouldUseExpNotation(0.0, 1.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 10.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 100.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 1000.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 10000.0), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(0.0, 100000.0), equalTo(true));

        assertThat(linearTicks.shouldUseExpNotation(1.0, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(10.0, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(100.0, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(1000.0, 0.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(10000.0, 0.0), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(100000.0, 0.0), equalTo(true));

        assertThat(linearTicks.shouldUseExpNotation(1.0, 1.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(10.0, 10.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(100.0, 100.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(1000.0, 1000.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(10000.0, 10000.0), equalTo(true));
        assertThat(linearTicks.shouldUseExpNotation(100000.0, 100000.0), equalTo(true));

        assertThat(linearTicks.shouldUseExpNotation(0.9, 0.9), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(9.0, 9.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(90.0, 90.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(900.0, 900.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(9000.0, 9000.0), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(90000.0, 90000.0), equalTo(true));

        assertThat(linearTicks.shouldUseExpNotation(0.9999999999, 0.9999999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(9.999999999, 9.999999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(99.99999999, 99.99999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(999.9999999, 999.9999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(9999.999999, 9999.999999), equalTo(false));
        assertThat(linearTicks.shouldUseExpNotation(99999.99999, 99999.99999), equalTo(true));
    }
}
