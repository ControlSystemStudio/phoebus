/**
 *
 */
package org.phoebus.channelfinder.utility;

import java.util.Collection;
import java.util.function.BiConsumer;

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
    private final BiConsumer<String, Exception> errorHandler;

    /**
     * Submit a job to add a Property to a channel or a group of channels
     *
     * @param client - channelfinder client, which this job be submitted to
     * @param channelNames - collection of channels to which the property is to be added
     * @param property - the property to be added to the channels
     * @param errorHandler - error handler
     * @return {@link Job}
     */
    public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Property property,
                                final BiConsumer<String, Exception> errorHandler)
    {
        return JobManager.schedule("Adding property : " + property.getName() + " to " + channelNames.size() + " channels",
                new AddProperty2ChannelsJob(client, channelNames, property, errorHandler));
    }

    private AddProperty2ChannelsJob(ChannelFinderClient client,
                                    Collection<String> channels,
                                    Property property,
                                    BiConsumer<String, Exception> errorHandler)
    {
        super();
        this.client = client;
        this.errorHandler = errorHandler;
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
            client.update(Property.Builder.property(property), channelNames);
        };
    }

    @Override
    public BiConsumer<String, Exception> getErrorHandler()
    {
        return errorHandler;
    }
}
