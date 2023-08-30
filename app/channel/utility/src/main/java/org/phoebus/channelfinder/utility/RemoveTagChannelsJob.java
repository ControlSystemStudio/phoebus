package org.phoebus.channelfinder.utility;

import java.util.Collection;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.Tag;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;

/**
 * A job to remove tag from a list of channels
 *
 * @author Kunal Shroff
 */
public class RemoveTagChannelsJob extends JobRunnableWithCancel {

    private final ChannelFinderClient client;
    private final Tag tag;
    private final Collection<String> channelNames;
    private final Runnable onSuccess;

    /**
     * submit a job to remove a Tag from a channel or a group of channels
     *
     * @param client - channelfinder client, which this job be submitted to
     * @param channelNames - collection of channels to which the property is to be removed
     * @param tag - the tag to be removed
     * @param onSuccess  - called on success
     * @return Job
     */
    public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Tag tag,
                                final Runnable onSuccess)
    {
        return JobManager.schedule("Removing tag : " + tag.getName() + " to " + channelNames.size() + " channels",
                new RemoveTagChannelsJob(client, channelNames, tag, onSuccess));
    }

    private RemoveTagChannelsJob(ChannelFinderClient client, Collection<String> channels, Tag tag, Runnable onSuccess)
    {
        super();
        this.client = client;
        this.onSuccess = onSuccess;
        this.channelNames = channels;
        this.tag = tag;
    }

    @Override
    public String getName()
    {
        return "Removing tag : " + tag.getName() + " from " + channelNames.size() + " channels";
    }

    @Override
    public Runnable getRunnable()
    {
        return () -> {
            try
            {
                client.delete(Tag.Builder.tag(tag), channelNames);
            }
            catch (Throwable thrown)
            {
                ChannelErrorHandler.displayError(
                    "Failed to remove tag '" + tag.getName() + "'",
                    thrown);
                return;
            }
            onSuccess.run();
        };
    }
}
