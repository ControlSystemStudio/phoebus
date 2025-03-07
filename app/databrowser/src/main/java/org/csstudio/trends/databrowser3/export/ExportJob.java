/*******************************************************************************
 * Copyright (c) 2010-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.ArchiveReaders;
import org.phoebus.archive.reader.LinearValueIterator;
import org.phoebus.archive.reader.MergingValueIterator;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeDuration;
import org.phoebus.util.time.TimestampFormats;

/** Base for Eclipse Job for exporting data from Model to file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class ExportJob implements JobRunnable
{
    final protected static int PROGRESS_UPDATE_LINES = 1000;
    final protected String comment;
    final protected Model model;
    final protected Instant start, end;
    final protected Source source;
    final protected double optimize_parameter;
    final protected String filename;
    final protected Consumer<Exception> error_handler;
    /** Active readers, used to cancel and close them */
    final private CopyOnWriteArrayList<ArchiveReader> archive_readers = new CopyOnWriteArrayList<ArchiveReader>();
    final protected boolean unixTimeStamp;

    /** Thread that polls a progress monitor and cancels active archive readers
     *  if the user requests the export job to end via the progress monitor
     */
    class CancellationPoll implements Runnable
    {
        final private JobMonitor monitor;
        volatile boolean exit = false;

        public CancellationPoll(final JobMonitor monitor)
        {
            this.monitor = monitor;
        }

        @Override
        public void run()
        {
            while (! exit)
            {
                if (monitor.isCanceled())
                {
                    for (ArchiveReader reader : archive_readers)
                        reader.cancel();
                }
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    // Ignore
                }
            }
        }
    }

    /** @param comment Comment prefix ('#\t' for most ASCII, '%' for Matlab, ...)
     *  @param model Model with data
     *  @param start Start time
     *  @param end End time
     *  @param source Where to get samples
     *  @param optimize_parameter Used by optimized source
     *  @param filename Name of file to create
     *                  or <code>null</code> if <code>performExport</code>
     *                  handles the file
     *  @param error_handler Callback for errors
     * @param unixTimeStamp If <code>true</code>, time stamps are UNIX style, i.e. ms since EPOCH. Defaults to false.
     */
    public ExportJob(final String comment, final Model model,
        final Instant start, final Instant end, final Source source,
        final double optimize_parameter,
        final String filename,
        final Consumer<Exception> error_handler,
         final boolean unixTimeStamp)
    {
        this.comment = comment;
        this.model = model;
        this.start = start;
        this.end = end;
        this.source = source;
        this.optimize_parameter = optimize_parameter;
        this.filename = filename;
        this.error_handler = error_handler;
        this.unixTimeStamp = unixTimeStamp;
    }

    /** Job's main routine
     *  {@inheritDoc}
     */
    @Override
    public final void run(final JobMonitor monitor)
    {
        monitor.beginTask("Data Export");
        try
        {
            final PrintStream out;
            if (filename != null)
            {
                out = new PrintStream(filename);
                printExportInfo(out);
            }
            else
                out = null;
            // Start thread that checks monitor to cancels readers when
            // user tries to abort the export job
            final CancellationPoll cancel_poll = new CancellationPoll(monitor);
            final Future<?> done = Activator.thread_pool.submit(cancel_poll);
            performExport(monitor, out);
            // ask thread to exit
            cancel_poll.exit = true;
            if (out != null)
                out.close();
            // Wait for poller to quit
            done.get();
        }
        catch (final Exception ex)
        {
            error_handler.accept(ex);
        }
        for (ArchiveReader reader : archive_readers)
            reader.close();
        monitor.done();
    }

    /** Print file header, gets invoked before <code>performExport</code> */
    protected void printExportInfo(final PrintStream out)
    {
        out.println(comment + "Created by CS-Studio Data Browser");
        out.println(comment);
        out.println(comment + "Start Time : " + TimestampFormats.MILLI_FORMAT.format(start));
        out.println(comment + "End Time   : " + TimestampFormats.MILLI_FORMAT.format(end));
        out.println(comment + "Source     : " + source.toString());
        if (source == Source.OPTIMIZED_ARCHIVE)
            out.println(comment + "Desired Value Count: " + optimize_parameter);
        else if (source == Source.LINEAR_INTERPOLATION)
            out.println(comment + "Interpolation Interval: " + SecondsParser.formatSeconds(optimize_parameter));
    }

    /** Perform the data export
     *  @param out PrintStream for output
     *  @throws Exception on error
     */
    abstract protected void performExport(final JobMonitor monitor,
                                          final PrintStream out) throws Exception;

    /** Print info about item
     *  @param out PrintStream for output
     *  @param item ModelItem
     */
    protected void printItemInfo(final PrintStream out, final ModelItem item)
    {
        out.println(comment + "Channel: " + item.getResolvedName());
        // If display name differs from PV, show the _resolved_ version
        if (! item.getName().equals(item.getDisplayName()))
            out.println(comment + "Name   : " + item.getResolvedDisplayName());
        if (item instanceof PVItem)
        {
            final PVItem pv = (PVItem) item;
            out.println(comment + "Archives:");
            int i=1;
            for (ArchiveDataSource archive : pv.getArchiveDataSources())
            {
                out.println(comment + i + ") " + archive.getName());
                out.println(comment + "   URL: " + archive.getUrl());
                ++i;
            }
        }
        out.println(comment);
    }

    /** @param item ModelItem
     *  @return ValueIterator for samples in the item
     *  @throws Exception on error
     */
    protected ValueIterator createValueIterator(final ModelItem item) throws Exception
    {
        if (source == Source.PLOT || !(item instanceof PVItem))
            return new ModelSampleIterator(item, start, end);

        // Start ValueIterator for each sub-archive
        final Collection<ArchiveDataSource> archives = ((PVItem)item).getArchiveDataSources();
        final List<ValueIterator> iters = new ArrayList<>();
        Exception error = null;
        for (ArchiveDataSource archive : archives)
        {
            // Create reader, remember to close it when done
            final ArchiveReader reader = ArchiveReaders.createReader(archive.getUrl());
            archive_readers.add(reader);
            // Create ValueIterator
            try
            {
                ValueIterator iter;
                if (source == Source.OPTIMIZED_ARCHIVE  &&  optimize_parameter > 1)
                    iter = reader.getOptimizedValues(item.getResolvedName(), start, end, (int)optimize_parameter);
                else
                {
                    iter = reader.getRawValues(item.getResolvedName(), start, end);
                    if (source == Source.LINEAR_INTERPOLATION && optimize_parameter >= 1)
                        iter = new LinearValueIterator(iter, TimeDuration.ofSeconds(optimize_parameter));
                }

                iters.add(iter);
            }
            catch (Exception ex)
            {
                Logger.getLogger(getClass().getName()).log(Level.FINE, "Export error for " + item.getResolvedName(), ex);
                if (error == null)
                    error = ex;
            }
        }
        // If none of the iterators work out, report the first error that we found
        if (iters.isEmpty()  &&  error != null)
            throw error;
        // Return a merging iterator
        return new MergingValueIterator(iters.toArray(new ValueIterator[iters.size()]));
    }
}
