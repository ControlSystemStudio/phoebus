/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.info;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/** Memory usage info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MemoryInfo
{
    final private static double MB = 1024*1024;

    final private long used_mem, max_mem, non_heap;

    /** Initialize with memory usage of this JVM */
    public MemoryInfo()
    {
        final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        final MemoryUsage heap = memory.getHeapMemoryUsage();
        used_mem = heap.getUsed();
        max_mem = heap.getMax();
        non_heap = memory.getNonHeapMemoryUsage().getUsed();
    }

    /** Initialize
     *  @param used_mem Used memory (kB)
     *  @param max_mem Maximum available memory (kB)
     *  @param non_heap Used non-heap memory (kB)
     */
    public MemoryInfo(final long used_mem, final long max_mem, final long non_heap)
    {
        this.used_mem = used_mem;
        this.max_mem = max_mem;
        this.non_heap = non_heap;
    }

    /** @return Used memory (kB) */
    public long getUsedMem()
    {
        return used_mem;
    }

    /** @return Maximum memory that server will try to use (kB) */
    public long getMaxMem()
    {
        return max_mem;
    }

    /** @return Memory usage in percent of max: 0..100 */
    public double getMemoryPercentage()
    {
        return used_mem * 100.0 / max_mem;
    }

    /** @return Used non-heap memory (kB) */
    public long getNonHeapUsedMem()
    {
        return non_heap;
    }

    /** @return Memory usage in percent of max */
    public String getMemoryInfo()
    {
        return String.format("Heap: %.1f / %.1f MB (%.1f %%), Non-Heap: %.1f MB",
                used_mem / MB, max_mem / MB, getMemoryPercentage(), non_heap / MB);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "Memory: " + getMemoryInfo();
    }
}
