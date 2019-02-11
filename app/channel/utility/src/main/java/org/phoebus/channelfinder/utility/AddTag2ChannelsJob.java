package org.phoebus.channelfinder.utility;

import java.util.Collection;
import java.util.function.BiConsumer;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.Tag;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;

public class AddTag2ChannelsJob implements JobRunnable {

    private final ChannelFinderClient client;
    private final Tag tag;
    private final Collection<String> channelNames;
    private final BiConsumer<String, Exception> error_handler;
    

    /**
     * submit a job to add a tag _tag_ to a group of channels
     *
     * @param name - job name
     * @param channels - collection of channels to which the tag is to be added
     * @param tag - builder of the the tag to be added
     */
    public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Tag tag,
                                final BiConsumer<String, Exception> error_handler) {
        return JobManager.schedule("Adding tag : " + tag.getName() + " to " + channelNames.size() + " channels",
                new AddTag2ChannelsJob(client, channelNames, tag, error_handler));
    }

    private AddTag2ChannelsJob(ChannelFinderClient client, Collection<String> channels, Tag tag, BiConsumer<String, Exception> error_handler) {
        super();
        this.client = client;
        this.error_handler = error_handler;
        this.channelNames = channels;
        this.tag = tag;
    }

    @Override
    public void run(JobMonitor monitor) throws Exception {
        monitor.beginTask("Adding tag : " + tag.getName() + " to " + channelNames.size() + " channels");
        client.update(Tag.Builder.tag(tag), channelNames);
    }

}
