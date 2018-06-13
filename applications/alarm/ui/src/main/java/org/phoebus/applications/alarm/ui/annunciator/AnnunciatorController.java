/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.util.PriorityQueue;

import org.phoebus.applications.alarm.talk.Annunciation;
import org.phoebus.framework.jobs.JobManager;

/**
 * Controller class for an annunciator class.
 * <p>Annunciates messages so long as the message queue remains below a given threshold.
 * Should the threshold be exceeded a message saying "There are N new messages" will be
 * spoken and the message queue will be cleared.
 * @author Evan Smith
 *
 */
public class AnnunciatorController
{
    private final Annunciator annunciator;
    private final Thread annunciatorThread;
    
    MessageComparator messageComparator = new MessageComparator();
    private final PriorityQueue<String> to_annunciate = new PriorityQueue<String>(messageComparator);
    
    private int threshold; 
    // Muted is only ever set in the application thread so it doesn't need to be thread safe.
    // Muted _IS_ read from multiple threads, so it should always be fetched from memory.
    private volatile Boolean muted = false;
    private volatile Boolean run = true;
    /**
     * Constructor. The annunciator must be non null and the threshold must be greater than 0.
     * @param a - Annunciator.
     * @param threshold - Integer value that the length of the queue should not exceed.
     */
    public AnnunciatorController(int threshold)
    {
       annunciator = new Annunciator();
        if (threshold > 0)
            this.threshold = threshold;
        else
            threshold = 3;
        
        // Runnable that will execute in another thread. Handles speaking and message queue.
        final Runnable speaker = () -> 
        {
            // Process new messages until killed.
            while(run)
            {
                synchronized (to_annunciate)
                {
                    //Clear the queue if above the threshold.
                    int size = to_annunciate.size();
                    if (size > this.threshold)
                    {
                        synchronized (muted)
                        {
                            if (! muted)
                                annunciator.speak("There are " + size + " new messages.");
                        }
                        to_annunciate.clear();
                    }
                    else // Otherwise speak the messages in the queue.
                    {
                        while (! to_annunciate.isEmpty())
                        {
                            String message = to_annunciate.poll();
                            synchronized (muted)
                            {
                                if (! muted)
                                    annunciator.speak(message);
                            }
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
        annunciatorThread.setDaemon(true);
        annunciatorThread.start();
    }
    
    /**
     * Annunciate the passed message.
     * @param message
     */
    private void annunciate(String message)
    {
        // May block on the to_annunciate queue. So call in another thread to prevent blocking on UI thread.
        JobManager.schedule("annunciate message", (monitor) -> 
        {
            // Add the new message and notify the speech thread.
            synchronized(to_annunciate)
            {
                to_annunciate.add(message);
                to_annunciate.notifyAll();
            }
        });
    }
    
    /**
     * Handle an annunciation by notifying the speaker thread that a message has been received.
     * @param a - Annunciation
     */
    public void handleAnnunciation(Annunciation a)
    {
        if (! a.message.get().startsWith("*"))
        {
            final String message = a.severity.get().toString() + " Alarm: " + a.message.get();
            annunciate(message);
        }
        else
        {
            annunciate(a.message.get());
        }
    }
    
    /**
     * Mute the annunciator.
     * @param val True for muted, False for not muted.
     */
    public void mute(boolean val)
    {
        synchronized (muted)
        {
            muted = val;
        }
    }

    /**
     * Shutdown the annunciator controller.
     * @throws InterruptedException
     */
    public void shutdown() throws InterruptedException
    {
        synchronized(run)
        {
            run = false;
        }
        annunciator.shutdown();
    }
}
