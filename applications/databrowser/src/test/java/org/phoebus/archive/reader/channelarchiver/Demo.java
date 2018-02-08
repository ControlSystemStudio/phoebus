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
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;
import org.phoebus.util.time.TimestampFormats;

public class Demo
{
    public static void main(String[] args) throws Exception
    {
        final ArchiveReaderFactory factory = new XMLRPCArchiveReaderFactory();
        final ArchiveReader reader = factory.createReader("xnds://ics-web4.sns.ornl.gov:8080/RPC2?key=1");

        System.out.println(reader.getDescription());
        System.out.println(reader.getNamesByPattern(""));
        System.out.println(reader.getNamesByPattern("Tnk"));


        reader.getRawValues("DTL_HPRF:Tnk1:T",
                            Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-03-09 05:48")),
                            Instant.from(TimestampFormats.DATETIME_FORMAT.parse("2004-04-01 12:03")));
    }
}
