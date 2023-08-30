package org.phoebus.channelfinder.utility;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Background job for searching channels from the channelfinder directory
 * service
 * 
 * @author Kunal Shroff, Kay Kasemir
 */
public class ChannelSearchJob extends JobRunnableWithCancel {
    private static final String NAME = "searching Channelfinder for pattern : ";

    private final ChannelFinderClient client;
    private final String pattern;
    private final Consumer<Collection<Channel>> channelHandler;
    private final BiConsumer<String, Exception> errorHandler;

    /**
     * Submit search job
     *
     * @param client          client to use for making REST requests to channelfinder
     * @param pattern         Space seperated search criterias, patterns may include * and ?
     *                        wildcards
     *                        channelNamePattern propertyName=valuePattern1,valuePattern2 Tags=tagNamePattern
     *                        Each criteria is logically ANDed, || seperated values are
     *                        logically ORed Query for channels based on the Query string
     *                        query
     *                        example: find("SR* Cell=1,2 Tags=GolderOrbit,myTag") this
     *                        will return all channels with names starting with SR AND have
     *                        property Cell=1 OR 2 AND have tags goldenOrbit AND myTag. IMP:
     *                        each criteria is logically AND'ed while multiple values for
     *                        Properties are OR'ed.
     * @param channel_handler Invoked when the job located names on the server
     * @param errorHandler   Invoked with URL and Exception when the job failed
     * @return {@link Job}
     */
    public static Job submit(ChannelFinderClient client,
                             final String pattern,
                             final Consumer<Collection<Channel>> channel_handler,
                             final BiConsumer<String, Exception> errorHandler)
    {
        return JobManager.schedule(NAME + pattern,
                new ChannelSearchJob(client, pattern, channel_handler, errorHandler));
    }

    /**
     * private constructor
     * @param client - client to be used for searching
     * @param pattern - search pattern
     * @param channelHandler - handler for matching channels
     * @param errorHandler - error handler
     */
    private ChannelSearchJob(ChannelFinderClient client,
                             String pattern,
                             Consumer<Collection<Channel>> channelHandler,
                             BiConsumer<String, Exception> errorHandler)
    {
        super();
        this.client = client;
        this.pattern = pattern;
        this.channelHandler = channelHandler;
        this.errorHandler = errorHandler;
    }

    @Override
    public String getName()
    {
        return NAME + pattern;
    }

    @Override
    public Runnable getRunnable()
    {
        return () -> {
            Collection<Channel> channels = client.find(pattern);
            channelHandler.accept(channels);
        };
    }

    @Override
    public BiConsumer<String, Exception> getErrorHandler()
    {
        return errorHandler;
    }
}
