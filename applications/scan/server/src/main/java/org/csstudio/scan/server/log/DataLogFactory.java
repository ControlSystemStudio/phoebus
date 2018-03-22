/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.csstudio.scan.info.Scan;

/** Factory for scan {@link DataLog}
 *  @author Kay Kasemir
 */
public class DataLogFactory
{
    public static DataLog getDataLog(final Scan scan)
    {
        return new DummyDataLog();
    }

    private static final AtomicLong id = new AtomicLong();

    // TODO Implement data log factory
    public static Scan createDataLog(String name)
    {
        return new Scan(id.incrementAndGet(), name, Instant.now());
    }

    public static List<Scan> getScans()
    {
        return Collections.emptyList();
    }

    public static void deleteDataLog(Scan scan)
    {
        // TODO Implement
    }
}
