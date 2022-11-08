/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

import org.csstudio.archive.Preferences;
import org.csstudio.archive.writer.WriteChannel;
import org.epics.util.array.ArrayDouble;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VInt;
import org.epics.vtype.VLong;
import org.epics.vtype.VString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

//import org.junit.Ignore;

/** Archive Writer Demo
 *
 *  <p>Main purpose of these tests is to run in debugger, step-by-step,
 *  so verify if correct RDB entries are made.
 *  The sources don't include anything to check the raw RDB data.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBArchiveWriterDemo
{
    final Display display = Display.of(Range.of(0, 10), Range.of(1, 9), Range.of(2,  8), Range.of(0, 10), "a.u.", NumberFormats.precisionFormat(2));

    private final String name = "jane", array_name = "sim://noiseWaveform(0,10,100,10)";
    private static RDBArchiveWriter writer;

    @BeforeAll
    public static void setup() throws Exception
    {
        writer = new RDBArchiveWriter(Preferences.url, Preferences.user, Preferences.password, Preferences.schema, Preferences.use_array_blob);
    }

    @AfterAll
    public static void close()
    {
        if (writer != null)
            writer.close();
    }

    @Test
    public void testChannelLookup() throws Exception
    {
        if (writer == null)
            return;
        WriteChannel channel = writer.getChannel(name);
        System.out.println(channel);
        assertThat(channel, not(nullValue()));
        assertThat(name, equalTo(channel.getName()));

        if (array_name == null)
            return;
        channel = writer.getChannel(array_name);
        System.out.println(channel);
        assertThat(channel, not(nullValue()));
        assertThat(array_name, equalTo(channel.getName()));
    }

    @Test
    public void testWriteDouble() throws Exception
    {
        if (writer == null)
            return;
        System.out.println("Writing double sample for channel " + name);
        final WriteChannel channel = writer.getChannel(name);
        // Write double
        writer.addSample(channel, VDouble.of(3.14,  Alarm.none(), Time.now(), display));
        // .. double that could be int
        writer.addSample(channel, VLong.of(3L, Alarm.none(), Time.now(), display));
        writer.flush();
    }

    @Test
    public void testWriteDoubleArray() throws Exception
    {
        if (writer == null  ||  array_name == null)
            return;
        System.out.println("Writing double array sample for channel " + array_name);
        final WriteChannel channel = writer.getChannel(array_name);
        writer.addSample(channel, VDoubleArray.of(ArrayDouble.of(10, 2, 3, 4), Alarm.none(), Time.now(), display));
        writer.flush();
    }

    @Test
    public void testWriteLongEnumText() throws Exception
    {
        if (writer == null)
            return;
        final WriteChannel channel = writer.getChannel(name);

        // Enum, sets enumerated meta data
        writer.addSample(channel, VEnum.of(0, EnumDisplay.of("Zero", "One"), Alarm.none(), Time.now()));
        writer.addSample(channel, VEnum.of(1, EnumDisplay.of("Zero", "One"), Alarm.of(AlarmSeverity.MINOR, AlarmStatus.DB, "STATE"), Time.now()));
        writer.flush();

        // Writing string leaves the enumerated meta data untouched
        writer.addSample(channel, VString.of("Hello", Alarm.none(), Time.now()));
        writer.flush();

        // Integer, sets numeric meta data
        writer.addSample(channel, VInt.of(42, Alarm.none(), Time.now(), display));
        writer.flush();
    }
}
