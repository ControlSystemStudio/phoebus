/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import org.csstudio.javafx.rtplot.internal.LinearTicks;
import org.csstudio.javafx.rtplot.internal.MajorTick;
import org.csstudio.javafx.rtplot.internal.MinorTick;
import org.csstudio.javafx.rtplot.internal.Ticks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


/** Helper for testing ticks
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TicksTestBase
{
    protected final static BufferedImage buf = new BufferedImage(400, 50, BufferedImage.TYPE_INT_ARGB);
    protected final static Graphics2D gc = buf.createGraphics();

    @BeforeAll
    public static void setup()
    {
        // Set locate for predictable formatting results
        Locale.setDefault(Locale.US);
    }

    @AfterAll
    public static void cleanup()
    {
        gc.dispose();
    }

    /** Helper for displaying ticks as text
     *  @param ticks
     *  @return
     */
    public String ticks2text(final Ticks<Double> ticks)
    {
        final StringBuilder buf = new StringBuilder();

        final List<MajorTick<Double>> major_ticks = ticks.getMajorTicks();
        final List<MinorTick<Double>> minor_ticks = ticks.getMinorTicks();
        final Iterator<MajorTick<Double>> maj = major_ticks.iterator();
        final Iterator<MinorTick<Double>> min = minor_ticks.iterator();

        MajorTick<Double> major = maj.hasNext() ? maj.next() : null;
        MinorTick<Double> minor = min.hasNext() ? min.next() : null;

        if (major_ticks.size() > 0)
        {
            final boolean normal = major_ticks.get(0).getValue()
                                      <=
                                   major_ticks.get(major_ticks.size()-1).getValue();

            while (true)
            {
                if (major != null &&  minor != null)
                {
                    if (normal ? major.getValue() <= minor.getValue()
                               : major.getValue() >= minor.getValue())
                    {
                        buf.append("'").append(major.getLabel()).append("' ");
                        major = maj.hasNext() ? maj.next() : null;
                    }
                    else
                    {
                        buf.append(ticks.format(minor.getValue())).append(" ");
                        minor = min.hasNext() ? min.next() : null;
                    }
                }
                else if (major != null)
                {
                    buf.append("'").append(major.getLabel()).append("' ");
                    major = maj.hasNext() ? maj.next() : null;
                }
                else if (minor != null)
                {
                    buf.append(ticks.format(minor.getValue())).append(" ");
                    minor = min.hasNext() ? min.next() : null;
                }
                else
                    break;
            }
        }
        return buf.toString();
    }

    @Test
    public void testPrecision()
    {
        assertThat(LinearTicks.determinePrecision(100.0), equalTo(0));
        assertThat(LinearTicks.determinePrecision(5.0), equalTo(1));
        assertThat(LinearTicks.determinePrecision(0.5), equalTo(2));
        assertThat(LinearTicks.determinePrecision(2e-6), equalTo(7));
    }

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
            assertThat(LinearTicks.selectNiceStep(1.99*order_of_magnitude), equalTo(2.0*order_of_magnitude));
            assertThat(LinearTicks.selectNiceStep(1.5*order_of_magnitude), equalTo(2.0*order_of_magnitude));
        }
    }
}
