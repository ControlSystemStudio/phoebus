/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.jobs;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.phoebus.framework.jobs.BasicJobMonitor;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.SubJobMonitor;

/** Demo of the JobMonitor API
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JobMonitorTest
{
    @Test
    public void testIndeterminateJobMonitor() throws Exception
    {
        final JobMonitor monitor = new BasicJobMonitor();
        monitor.beginTask("Whatever");
        assertThat(monitor.getPercentage(), equalTo(-1));
        monitor.done();
        assertThat(monitor.getPercentage(), equalTo(100));
    }

    @Test
    public void testJobMonitorAccounting() throws Exception
    {
        final JobMonitor monitor = new BasicJobMonitor();
        monitor.beginTask("Steps", 3);
        assertThat(monitor.getPercentage(), equalTo(0));
        monitor.worked(1);
        assertThat(monitor.getPercentage(), equalTo(33));
        monitor.worked(1);
        assertThat(monitor.getPercentage(), equalTo(66));
        monitor.done();
        assertThat(monitor.getPercentage(), equalTo(100));
    }

    @Test
    public void testSubJobMonitor() throws Exception
    {
        final JobMonitor monitor = new BasicJobMonitor();
        monitor.beginTask("Startup", 4);

        JobMonitor sub = new SubJobMonitor(monitor, 2);
        sub.beginTask("Sub");
        // Nothing done, yet
        assertThat(sub.getPercentage(), equalTo(0));
        assertThat(monitor.getPercentage(), equalTo(0));

        // Sub task completes 2 out of 4 total steps
        sub.done();
        assertThat(monitor.getPercentage(), equalTo(50));



        // Another sub monitor for the remaining 2 steps,
        // which internally splits its work into 10 steps
        sub = new SubJobMonitor(monitor, 2);
        sub.beginTask("Sub", 10);
        // Nothing done, yet
        assertThat(sub.getPercentage(), equalTo(0));
        assertThat(monitor.getPercentage(), equalTo(50));

        // Sub monitor performs 1 step of its 10,
        // which is 10% of its work,
        // but doesn't register in the parent monitor
        sub.worked(1);
        assertThat(sub.getPercentage(), equalTo(10));
        assertThat(monitor.getPercentage(), equalTo(50));

        // Sub monitor performs total of 5 step of its 10,
        // which is 50% of its work,
        // adding one more step to the parent -> 3 of 4 in parent
        sub.worked(4);
        assertThat(sub.getPercentage(), equalTo(50));
        assertThat(monitor.getPercentage(), equalTo(75));

        // Sub monitor completes its 10, parent is also done
        sub.worked(5);
        assertThat(sub.getPercentage(), equalTo(100));
        assertThat(monitor.getPercentage(), equalTo(100));

        // Sub monitor cannot count, and keeps reporting more steps
        sub.worked(47);
        assertThat(sub.getPercentage(), equalTo(100));
        assertThat(monitor.getPercentage(), equalTo(100));
    }
}
