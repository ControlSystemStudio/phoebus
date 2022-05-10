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
 * A job for adding a property to channels.
 * @author Kunal Shroff
 */
public class AddProperty2ChannelsJob extends JobRunnableWithCancel {

    private final ChannelFinderClient client;
    private final Property property;
    private final Collection<String> channelNames;
    private final Runnable onSuccess;

    /**
     * Submit a job to add a Property to a channel or a group of channels
     *
     * @param client - channelfinder client, which this job be submitted to
     * @param channelNames - collection of channels to which the property is to be added
     * @param property - the property to be added to the channels
     * @param onSuccess - called on success
     * @return {@link Job}
     */
    public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Property property,
                                final Runnable onSuccess)
    {
        return JobManager.schedule("Adding property : " + property.getName() + " to " + channelNames.size() + " channels",
                new AddProperty2ChannelsJob(client, channelNames, property, onSuccess));
    }

    private AddProperty2ChannelsJob(ChannelFinderClient client,
                                    Collection<String> channels,
                                    Property property,
                                    final Runnable onSuccess)
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
        return "Adding property : " + property.getName() + " to " + channelNames.size() + " channels";
    }

    @Override
    public Runnable getRunnable()
    {
        return () -> {
            try
            {
                client.update(Property.Builder.property(property), channelNames);
            }
            catch (Throwable thrown)
            {
                ChannelErrorHandler.displayError(
                    "Failed to add property '" + property.getName() + "' = '" + property.getValue() + "'",
                    thrown);
                return;
            }
            onSuccess.run();
        };
    }
}
