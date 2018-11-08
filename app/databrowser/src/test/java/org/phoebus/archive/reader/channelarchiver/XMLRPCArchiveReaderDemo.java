/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import java.time.Instant;
import java.util.List;

import org.epics.vtype.VType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;
import org.phoebus.util.time.TimestampFormats;

/** Demo of XML-RPC archive reader
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLRPCArchiveReaderDemo
{
    // Meant for the 'ArchiveDataServerStandalone' with ChannelArchiver/DemoData/index

    private static ArchiveReaderFactory factory;
    private static ArchiveReader reader;

    @BeforeClass
    public static void setup() throws Exception
    {
        factory = new XMLRPCArchiveReaderFactory();
        reader = factory.createReader("xnds://ics-web4.sns.ornl.gov:8080/RPC2?key=1");
    }

    @Test
    public void testInfo() throws Exception
    {
        System.out.println(reader.getDescription());
    }

    @Test
    public void testNames() throws Exception
    {
        System.out.println(reader.getNamesByPattern(""));
        System.out.println(reader.getNamesByPattern("Tnk"));
    }

    @Test
    public void testRaw() throws Exception
    {
        Instant start = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-03-05 00:00"));
        Instant end = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2005-03-10 00:00"));

        for (String name : List.of("DTL_HPRF:Tnk1:T",
                                   "EnumPV",
                                   "TextPV",
                                   "ExampleArray",
                                   "U16PV",
                                   "jane",
                                   "fred",
                                   "alan"))
        {
            System.out.println(name);
            ValueIterator values = reader.getRawValues(name, start, end);
            int count = 0;
            while (values.hasNext())
            {
                final VType value = values.next();
                System.out.println(value);
                ++count;
            }
            System.out.println(count + " samples");
        }
    }

    @Test
    public void testOptimized() throws Exception
    {
        Instant start = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-03-10 00:00"));
        Instant end = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-03-12 00:00"));

        String name = "DTL_HPRF:Tnk1:T";
        System.out.println(name);
        ValueIterator values = reader.getOptimizedValues(name, start, end, 100);
        while (values.hasNext())
        {
            final VType value = values.next();
            System.out.println(value);
        }
    }
}
