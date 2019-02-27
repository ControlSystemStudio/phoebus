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
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;


/**
 * @author Kunal Shroff
 *
 */
public class RemovePropertyChannelsJob implements JobRunnable {

    private final ChannelFinderClient client;
    private final Property property;
    private final Collection<String> channelNames;
    private final BiConsumer<String, Exception> error_handler;
    

    /**
     * submit a job to add a Property to a channel or a group of channels
     *
     * @param name - job name
     * @param channelNames - collection of channels to which the tag is to be added
     * @param tag - builder of the the tag to be added
     */
    public static Job submit(ChannelFinderClient client,
                                final Collection<String> channelNames,
                                final Property property,
                                final BiConsumer<String, Exception> error_handler) {
        return JobManager.schedule("Adding tag : " + property.getName() + " to " + channelNames.size() + " channels",
                new RemovePropertyChannelsJob(client, channelNames, property, error_handler));
    }

    private RemovePropertyChannelsJob(ChannelFinderClient client, Collection<String> channels, Property property, BiConsumer<String, Exception> error_handler) {
        super();
        this.client = client;
        this.error_handler = error_handler;
        this.channelNames = channels;
        this.property = property;
    }

    @Override
    public void run(JobMonitor monitor) throws Exception {
        monitor.beginTask("Removing property : " + property.getName() + " to " + channelNames.size() + " channels");
        client.delete(Property.Builder.property(property), channelNames);
        //schannel_handler.accept(channels);
    }

}
