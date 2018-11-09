/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

import static org.csstudio.archive.Engine.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Level;

import org.csstudio.archive.writer.ArchiveWriter;
import org.csstudio.archive.writer.ArchiveWriterFactory;
import org.csstudio.archive.writer.WriteChannel;
import org.epics.vtype.VType;

/** Thread that writes values from multiple <code>SampleBuffer</code>s
 *  to an <code>RDBArchiveServer</code>.
 *  <p>
 *  When there are write errors, it sets the sample buffer error state
 *  and tries to reconnect to the database and write again until successful.
 *  Since the Oracle batch mechanism doesn't tell us what exactly failed
 *  in a batch, all the samples that were part of the batch might
 *  be lost.
 *  The channels that add samples to the sample buffer supposedly notice
 *  the error condition and add a special indicator once we recover.
 *
 *  @author Kay Kasemir
 */
public class WriteThread implements Runnable
{
    /** Minimum write period [seconds] */
    private static final double MIN_WRITE_PERIOD = 5.0;

    /** Server to which this thread writes. */
    private ArchiveWriter writer;

    /** All the sample buffers this thread writes. */
    final private ArrayList<SampleBuffer> buffers =
        new ArrayList<>();

    /** Flag that tells the write thread to run or quit. */
    private boolean do_run;

    /** Synchronization block for waiting.
     *  Signaled in stop().
     */
    private Object wait_block = new Object();

    /** Delay between write runs. */
    private long millisec_delay = 5000;

    /** Number of values to place into one batch */
    private int batch_size = 500;

    /** Time of end of last write run */
    private Instant last_write_stamp = null;

    /** Average number of values per write run */
    private Average write_count = new Average();

    /** Average duration of write run */
    private Average write_time = new Average();

    /** Thread the executes this.run() */
    private Thread thread;

    /** Add a channel's buffer that this thread reads */
    public void addChannel(final ArchiveChannel channel)
    {
        addSampleBuffer(channel.getSampleBuffer());
    }

    /** Add a sample buffer that this thread reads */
    void addSampleBuffer(final SampleBuffer buffer)
    {
        buffers.add(buffer);
    }

    /** Start the write thread.
     *  @param write_period Period between writes in seconds
     *  @param batch_size Number of values to batch
     */
    @SuppressWarnings("nls")
    public void start(double write_period, int batch_size)
    {
        if (write_period < MIN_WRITE_PERIOD)
        {
            logger.log(Level.INFO, "Adjusting write period from {0} to {1}",
                new Object[] { write_period, MIN_WRITE_PERIOD });
            write_period = MIN_WRITE_PERIOD;
        }
        millisec_delay = (int)(1000.0 * write_period);
        this.batch_size = batch_size;
        thread = new Thread(this, "WriteThread");
        thread.start();
    }

    /** Reset statistics */
    public void reset()
    {
        write_count.reset();
        write_time.reset();
    }

    /** Ask the write thread to stop ASAP. */
    private void stop()
    {
        do_run = false;
        synchronized (wait_block)
        {
            wait_block.notify();
        }
    }

    /** @return Timestamp of end of last write run */
    public Instant getLastWriteTime()
    {
        return last_write_stamp;
    }

    /** @return Average number of values per write run */
    public double getWriteCount()
    {
        return write_count.get();
    }

    /** @return  Average duration of write run in seconds */
    public double getWriteDuration()
    {
        return write_time.get();
    }

    /** 'Main loop' of the write thread.
     *  <p>
     *  Writes all values out, then waits.
     *  The idea is that waiting a little for values
     *  to accumulate actually helps, because then
     *  we may write a few values per channel,
     *  so the effort for locating a channel's ID
     *  and the batching actually pays off.
     *  <p>
     *  Since the wait time can be considerable (30 seconds?),
     *  we wait on a semaphore (wait_block), which
     *  can be notified in stop() to cause an ASAP exit.
     */
    @Override
    @SuppressWarnings("nls")
    public void run()
    {
        logger.info("WriteThread starts");
        boolean write_error = false;
        do_run = true;
        while (do_run)
        {
            long delay;
            try
            {
                // If there was an error before...
                if (write_error)
                {   // .. try to reconnect
                    if (writer != null)
                    {
                        writer.close();
                        writer = null;
                    }
                    // If we get here, all is OK so far ...
                    write_error = false;
                    // .. and we continue to write.
                }
                if (writer == null)
                    writer = ArchiveWriterFactory.getArchiveWriter();
                final long start = System.currentTimeMillis();
                // In case of a network problem, we can hang in here
                // for a long time...
                final long written = write();
                final long milli = System.currentTimeMillis() - start;
                last_write_stamp = Instant.now();
                write_count.update(written);
                write_time.update(milli / 1000.0);
                // How much of the scheduled delay is left after write()?
                delay = millisec_delay - milli;
            }
            catch (Exception ex)
            {   // Error in write() or the preceding reconnect()...
                logger.log(Level.WARNING, "Error, will try to reconnect", ex);
                // Use max. delay
                delay = millisec_delay;
                write_error = true;
            }
            SampleBuffer.setErrorState(write_error);
            // See if there's any time left to wait,
            // or if we already used all that time in the last 'write'
            if (delay > 0)
            {
                synchronized (wait_block)
                {
                    try
                    {
                        wait_block.wait(delay);
                    }
                    catch (InterruptedException ex)
                    {
                        logger.log(Level.WARNING, "Interrupted wait", ex);
                    }
                }
            }
        }
        logger.info("WriteThread exists");
    }

    /** Stop the write thread, performing a final write. */
    public void shutdown() throws Exception
    {
        // Stop the thread
        stop();
        // Wait for it to end
        thread.join();
        // Then write once more.
        // Errors in this last write are passed up.
        try
        {
            write();
        }
        finally
        {
            if (writer != null)
            {
                writer.close();
                writer = null;
            }
        }
    }

    /** Write right now until all sample buffers are empty
     *  @return number of samples written
     */
    private long write() throws Exception
    {
        int total_count = 0;
        int count = 0;
        for (SampleBuffer buffer : buffers)
        {
            // Update max buffer length etc. before we start to remove samples
            buffer.updateStats();
            // Write samples for one channel
            final String name = buffer.getChannelName();
            final String retention = buffer.getArchiveDataRetention();
            final WriteChannel channel = writer.getChannel(name, retention);
            VType sample = buffer.remove();
            while (sample != null)
            {   // Write one value
                writer.addSample(channel, sample);
                // Note: count across different sample buffers!
                ++count;
                if (count > batch_size)
                {
                    total_count += count;
                    count = 0;
                    writer.flush();
                }
                // next
                sample = buffer.remove();
            }
        }
        // Flush remaining samples (less than batch_size)
        writer.flush();
        total_count += count;
        return total_count;
    }
}
