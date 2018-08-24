/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.time.Instant;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import org.phoebus.framework.jobs.JobManager;

/** Controller class for an annunciator.
 *
 *  <p>Annunciates messages in order of severity so long as the message queue remains below
 *  a given threshold. Should the threshold be exceeded, a message saying "There are N new messages"
 *  will be spoken and the message queue will be cleared.
 *
 *  <p>Messages marked as 'standout' will always be annunciated regardless of queue size.
 *
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AnnunciatorController
{
    private final Annunciator annunciator;
    private final Thread      annunciatorThread;

    private final PriorityQueue<AnnunciatorMessage> to_annunciate = new PriorityQueue<>();

    private int threshold;

    // Muted is only ever set in the application thread so it doesn't need to be thread safe.
    // Muted _IS_ read from multiple threads, so it should always be fetched from memory.
    private volatile boolean muted = false;
    private volatile boolean run   = true;

    /**
     * Create AnnunciatorController.
     * @param threshold - Integer value that the length of the queue should not exceed.
     * @param addToTable - callback to add the message to the annunciator table.
     */
    public AnnunciatorController(final int threshold, final Consumer<AnnunciatorMessage> addToTable)
    {
        annunciator = new Annunciator();
        this.threshold = threshold;

        // Runnable that will execute in another thread. Handles speaking and message queue.
        final Runnable speaker = () ->
        {
            // Process new messages until killed.
            while (run)
            {
                synchronized (to_annunciate)
                {
                    // If above threshold, empty the queue, annunciating any stand out messages, and annunciate a generic queue size message.
                    int size = to_annunciate.size();
                    if (size > this.threshold)
                    {
                        int flurry = 0;
                        Instant earliest = Instant.now();
                        // Empty the queue
                        while (! to_annunciate.isEmpty())
                        {
                            AnnunciatorMessage message = to_annunciate.poll();
                            if (earliest.compareTo(message.time) < 0)
                                earliest = message.time;
                            // Annunciate if marked as stand out.
                            if (message.standout)
                            {
                                addToTable.accept(message);
                                if (! muted)
                                    annunciator.speak(message.message);
                            }
                            else // Increment count of non stand out messages.
                                flurry++;
                        }
                        // Annunciate generic message for queue size.
                        if (flurry > 0)
                        {
                            AnnunciatorMessage flurryMessage = new AnnunciatorMessage(false, null, earliest, "There are " + flurry + " new messages");
                            addToTable.accept(flurryMessage);
                            if (! muted)
                                annunciator.speak(flurryMessage.message);
                        }
                    }
                    else // Otherwise, speak the messages in the queue.
                    {
                        while (! to_annunciate.isEmpty())
                        {
                            AnnunciatorMessage message = to_annunciate.poll();
                            addToTable.accept(message);
                            if (! muted)
                                annunciator.speak(message.message);
                        }
                    }
                    // Wait for more.
                    try
                    {
                        to_annunciate.wait();
                    }
                    catch (InterruptedException e)
                    {/* Time to die? */}
                }
            }
        };

        annunciatorThread = new Thread(speaker);
        // The thread should be killed by shutdown() call, but set to daemon so it dies
        // when program closes regardless.
        annunciatorThread.setDaemon(true);
        annunciatorThread.start();
    }


    /**
     * Annunciate the passed message.
     * @param message
     */
    public void annunciate(AnnunciatorMessage message)
    {
        // May block on the to_annunciate queue. So call in another thread to prevent blocking on UI thread.
        JobManager.schedule("annunciate message", (monitor) ->
        {
            // Add the new message and notify the annunciator thread.
            synchronized(to_annunciate)
            {
                to_annunciate.add(message);
                to_annunciate.notifyAll();
            }
        });
    }

    /**
     * Set the muted attribute of the annunciator.
     * @param val - True for muted, False for not muted.
     */
    public void setMuted(final boolean val)
    {
        muted = val;
    }

    /**
     * Shutdown the annunciator controller.
     * @throws InterruptedException
     */
    public void shutdown() throws InterruptedException
    {
        // Set run to false and wake annunciatorThread.
        run = false;
        synchronized(to_annunciate)
        {
            to_annunciate.notifyAll();
        }
        // The thread should shutdown having left the while(run) loop.
        annunciatorThread.join(1000);

        // Deallocate the annunciator's voice.
        annunciator.shutdown();
    }
}
