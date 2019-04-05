/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
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
package org.csstudio.scan.server.internal;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.scan.info.Scan;
import org.csstudio.scan.server.log.DataLogFactory;
import org.phoebus.framework.jobs.NamedThreadFactory;

/** Engine that accepts {@link ExecutableScan}s, queuing them and executing
 *  them in order
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanEngine
{
    /** All the scans handled by this engine
     *
     *  <p>New, pending scans, i.e. in Idle state, are added to the end.
     *  The currently executing scan is Running or Paused.
     *  Scans that either Finished, Failed or were Aborted
     *  are kept around for a little while.
     *
     *  <p>The list is generally thread-safe (albeit slow when adding elements).
     *  It is only locked to avoid starting a scan that's about to be moved up
     *  for later execution
     *  a) .. when locating the next scan to execute
     *  b) .. when changing the order of scans in the list
     */
    final private List<LoggedScan> scan_queue = new CopyOnWriteArrayList<>();

    /** Executor for scans off the queue, with executeQueuedScans() handling one by one */
    final private ExecutorService queue_executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("ScanEngineQueue"));

    /** Flag for executeQueuedScans() */
    private volatile boolean running = true;

    /** Executor for scans that executes all submitted scans.
     *  Those submitted in parallel are run right away.
     *  Queued scans are submitted by executeQueuedScans()
     */
    final private ExecutorService scan_executor = Executors.newCachedThreadPool(new NamedThreadFactory("ScanEnginePool"));

    /** Start the scan engine, i.e. create thread that will process
     *  scans
     *  @param load_existing_scans Load info about existing scans?
     *  @throws Exception on error
     */
    public void start(final boolean load_existing_scans) throws Exception
    {
        if (! load_existing_scans)
            return;

        final List<Scan> scans = DataLogFactory.getScans();
        for (Scan scan : scans)
            scan_queue.add(new LoggedScan(scan));

        running = true;
        queue_executor.execute(this::executeQueuedScans);
    }

    private void executeQueuedScans()
    {
        while (running)
        {
            ExecutableScan next = null;

            try
            {
                // TODO Semaphore
                Thread.sleep(1000);

                System.out.println("Looking for next scan to execute...");
                next = null;
                synchronized (scan_queue)
                {
                    // Search from most-recently added,
                    // find first scan that's done,
                    // so the one before is next to execute.
                    for (int i = scan_queue.size()-1;  i>=0;  --i)
                    {
                        final LoggedScan scan = scan_queue.get(i);
                        if (scan.getScanState().isDone())
                            break;
                        else if (scan instanceof ExecutableScan)
                            next = (ExecutableScan) scan;
                    }
                }
                if (next != null)
                {
                    System.out.println("Found " + next);
                    final Future<Object> done = next.submit(scan_executor);
                    done.get();
                    System.out.println("Completed " + next);
                }
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, "Scan Queue Handler Error while executing " + next, ex);
            }
        }
        logger.log(Level.WARNING, "Scan Queue Handler exits");
    }

    /** Stop the scan engine, aborting scans
     *  that are still running
     */
    public void stop()
    {
        running = false;
        // TODO Signal semaphore
        
        queue_executor.shutdownNow();
        scan_executor.shutdownNow();
        try
        {
            queue_executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            // Ignore, shutting down anyway
        }
        try
        {
            scan_executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            // Ignore, shutting down anyway
        }

        for (LoggedScan scan : scan_queue)
            closeExecutableScan(scan);
        scan_queue.clear();
    }

    /** 'close' an executable scan
     *  @param scan Scan, potentially an {@link ExecutableScan}
     *  @return <code>true</code> if scan was executable, now closed
     */
    private boolean closeExecutableScan(final LoggedScan scan)
    {
        try
        {
            if (scan instanceof ExecutableScan)
            {
                ((ExecutableScan) scan).close();
                return true;
            }
        }
        catch (Exception e)
        {
            // Ignore, shutting down anyway
        }
        return false;
    }

    /** Submit a scan to the engine for execution
     *  @param scan The {@link ExecutableScan}
     *  @param queue Queue the scan, or execute as soon as possible?
     *  @throws IllegalStateException if scan had been submitted before
     */
    public void submit(final ExecutableScan scan, final boolean queue)
    {
        scan_queue.add(scan);
        if (queue)
        {
            // TODO Wake queue_executor.
        }
        else
            scan.submit(scan_executor);
    }

    /** Check if there are any scans executing or waiting to be executed
     *  @return Number of pending scans
     */
    public boolean hasPendingScans()
    {
        // Ideally, would check from _end_ because pending scans
        // are added to end of queue.
        // To be thread-safe, using plain COWList iteration.
        for (LoggedScan scan : scan_queue)
            if (! scan.getScanState().isDone())
                return true;
        return false;
    }

    /** @return List of scans */
    public List<LoggedScan> getScans()
    {
        final List<LoggedScan> scans = new ArrayList<>();
        scans.addAll(scan_queue);
        return scans;
    }

    /** @return List of executable scans */
    public List<ExecutableScan> getExecutableScans()
    {
        final List<ExecutableScan> scans = new ArrayList<>();
        for (LoggedScan scan : scan_queue)
            if (scan instanceof ExecutableScan)
                scans.add((ExecutableScan) scan);
        return scans;
    }

    /** Find scan by ID
     *  @param id Scan ID
     *  @return {@link ExecutableScan}
     *  @throws UnknownScanException if scan ID not valid
     */
    public LoggedScan getScan(final long id) throws UnknownScanException
    {
        // Linear lookup. Good enough?
        for (LoggedScan scan : scan_queue)
            if (scan.getId() == id)
                return scan;
        throw new UnknownScanException(id);
    }

    /** Find executable scan by ID
     *  @param id Scan ID
     *  @return {@link ExecutableScan} or <code>null</code> if scan is not executable
     *  @throws UnknownScanException if scan ID not valid
     */
    public ExecutableScan getExecutableScan(final long id) throws UnknownScanException
    {
        final LoggedScan scan = getScan(id);
        if (scan instanceof ExecutableScan)
            return (ExecutableScan) scan;
        return null;
    }

    /** @param scan Scan to remove (if it's 'done')
     *  @throws Exception on error
     */
    public void removeScan(final LoggedScan scan) throws Exception
    {
        // Only remove scans that are 'done'
        if (scan.getScanState().isDone())
        {
            DataLogFactory.deleteDataLog(scan);
            scan_queue.remove(scan);
            closeExecutableScan(scan);
        }
    }

    /** Remove completed scans
     *  @throws Exception on error
     */
    public void removeCompletedScans() throws Exception
    {
        for (LoggedScan scan : scan_queue)
            removeScan(scan);
    }

    /** Remove the oldest completed scan
     *  @return LoggedScan that was removed or <code>null</code>
     */
    public Scan removeOldestCompletedScan()
    {
        for (LoggedScan scan : scan_queue)
            if (scan.getScanState().isDone())
            {
                scan_queue.remove(scan);
                closeExecutableScan(scan);
                return scan;
            }
        return null;
    }

    /** Turn oldest completed in-memory scan into logged scan
     *  @return Logged that was turned into logged scan or <code>null</code>
     */
    public Scan logOldestCompletedScan()
    {
        for (LoggedScan scan : scan_queue)
            if (scan.getScanState().isDone()  &&  closeExecutableScan(scan))
            {
                final LoggedScan logged = new LoggedScan(scan);
                final int index = scan_queue.indexOf(scan);
                scan_queue.set(index, logged);
                return logged;
            }
        return null;
    }
}
