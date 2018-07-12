/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.javafx.rtplot.internal.LinearTicks;
import org.junit.Test;

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
}
