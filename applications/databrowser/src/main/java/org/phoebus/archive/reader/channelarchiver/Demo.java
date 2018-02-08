/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.spi.ArchiveReaderFactory;

public class Demo
{
    public static void main(String[] args) throws Exception
    {
        final ArchiveReaderFactory factory = new XMLRPCArchiveReaderFactory();
        final ArchiveReader reader = factory.createReader("http://ics-web4.sns.ornl.gov:8080/RPC2");
    }
}
