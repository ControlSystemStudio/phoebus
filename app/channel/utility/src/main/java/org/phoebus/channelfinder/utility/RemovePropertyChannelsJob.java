/**
 *
 */
package org.phoebus.channelfinder.utility;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.Property;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnableWithCancel;

import java.util.Collection;
import java.util.function.BiConsumer;


/**
 * A Job to remove properties from a list of channels
 *
 * @author Kunal Shroff
 */
public class RemovePropertyChannelsJob extends JobRunnableWithCancel {

    private final ChannelFinderClient client;
    private final Property property;
    private final Collection<String> channelNames;
    private final BiConsumer<String, Exception> errorHandler;
    

    /**
     * submit a job to remove a Property from a channel or a group of channels
     *
     * @param client - channelfinder client, which this job be submitted to
     * @param channelNames - collection of channels to which the property is to be removed
     * @param property - the property to be removed
     * @param errorHandler - error handler
     * @return Job
     */
     public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Property property,
                                final BiConsumer<String, Exception> errorHandler)
    {
        return JobManager.schedule("Removing property : " + property.getName() + " to " + channelNames.size() + " channels",
                new RemovePropertyChannelsJob(client, channelNames, property, errorHandler));
    }

    private RemovePropertyChannelsJob(ChannelFinderClient client,
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
        return "Removing property : " + property.getName() + " to " + channelNames.size() + " channels";
    }

    @Override
    public Runnable getRunnable()
    {
        return () -> {
            client.delete(Property.Builder.property(property), channelNames);
        };
    }

    @Override
    public BiConsumer<String, Exception> getErrorHandler()
    {
        return errorHandler;
    }
}
