/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.time.Instant;

import org.csstudio.javafx.rtplot.data.ArrayPlotDataProvider;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.SimpleDataItem;
import org.csstudio.javafx.rtplot.data.TimeDataSearch;
import org.junit.Test;

/** JUnit test
 *  @author Kay Kasemir
 */
public class TraceAnalyzerTest
{
    @Test
    public void testFindSample() throws Exception
    {
        final ArrayPlotDataProvider<Instant> data = new ArrayPlotDataProvider<>();
        for (int i=0; i<=10; ++i)
            data.add(new SimpleDataItem<Instant>(Instant.ofEpochSecond(10*i), 10.0*i-50));

        for (int i=0; i<data.size(); ++i)
            System.out.println(data.get(i));

        TimeDataSearch search = new TimeDataSearch();

        int index = search.findClosestSample(data, Instant.ofEpochSecond(39));
        assertThat(index, not(equalTo(-1)));
        PlotDataItem<Instant> sample = data.get(index);
        assertThat(sample.getPosition().getEpochSecond(), equalTo(40L));
        assertThat(sample.getValue(), equalTo(-10.0));

        index = search.findClosestSample(data, Instant.ofEpochSecond(32));
        assertThat(index, not(equalTo(-1)));
        sample = data.get(index);
        assertThat(sample.getPosition().getEpochSecond(), equalTo(30L));
        assertThat(sample.getValue(), equalTo(-20.0));
    }
}
