/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import java.time.Instant;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;
import org.phoebus.util.time.TimestampFormats;

/** Demo of XML-RPC archive reader
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Demo
{
    public static void main(String[] args) throws Exception
    {
        final ArchiveReaderFactory factory = new XMLRPCArchiveReaderFactory();
        final ArchiveReader reader = factory.createReader("xnds://ics-web4.sns.ornl.gov:8080/RPC2?key=1");

        System.out.println(reader.getDescription());
        System.out.println(reader.getNamesByPattern(""));
        System.out.println(reader.getNamesByPattern("Tnk"));

        String name = "DTL_HPRF:Tnk1:T";
        System.out.println(name);
        Instant start = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-03-09 05:48"));
        Instant end = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-03-10 00:00"));
        ValueIterator values = reader.getRawValues(name, start, end);
        while (values.hasNext())
            System.out.println(values.next());

        name = "EnumPV";
        System.out.println(name);
        start = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-03-05 00:00"));
        end = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2005-03-10 00:00"));
        values = reader.getRawValues(name, start, end);
        while (values.hasNext())
            System.out.println(values.next());

        name = "TextPV";
        System.out.println(name);
        start = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-03-05 00:00"));
        end = Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2005-03-10 00:00"));
        values = reader.getRawValues(name, start, end);
        while (values.hasNext())
            System.out.println(values.next());

    }
}
