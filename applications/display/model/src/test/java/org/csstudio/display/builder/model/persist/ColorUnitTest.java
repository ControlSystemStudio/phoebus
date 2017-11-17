/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.junit.Test;

/** JUnit test of color handling
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ColorUnitTest
{
    /** Test fetching named colors
     *  @throws Exception on error
     */
    @Test
    public void testNamedColors() throws Exception
    {
        final NamedWidgetColors colors = new NamedWidgetColors();

        NamedWidgetColor color = colors.getColor(NamedWidgetColors.ALARM_MAJOR).orElse(null);
        System.out.println(color);
        assertThat(color, not(nullValue()));
        assertThat(color.getRed(), equalTo(255));

        color = colors.getColor("STOP").orElse(null);
        assertThat(color, nullValue());

        colors.read(getClass().getResourceAsStream("/examples/color.def"));

        color = colors.getColor("STOP").orElse(null);
        System.out.println(color);
        assertThat(color, not(nullValue()));
    }

    /** Test fetching named colors
     *  @throws Exception on error
     */
    @Test
    public void testDefaultColors() throws Exception
    {
        final NamedWidgetColors colors = new NamedWidgetColors();

        // Fetch default alarm color
        NamedWidgetColor color = colors.getColor(NamedWidgetColors.ALARM_MAJOR).orElse(null);
        System.out.println(color);
        assertThat(color, not(nullValue()));
        assertThat(color.getRed(), equalTo(255));

        // Redefine, temporarily
        colors.define(new NamedWidgetColor(color.getName(), 0, 0, 0));
        color = colors.getColor(NamedWidgetColors.ALARM_MAJOR).orElse(null);
        System.out.println(color);
        assertThat(color.getRed(), equalTo(0));

        // STOP is not known by default
        color = colors.getColor("STOP").orElse(null);
        assertThat(color, nullValue());

        // Load STOP, also load MAJOR back to default
        colors.read(getClass().getResourceAsStream("/examples/color.def"));

        color = colors.getColor(NamedWidgetColors.ALARM_MAJOR).orElse(null);
        System.out.println(color);
        assertThat(color, not(nullValue()));
        assertThat(color.getRed(), equalTo(255));

        color = colors.getColor("STOP").orElse(null);
        System.out.println(color);
        assertThat(color, not(nullValue()));
    }

    /** Test fetching named colors from service
     *
     *  Time-based test, may occasionally fail because background thread doesn't get to run as expected
     *
     *  @throws Exception on error
     */
    @Test(timeout=30000)
    public void testColorService() throws Exception
    {
        System.out.println("On " + Thread.currentThread().getName());

        // Default colors do not include 'STOP'
        NamedWidgetColors colors = WidgetColorService.getColors();
        NamedWidgetColor color = colors.getColor("STOP").orElse(null);

        if (color != null)
        {
            // When another test already loaded the colors, this test is obsolete
            System.out.println("Another test already loaded the examples/color.def");
            return;
        }

        int delay_seconds = 10;

        // Load colors, using a source with artificial delay
        final DelayedStream slow_color_source = new DelayedStream("/examples/color.def");
        WidgetColorService.loadColors(new String[] { "Slow file" }, file -> slow_color_source.call());

        // Getting the colors is now delayed by the WidgetColorService.LOAD_DELAY
        long start = System.currentTimeMillis();
        colors = WidgetColorService.getColors();
        double seconds = (System.currentTimeMillis() - start) / 1000.0;
        System.out.println("Loading the slow color file took " + seconds + " seconds");

        // Should get default names, because slow_color_source is not ready, yet
        color = colors.getColor("STOP").orElse(null);
        System.out.println("'STOP' should be null: " + color);
        assertThat(color, nullValue());

        // The file is still loading, and eventually we should get it
        slow_color_source.proceed();
        TimeUnit.SECONDS.sleep(delay_seconds);
        start = System.currentTimeMillis();
        colors = WidgetColorService.getColors();
        seconds = (System.currentTimeMillis() - start) / 1000.0;
        System.out.println("Fetching colors after the file got loaded took " + seconds + " seconds");

        color = colors.getColor("STOP").orElse(null);
        System.out.println(color);
        assertThat(color, not(nullValue()));
    }
}
