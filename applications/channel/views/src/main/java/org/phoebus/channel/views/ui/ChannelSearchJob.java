package org.phoebus.channel.views.ui;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;

/**
 * Background job for searching channels from the channelfinder directory
 * service
 * 
 * @author Kunal Shroff, Kay Kasemir
 */
public class ChannelSearchJob implements JobRunnable {
    private final ChannelFinderClient client;
    private final String pattern;
    private final Consumer<Collection<Channel>> channel_handler;
    private final BiConsumer<String, Exception> error_handler;

    /**
     * Submit search job
     * 
     * @param archives
     *            Archives to search
     * @param pattern
     *            Glob-type name pattern
     * @param channel_handler
     *            Invoked when the job located names on the server
     * @param error_handler
     *            Invoked with URL and Exception when the job failed
     * @return {@link Job}
     */
    public static Job submit(ChannelFinderClient client, final String pattern,
            final Consumer<Collection<Channel>> channel_handler, final BiConsumer<String, Exception> error_handler) {
        return JobManager.schedule("searching Channelfinder for : " + pattern,
                new ChannelSearchJob(client, pattern, channel_handler, error_handler));
    }


    private ChannelSearchJob(ChannelFinderClient client, String pattern, Consumer<Collection<Channel>> channel_handler,
            BiConsumer<String, Exception> error_handler) {
        super();
        this.client = client;
        this.pattern = pattern;
        this.channel_handler = channel_handler;
        this.error_handler = error_handler;
    }


    @Override
    public void run(JobMonitor monitor) throws Exception {
        monitor.beginTask("searching Channelfinder for : " + pattern);
        Collection<Channel> channels = client.find(pattern);
        channel_handler.accept(channels);
    }
}
