/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.archive;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.DataBrowserInstance;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.model.RequestType;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.ArchiveReaders;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.util.time.TimestampFormats;

/** JobRunnable for fetching archived data.
 *
 *  <p>Actually spawns another thread so that the 'main' job can
 *  poll the progress monitor for cancellation and ask the secondary
 *  thread to cancel.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArchiveFetchJob implements JobRunnable
{
    /** Poll period in millisecs */
    private static final int POLL_PERIOD_MS = 1000;

    /** Limit the number of concurrently running jobs */
    private static final Semaphore concurrent_requests = new Semaphore(Preferences.concurrent_requests, true);

    /** Item for which to fetch samples */
    private final PVItem item;

    /** Start/End time */
    private final Instant start, end;

    /** Listener that's notified when (if) we completed OK */
    private final ArchiveFetchJobListener listener;

    private Job job;

    /** Thread that performs the actual background work.
     *
     *  Instead of directly accessing the archive, ArchiveFetchJob launches
     *  a WorkerThread for the actual archive access, so that the Job
     *  can then poll the progress monitor for cancellation and if
     *  necessary cancel the archive reader when it is 'stuck'
     *  in a long running operation.
     */
    class WorkerThread implements Runnable
    {
        private volatile String message = "Queued";
        private volatile boolean cancelled = false;

        /** Archive reader that's currently queried */
        private volatile ArchiveReader reader;

        /** @return Message that somehow indicates progress */
        public String getMessage()
        {
            return message;
        }

        /** Request thread to cancel its operation */
        public void cancel()
        {
            cancelled = true;
            final ArchiveReader r = reader;
            if (r != null)
                r.cancel();
        }

        /** {@inheritDoc} */
        @Override
        public void run()
        {
            logger.log(Level.FINE, "Starting {0}", ArchiveFetchJob.this);
            final long start_time = System.currentTimeMillis();
            long samples = 0;

            // Number of bins. Negative values are scaling factor for display width
            int bins = Preferences.plot_bins;
            if (bins < 0)
                bins = DataBrowserInstance.display_pixel_width * (-bins);
            // Bins could be 0 when display_pixel_width has not been initialed
            // (no DB instance had been opened)
            if (bins <= 0)
                bins = 800;

            final Collection<ArchiveDataSource> archives = item.getArchiveDataSources();
            final List<ArchiveDataSource> archives_without_channel = new ArrayList<>();
            final int bins_final = bins;
            int i = 0;
            for (ArchiveDataSource archive : archives)
            {
                if (cancelled)
                    break;
                // Display "N/total", using '1' for the first sub-archive.
                message = MessageFormat.format(Messages.ArchiveFetchDetailFmt,
                                               archive.getName(), ++i, archives.size());

                try
                {
                    final List<VType> fetched = fetchFromSource(archive, bins_final);
                    if (!cancelled)
                    {
                        samples += fetched.size();
                        item.mergeArchivedSamples(archive.getName(), fetched);
                    }
                }
                catch (UnknownChannelException ex)
                {
                    // Do not immediately notify about unknown channels. First search for the data in all archive
                    // sources and only report this kind of errors at the end
                    archives_without_channel.add(archive);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, ex,
                            () -> "Archive fetch failed for source: " + archive.getName());
                    if (!cancelled)
                        listener.archiveFetchFailed(ArchiveFetchJob.this, archive, ex);
                }
            }
            final long end_time = System.currentTimeMillis();
            logger.log(Level.FINE,
                    "Ended {0} with {1} samples in {2} secs",
                    new Object[] { ArchiveFetchJob.this, samples, (end_time - start_time)/1000 });

            if (cancelled)
                return;

            if (archives_without_channel.size() > 0)
                listener.channelNotFound(ArchiveFetchJob.this,
                        archives_without_channel.size() < archives.size(),
                        archives_without_channel);

            listener.fetchCompleted(ArchiveFetchJob.this);
        }

        /** Fetch all samples from one archive source.
         *  Runs directly on WorkerThread, timed by the outer polling loop.
         *  @return list of samples
         *  @throws Exception on fetch error
         */
        List<VType> fetchFromSource(final ArchiveDataSource archive, final int bins) throws Exception
        {
            try (final ArchiveReader the_reader = openReader(archive.getUrl()))
            {
                reader = the_reader;
                try
                (
                    final ValueIterator value_iter = (item.getRequestType() == RequestType.RAW)
                            ? the_reader.getRawValues(item.getResolvedName(), start, end)
                            : the_reader.getOptimizedValues(item.getResolvedName(), start, end, bins)
                )
                {
                    final List<VType> result = new ArrayList<>();
                    while (value_iter.hasNext())
                        result.add(value_iter.next());
                    return result;
                }
                finally
                {
                    reader = null;
                }
            }
        }

        @Override
        public String toString()
        {
            return "WorkerTread for " + ArchiveFetchJob.this.toString();
        }
    }

    /** Schedule a new job.
     *
     *  @param item the item for which the data are fetched
     *  @param start the lower time boundary for the historic data
     *  @param end the upper time boundary for the history data
     *  @param listener the listener notified when the job is complete or an error happens
     */
    public ArchiveFetchJob(final PVItem item, final Instant start,
                           final Instant end,
                           final ArchiveFetchJobListener listener)
    {
        this.item = item;
        this.start = start;
        this.end = end;
        this.listener = listener;
        this.job = JobManager.schedule(toString(), this);
    }

    /** Test-only constructor: does not schedule via JobManager. */
    ArchiveFetchJob(final PVItem item, final Instant start, final Instant end,
                    final ArchiveFetchJobListener listener, final boolean testOnly)
    {
        this.item = item;
        this.start = start;
        this.end = end;
        this.listener = listener;
        this.job = null;
    }

    /** Create an {@link ArchiveReader} for the given URL.
     *  Override in tests to inject fakes.
     */
    protected ArchiveReader openReader(final String url) throws Exception
    {
        return ArchiveReaders.createReader(url);
    }

    /** @return PVItem for which this job was created */
    public PVItem getPVItem()
    {
        return item;
    }

    /** Job's main routine which starts and monitors WorkerThread */
    @Override
    public void run(JobMonitor monitor) throws Exception
    {
        if (item == null)
            return;

        // When creating a configuration with macros,
        // data can only be fetched once the macros resolve
        if (MacroHandler.containsMacros(item.getResolvedName()))
            return;

        monitor.beginTask("Pending...");

        // When a model is loaded, items added,
        // or user zooms/pans, this can result in jobs being added
        // and then soon canceled to add updated jobs.
        // Wait a little to then check if we're already cancelled,
        // instead of starting the request right away only to then
        // have a hard time cancelling the ongoing query.
        // (For zoom/pan, this delay is actually used twice:
        //  before starting the fetch job, then right here)
        TimeUnit.MILLISECONDS.sleep(Preferences.archive_fetch_delay);

        // Cancelled before even started the worker?
        if (monitor.isCanceled())
            return;

        concurrent_requests.acquire();
        try
        {
            monitor.beginTask(Messages.ArchiveFetchStart);

            final WorkerThread worker = new WorkerThread();
            final Future<?> done = Activator.thread_pool.submit(worker);
            // Poll worker and progress monitor
            long start = System.currentTimeMillis();
            String lastSourceMessage = "";
            long sourceStartTime = System.currentTimeMillis();
            while (!done.isDone())
            {   // Wait until worker is done, or time out to update info message
                try
                {
                    done.get(POLL_PERIOD_MS, TimeUnit.MILLISECONDS);
                }
                catch (Exception ex)
                {
                    // Ignore
                }
                final String currentMessage = worker.getMessage();
                if (!currentMessage.equals(lastSourceMessage))
                {
                    lastSourceMessage = currentMessage;
                    sourceStartTime = System.currentTimeMillis();
                }
                final long seconds = (System.currentTimeMillis() - start) / 1000;
                final String info = MessageFormat.format(Messages.ArchiveFetchProgressFmt,
                                                         currentMessage, seconds);
                monitor.updateTaskName(info);
                if (monitor.isCanceled()
                        || (Preferences.archive_read_timeout_ms > 0
                            && System.currentTimeMillis() - sourceStartTime > Preferences.archive_read_timeout_ms))
                    worker.cancel();
            }
        }
        finally
        {
            concurrent_requests.release();
        }
    }

    /** Programmatically cancel job
     *
     *  <p>.. because a new job for the same item
     *  replaces existing one.
     *  In addition, job may be cancelled by user
     *  from job UI.
     */
    public void cancel()
    {
        job.cancel();
    }

    /** @return Debug string */
    @Override
    public String toString()
    {
        return MessageFormat.format(Messages.ArchiveFetchJobFmt,
            item.getResolvedDisplayName(),
            TimestampFormats.FULL_FORMAT.format(start),
            TimestampFormats.FULL_FORMAT.format(end));
    }
}
