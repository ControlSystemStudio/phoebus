/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.archive;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.ChannelInfo;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.ArchiveReaders;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;

/** Background job for searching names on archive data server
 *  @author Kay Kasemir
 */
public class SearchJob implements JobRunnable
{
    private final List<ArchiveDataSource> archives;
    private final String pattern;
    private final Consumer<List<ChannelInfo>> channel_handler;
    private final BiConsumer<String, Exception> error_handler;

    /** Submit search job
     *  @param archives Archives to search
     *  @param pattern Glob-type name pattern
     *  @param channel_handler Invoked when the job located names on the server
     *  @param error_handler Invoked with URL and Exception when the job failed
     *  @return {@link Job}
     */
    public static Job submit(List<ArchiveDataSource> archives, final String pattern,
                             final Consumer<List<ChannelInfo>> channel_handler,
                             final BiConsumer<String, Exception> error_handler)
    {
        return JobManager.schedule(MessageFormat.format(Messages.SearchChannelFmt, pattern),
                                   new SearchJob(archives, pattern, channel_handler, error_handler));
    }

    private SearchJob(final List<ArchiveDataSource> archives, final String pattern,
                      final Consumer<List<ChannelInfo>> channel_handler,
                      final BiConsumer<String, Exception> error_handler)
    {
        this.archives = archives;
        this.pattern = pattern;
        this.channel_handler = channel_handler;
        this.error_handler = error_handler;
    }

    @Override
    public void run(final JobMonitor monitor) throws Exception
    {
        final List<ChannelInfo> channels = new ArrayList<>();
        monitor.beginTask(Messages.Search, archives.size());
        for (ArchiveDataSource archive : archives)
        {
            try
            (
                final ArchiveReader reader = ArchiveReaders.createReader(archive.getUrl());
            )
            {
                if (monitor.isCanceled())
                    break;
                monitor.updateTaskName(archive.getName());
                reader.getNamesByPattern(pattern)
                      .stream()
                      .map(name -> new ChannelInfo(name, archive))
                      .forEach(info -> channels.add(info));
                monitor.worked(1);
            }
            catch (final Exception ex)
            {
                error_handler.accept(archive.getUrl(), ex);
                return;
            }
        }
        channel_handler.accept(channels);
    }
}
