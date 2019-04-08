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
import org.csstudio.scan.server.internal.ExecutableScan.QueueState;
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

    /** Executor for executeQueuedScans() */
    final private ExecutorService queue_executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("QueueHandler"));

    /** Executor queued scans that are submitted by executeQueuedScans() */
    final private ExecutorService queued_scan_executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("QueuedScans"));

    /** Flag for executeQueuedScans() */
    private volatile boolean running = true;

    /** Executor for parallel scans. */
    final private ExecutorService parallel_executor = Executors.newCachedThreadPool(new NamedThreadFactory("ParallelScans"));

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

    /** Wake executeQueuedScans() because a new scan was added */
    private void signalNewScan()
    {
        synchronized (scan_queue)
        {
            scan_queue.notifyAll();
        }
    }

    /** Monitor the scan queue, submit one queued scan at a time */
    private void executeQueuedScans()
    {
        // This thread is not directly executing the queued scans because
        // by submitting them to an executor we get a Future that's used
        // to cancel it.
        // Scans are submitted to the single-threaded queued_scan_executor
        // and not the parallel_executor to assert that only one queued scan
        // runs at a time.
        while (running)
        {
            ExecutableScan next = null;
            try
            {
                logger.log(Level.FINE, "Looking for next scan to execute...");
                synchronized (scan_queue)
                {
                    // Search from most-recently added end of queue
                    for (int i = scan_queue.size()-1;  i>=0;  --i)
                    {
                        final LoggedScan scan = scan_queue.get(i);
                        // Track the last Queued scan,
                        // which should be the next to execute
                        if (scan instanceof ExecutableScan)
                        {
                            final ExecutableScan exe = (ExecutableScan) scan;
                            if (exe.getQueueState() == QueueState.Queued)
                                next = exe;
                        }
                        else // Stop looking when reaching logged scans
                            break;
                    }
                }
                if (next == null)
                {
                    logger.log(Level.FINE, "Waiting for new scan");
                    // Wait for a change in the scan queue
                    synchronized (scan_queue)
                    {
                        scan_queue.wait();
                    }
                }
                else
                {
                    logger.log(Level.FINE, "Found " + next);
                    next.setQueueState(QueueState.Submitted);
                    final Future<Object> done = next.submit(queued_scan_executor);
                    done.get();
                    logger.log(Level.FINE, "Completed " + next);
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
        signalNewScan();
        queue_executor.shutdownNow();
        queued_scan_executor.shutdownNow();
        parallel_executor.shutdownNow();
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
            queued_scan_executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            // Ignore, shutting down anyway
        }
        try
        {
            parallel_executor.awaitTermination(10, TimeUnit.SECONDS);
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
            // Wake queue_executor.
            scan.setQueueState(QueueState.Queued);
            signalNewScan();
        }
        else // Execute right away
            scan.submit(parallel_executor);
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

    private QueueState getQueueState(final LoggedScan scan)
    {
        if (scan instanceof ExecutableScan)
            return ((ExecutableScan) scan).getQueueState();
        return QueueState.NotQueued;
    }

    /** Ask server to move idle scan in list
     *
     *  <p>Has no effect if the scan is not idle or target slot in scan list cannot be used.
     *
     *  @param id ID that uniquely identifies a scan
     *  @param steps How far to move the scan 'up' (earlier) for positive steps, otherwise 'down'.
     *  @throws Exception on error
     */
    public void move(final long id, final int steps) throws Exception
    {
        logger.log(Level.INFO, "Move scan " + id + " by " + steps);
        synchronized (scan_queue)
        {
            final int N = scan_queue.size();
            for (int i=N-1; i>=0; --i)
            {   // Locate scan by ID
                final LoggedScan scan = scan_queue.get(i);
                if (scan.getId() != id)
                    continue;

                final int target = i + steps;
                if (target >= N)
                {
                    logger.log(Level.WARNING, "Cannot move " + scan + " beyond top of queue");
                    return;
                }
                // Is it a queued (i.e. idle) scan? Cannot move parallel or running queued scans.
                if (getQueueState(scan) != QueueState.Queued)
                {
                    logger.log(Level.WARNING, "Can only move idle, queued scans, not " + scan);
                    return;
                }

                // Is there any 'running' scan below the target location?
                // Cannot move below because then it would never run.
                boolean above_running = false;
                for (int r=target-1; r>=0; --r)
                {
                    final LoggedScan scn = scan_queue.get(r);
                    if (getQueueState(scn) == QueueState.Submitted &&
                        scn.getScanState().isActive())
                    {
                        above_running = true;
                        break;
                    }
                }
                if (!above_running)
                {
                    logger.log(Level.WARNING, "Scan must stay above the running queued scan");
                    return;
                }

                // Move
                scan_queue.remove(i);
                scan_queue.add(target, scan);
                return;
            }
        }
        logger.log(Level.WARNING, "Unknown scan " + id);
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
