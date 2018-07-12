/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/** JUnit test for PVSamples
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVItemUnitTest
{
    private static final int secs = 5;

    @Test
    public void testPVItemMonitor() throws Exception
    {
        final PVItem item = new PVItem("sim://ramp", 0.0);
        System.err.println("Monitor " + item + " for " + secs + " seconds...");
        // Expect about 1 sample per second from the PV
        checkSampleCount(item, secs);
    }

    @Test
    public void testPVItemScan() throws Exception
    {
        final PVItem item = new PVItem("sim://ramp", 0.5);
        System.err.println("Scan " + item + " for " + secs + " seconds...");
        // Expect about 2 samples per second from the PV
        checkSampleCount(item, 2*secs);
    }


    private void checkSampleCount(final PVItem item, final int expected) throws Exception
    {
        // Should initially have no samples...
        PVSamples samples = item.getSamples();
        System.out.println(samples);
        assertThat(samples.size(), equalTo(0));
        assertThat(samples.hasNewSamples(), equalTo(false));

        item.start();
        TimeUnit.SECONDS.sleep(secs);
        item.stop();

        samples = item.getSamples();
        System.out.println(samples);
        assertThat(samples.hasNewSamples(), equalTo(true));

        final int count = samples.size();
        final int perc_diff = Math.abs(count - expected) * 100 / expected;
        System.out.println("Got " + count + " samples, expected " + expected + ", difference of " + perc_diff + "%");
        assertThat(perc_diff < 50, equalTo(true));
    }
}
