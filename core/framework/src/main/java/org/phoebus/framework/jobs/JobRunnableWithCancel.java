package org.phoebus.framework.jobs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A helper class which provides the framework for running tasks in the background while still monitoring for cancel requests.
 *
 *
 * Ideally, a JobRunnable is written with cancellation and potentially progress updates in mind:
 *
 * JobManager.schedule("Demo", monitor ->
 * {
 *     monitor.beginTask("What I do", 100);
 *     for (int step=0; step < 100; ++step)
 *     {
 *         // Allow cancellation
 *         if (monitor.isCanceled())
 *             return;
 *         // .. do something
 *         monitor.worked(1);
 *     }
 * });
 *
 * If the task cannot be cancelled gracefully because it is for
 * example stuck in network calls or inside a 3rd party library,
 * the  `JobRunnableWithCancel` may be used to brute-force
 * interrupt the task in response to a cancellation request.
 * @author Kunal Shroff
 */
public abstract class JobRunnableWithCancel implements JobRunnable {

    private static final Logger logger = Logger.getLogger(JobRunnableWithCancel.class.getName());
    private static final ExecutorService executorService = Executors.newCachedThreadPool(new NamedThreadFactory(JobRunnableWithCancel.class.getName()));

    /*
     * A default implementation of an error handler for job which logs the exception.
     */
    private BiConsumer<String, Exception> errorHandler = (s, e) -> {
        logger.log(Level.WARNING, s, e);
    };

    /**
     * This is not intended to be Overridden.
     * <p>
     * This method creates a {@link FutureTask} using the {@link Runnable} provided in the concrete implementations.
     * If the job is cancelled, the executor cancels the task (this uses thread interrupt).
     *
     * @param monitor Monitor for reporting progress
     * @throws Exception on error
     */
    @Override
    public void run(JobMonitor monitor) throws Exception
    {
        monitor.beginTask(getName());

        FutureTask task = new FutureTask(getRunnable(), null);

        try {
            executorService.submit(task);
            int count = 0;
            while (!task.isDone())
            {
                if(monitor.isCanceled())
                {
                    task.cancel(true);
                } else {
                    monitor.updateTaskName(getName() + " running for : " + count + " seconds");
                    Thread.currentThread().sleep(1000);
                    count++;
                }
            }
            monitor.done();
        } catch (Exception e)
        {
            errorHandler.accept("Failed to complete " + getName(), e);
        }
    }

    /**
     * The name of the Job
     * @return String - job name
     */
    public abstract String getName();

    /**
     * The actual operation to be performed by this job in a background thread
     * @return Runnable - the operation to be executed by this job
     */
    public abstract Runnable getRunnable();

    /**
     * The error handler for handling exceptions thrown while running this job
     * @return BiConsumer - handles exceptions
     */
    public BiConsumer<String, Exception> getErrorHandler()
    {
        return this.errorHandler;
    }
}
