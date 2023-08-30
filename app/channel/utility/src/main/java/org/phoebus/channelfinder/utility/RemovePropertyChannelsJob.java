/**
 *
 */
package org.phoebus.channelfinder.utility;

import java.util.Collection;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.Property;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;


/**
 * A Job to remove properties from a list of channels
 *
 * @author Kunal Shroff
 */
public class RemovePropertyChannelsJob extends JobRunnableWithCancel {

    private final ChannelFinderClient client;
    private final Property property;
    private final Collection<String> channelNames;
    private final Runnable onSuccess;

    /**
     * submit a job to remove a Property from a channel or a group of channels
     *
     * @param client - channelfinder client, which this job be submitted to
     * @param channelNames - collection of channels to which the property is to be removed
     * @param property - the property to be removed
     * @param onSuccess - called on success
     * @return Job
     */
     public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Property property,
                                final Runnable onSuccess)
    {
        return JobManager.schedule("Removing property : " + property.getName() + " to " + channelNames.size() + " channels",
                new RemovePropertyChannelsJob(client, channelNames, property, onSuccess));
    }

    private RemovePropertyChannelsJob(ChannelFinderClient client,
                                      Collection<String> channels,
                                      Property property,
                                      Runnable onSuccess)
    {
        super();
        this.client = client;
        this.onSuccess = onSuccess;
        this.channelNames = channels;
        this.property = property;
    }

    @Override
    public String getName()
    {
        return "Removing property : " + property.getName() + " from " + channelNames.size() + " channels";
    }

    @Override
    public Runnable getRunnable()
    {
        return () -> {
            try
            {
                client.delete(Property.Builder.property(property), channelNames);
            }
            catch (Throwable thrown)
            {
                ChannelErrorHandler.displayError(
                    "Failed to remove property '" + property.getName() + "'",
                    thrown);
                return;
            }
            onSuccess.run();
        };
    }
}
