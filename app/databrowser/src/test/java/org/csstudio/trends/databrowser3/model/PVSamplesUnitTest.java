/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.phoebus.archive.vtype.ArchiveVNumber;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueUtil;

/** JUnit test for PVSamples
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto added test case for waveform index
 */
@SuppressWarnings("nls")
public class PVSamplesUnitTest
{
    @Test
    public void testPVSamples()
    {
        final AtomicInteger waveform_index = new AtomicInteger(0);
        // Start w/ empty PVSamples
        final PVSamples samples = new PVSamples(waveform_index);
        assertEquals(0, samples.size());

        // Add 'historic' samples
        final List<VType> history = new ArrayList<VType>();
        for (int i=0; i<10; ++i)
            history.add(TestHelper.makeValue(i));
        samples.mergeArchivedData("Test", history);
        // PVSamples include continuation until 'now'
        System.out.println(samples.toString());
        assertEquals(history.size()+1, samples.size());

        // Add 2 'live' samples
        samples.addLiveSample(TestHelper.makeValue(samples.size()));
        samples.addLiveSample(TestHelper.makeValue(samples.size()));
        // PVSamples include history, live, continuation until 'now'
        System.out.println(samples.toString());
        assertEquals(history.size()+3, samples.size());

        // Add a non-numeric sample
        samples.addLiveSample(TestHelper.makeError(samples.size(), "Disconnected"));
        // PVSamples include history, live, NO continuation
        System.out.println(samples.toString());
        assertEquals(history.size()+3, samples.size());

        // Check if the history.setBorderTime() update works
        // Create 'history' data from 0 to 20.
        history.clear();
        for (int i=0; i<21; ++i)
            history.add(TestHelper.makeValue(i));
        samples.mergeArchivedData("Test", history);

        // Since 'live' data starts at 11, history is only visible up to there,
        // i.e. 0..10 = 11 in history plus 3 'live' samples
        assertEquals(11 + 3, samples.size());
        System.out.println(samples.toString());
    }

    /** When 'monitoring' a PV, IOCs will send data with zero time stamps
     *  for records that have never been processed.
     *  Check that time stamps are patched to host time.
     */
    @Test
    public void testUndefinedLiveData()
    {
        final AtomicInteger waveform_index = new AtomicInteger(0);
        // Start w/ empty samples
        final PVSamples samples = new PVSamples(waveform_index);
        assertEquals(0, samples.size());

        // Add sample w/ null time stamp, INVALID/UDF
        final Instant null_time = Instant.ofEpochMilli(0);
        VType value = new ArchiveVNumber(null_time, AlarmSeverity.NONE, "", null, 0.0);
        assertThat(ValueUtil.timeOf(value).isTimeValid(), equalTo(false));

        samples.addLiveSample(value);
        System.out.println("Original: " + value);

        // Should have that sample, plus copy that's extrapolated to 'now'
        assertEquals(2, samples.size());

        value = samples.get(0).getVType();
        System.out.println("Sampled : " + value);
        assertThat(ValueUtil.timeOf(value).isTimeValid(), equalTo(true));
    }
}
