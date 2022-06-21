package org.phoebus.channelfinder.utility;

import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;

import java.util.function.BiConsumer;

/**
 *
 */
public class ConnectionCheckJob extends JobRunnableWithCancel {

    private static final String NAME = "checking connection to Channelfinder : ";

    private final ChannelFinderClient client;
    private final BiConsumer<String, Exception> errorHandler;

    private ConnectionCheckJob(ChannelFinderClient client, BiConsumer<String, Exception> errorHandler) {
        this.client = client;
        this.errorHandler = errorHandler;
    }

    public static Job submit(ChannelFinderClient client, BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule(NAME ,
                new ConnectionCheckJob(client, errorHandler));
    }

    @Override
    public String getName() {
        return NAME;
    }



    @Override
    public Runnable getRunnable() {
        return () -> {
            try {
                client.getAllTagNames();
            } catch (Exception e) {
                errorHandler.accept("Failed to connect to channelfinder ", e);
            }
        };

    }
}
