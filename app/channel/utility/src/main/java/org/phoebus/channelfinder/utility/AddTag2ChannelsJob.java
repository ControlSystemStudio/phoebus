package org.phoebus.channelfinder.utility;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.Tag;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;

import java.util.Collection;
import java.util.function.BiConsumer;

/**
 *
 * @author Kunal Shroff
 */
public class AddTag2ChannelsJob extends JobRunnableWithCancel {

    private final ChannelFinderClient client;
    private final Tag tag;
    private final Collection<String> channelNames;
    private final BiConsumer<String, Exception> errorHandler;

    /**
     * submit a job to add a tag to a channel or a group of channels
     *
     * @param client - channelfinder client, which this job be submitted to
     * @param channelNames - collection of channels to which the tag is to be added
     * @param tag - the tag to be added
     * @param errorHandler - error handler
     * @return Job
     */
    public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Tag tag,
                                final BiConsumer<String, Exception> errorHandler)
    {
        return JobManager.schedule("Adding tag : " + tag.getName() + " to " + channelNames.size() + " channels",
                new AddTag2ChannelsJob(client, channelNames, tag, errorHandler));
    }

    private AddTag2ChannelsJob(ChannelFinderClient client, Collection<String> channels, Tag tag, BiConsumer<String, Exception> errorHandler)
    {
        super();
        this.client = client;
        this.errorHandler = errorHandler;
        this.channelNames = channels;
        this.tag = tag;
    }

    @Override
    public String getName()
    {
        return "Adding tag : " + tag.getName() + " to " + channelNames.size() + " channels";
    }

    @Override
    public Runnable getRunnable()
    {
        return () -> {
            client.update(Tag.Builder.tag(tag), channelNames);
        };
    }

    @Override
    public BiConsumer<String, Exception> getErrorHandler()
    {
        return errorHandler;
    }
}
