/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.scanner;

import static org.csstudio.archive.Engine.logger;

import java.util.ArrayList;
import java.util.logging.Level;

import org.csstudio.archive.engine.model.Average;

/** Helper for scanning something.
 *  <p>
 *  Places runnable items on scan lists,
 *  determines how long to wait to the scan next.
 *  @author Kay Kasemir
 */
public class Scanner implements Scheduleable
{
    /** Granularity in seconds for the supported lists. */
    final private static double GRANULARITY = 0.1;

    /** Default idle delay */
    final private static long DEFAULT_IDLE_DELAY = 500;

    /** Time to wait in system millis when nothing to do */
    final private long idle_delay;

    /** All the single-period scan lists.
     *  <p>
     *  Note that we don't keep empty lists around,
     *  so every list should have a valid 'due time'.
     */
    final private ArrayList<ScanList> lists = new ArrayList<>();

    /** Next due time in system millis */
    private long next_due_time;

    /** Average idle time in percent. */
    final private Average idle_percentage = new Average();

    /** Construct scanner
     *  @param idle_delay Time to wait in system millis when nothing to do
     */
    public Scanner(final long idle_delay)
    {
        this.idle_delay = idle_delay;
    }

    /** Construct scanner with default idle delay */
    public Scanner()
    {
        this(DEFAULT_IDLE_DELAY);
    }

    /** Add an item to the scanner, placing it on a suitable scan list.
     *  @param item Item to scan
     *  @param period Scan period in seconds
     */
    public void add(final Runnable item, final double period)
    {
        // Avoid duplicates by removing what might be there
        remove(item);

        // Locate suitable scan list
        ScanList the_list = null;
        for (ScanList list : lists)
        {
            if (Math.abs(list.getPeriod() - period) < GRANULARITY)
            {
                the_list = list;
                break;
            }
        }
        // Nothing found?
        if (the_list == null)
        {   // Create new scan list
            the_list = new ScanList(Math.round(period / GRANULARITY) * GRANULARITY);
            lists.add(the_list);
        }
        // Add item to list
        the_list.add(item);
        computeDueTime();
    }

    /** Remove an item from the scanner */
    public void remove(final Runnable item)
    {
        // Brute-force remove from all lists,
        // stopping as soon as item was found
        for (ScanList list : lists)
            if (list.remove(item))
            {
                // If this leaves an empty list, remove it.
                if (! list.isDueAtAll())
                    lists.remove(list);
                return;
            }
    }

    /** Remove all items from this scanner */
    public void clear()
    {
        lists.clear();
    }

    /** @return Number of scan lists. */
    public long size()
    {
        return lists.size();
    }

    /** @return One of the scan lists. */
    public ScanList get(final int index)
    {
        return lists.get(index);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDueAtAll()
    {
        return lists.size() > 0;
    }

    /** {@inheritDoc} */
    @Override
    public long getNextDueTime()
    {
        if (lists.size() == 0)
            throw new Error("Scanner never due"); //$NON-NLS-1$
        return next_due_time;
    }

    /** Scan all the scan lists which are currently due */
    void scanDueScanLists()
    {
        final long now = System.currentTimeMillis();
        next_due_time = Long.MAX_VALUE;
        for (ScanList list : lists)
        {   // Scan (run) what's due now, and update the next due time
            if (list.getNextDueTime() <= now)
                list.scanItems();
            // Update the next due time - which could change in run()
            if (list.getNextDueTime() < next_due_time)
                next_due_time = list.getNextDueTime();
        }
    }

    /** Compute the next due time */
    private void computeDueTime()
    {
        next_due_time = Long.MAX_VALUE;
        for (ScanList list : lists)
            next_due_time = Math.min(list.getNextDueTime(), next_due_time);
    }

    /** Average idle time in percent.
     *  <p>
     *  100 means: Nothing to do, always waiting.<br>
     *   50 means: using about half the allocated time for the scan<br>
     *    0 means: Busy all the time, actually missing scans<br>
     */
    public double getIdlePercentage()
    {
        return idle_percentage.get();
    }

    /** Reset statistics */
    public void reset()
    {
        idle_percentage.reset();
    }

    /** Perform one scan: Wait for the next scan, perform it. */
    public void scanOnce()
    {
        try
        {
            if (isDueAtAll())
            {
                final long delay = getNextDueTime() - System.currentTimeMillis();
                if (delay > idle_delay)
                {   // Nothing due within idle time
                    idle_percentage.update(100.0);
                    Thread.sleep(idle_delay);
                    return;
                }
                if (delay > 0)
                {   // Need to wait a little, then run scan
                    idle_percentage.update(delay*100.0/idle_delay);
                    Thread.sleep(delay);
                    scanDueScanLists();
                    return;
                }
                // High noon
                idle_percentage.update(0.0);
                scanDueScanLists();
                return;
            }
            // Nothing to do, ever. Just wait.
            idle_percentage.update(100.0);
            Thread.sleep(idle_delay);
        }
        catch (InterruptedException ex)
        {
            logger.log(Level.WARNING, "Scanner interrupted", ex); //$NON-NLS-1$
        }
    }
}
