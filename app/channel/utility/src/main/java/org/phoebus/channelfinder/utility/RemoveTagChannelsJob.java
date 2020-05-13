package org.phoebus.channelfinder.utility;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.Tag;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;

public class RemoveTagChannelsJob implements JobRunnable {

    private final ChannelFinderClient client;
    private final Tag tag;
    private final Collection<String> channelNames;
    private final BiConsumer<String, Exception> error_handler;
    

    /**
     * submit a job to remove a Tag from a channel or a group of channels
     *
     * @param client - channelfinder client, which this job be submitted to
     * @param channelNames - collection of channels to which the property is to be removed
     * @param tag - the tag to be removed
     * @param error_handler 
     */
    public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Tag tag,
                                final BiConsumer<String, Exception> error_handler) {
        return JobManager.schedule("Removing tag : " + tag.getName() + " from " + channelNames.size() + " channels",
                new RemoveTagChannelsJob(client, channelNames, tag, error_handler));
    }

    private RemoveTagChannelsJob(ChannelFinderClient client, Collection<String> channels, Tag tag, BiConsumer<String, Exception> error_handler) {
        super();
        this.client = client;
        this.error_handler = error_handler;
        this.channelNames = channels;
        this.tag = tag;
    }

    @Override
    public void run(JobMonitor monitor) throws Exception {
        monitor.beginTask("Removing tag : " + tag.getName() + " from " + channelNames.size() + " channels");
        client.delete(Tag.Builder.tag(tag), channelNames);
        //schannel_handler.accept(channels);
    }

}
