/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.util.PriorityQueue;

import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.talk.Annunciation;
import org.phoebus.framework.jobs.JobManager;

/**
 * Controller class for an annunciator.
 * <p>Annunciates messages in order of severity so long as the message queue remains below 
 * a given threshold. Should the threshold be exceeded, a message saying "There are N new messages" 
 * will be spoken and the message queue will be cleared.
 * <p>Messages marked as 'standout' will always be annunciated regardless of queue size.
 * @author Evan Smith
 */
public class AnnunciatorController
{
    private final Annunciator annunciator;
    private final Thread annunciatorThread;
    
    private final PriorityQueue<AnnunciatorMessage> to_annunciate = new PriorityQueue<AnnunciatorMessage>();
    
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
                    // If above threshold, empty the queue, annunciating any stand out messages, and annunciate a generic queue size message.
                    int size = to_annunciate.size();
                    if (size > this.threshold)
                    {
                        int flurry = 0;
                        // Empty the queue 
                        while (! to_annunciate.isEmpty())
                        {
                            AnnunciatorMessage message = to_annunciate.poll();
                            // Annunciate if marked as stand out.
                            if (message.standout)
                            {
                                synchronized (muted)
                                {
                                    if (! muted)
                                        annunciator.speak(message.message);
                                }
                            }
                            else // Increment count of non stand out messages.
                                flurry++;
                        }
                        // Annunciate generic message for queue size.
                        if (flurry > 0)
                        {
                            synchronized (muted)
                            {
                                if (! muted)
                                    annunciator.speak("There are " + flurry + " new messages.");
                            }
                        }
                    }
                    else // Otherwise, speak the messages in the queue.
                    {
                        while (! to_annunciate.isEmpty())
                        {
                            AnnunciatorMessage message = to_annunciate.poll();
                            synchronized (muted)
                            {
                                if (! muted)
                                    annunciator.speak(message.message);
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
        // The thread should be killed by shutdown() call, but set to daemon so it dies 
        // when program closes regardless.
        annunciatorThread.setDaemon(true);
        annunciatorThread.start();
    }
    

    /**
     * Annunciate the passed message.
     * @param message
     */
    private void annunciate(AnnunciatorMessage message)
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
     * Handle an annunciation by notifying the speaker thread that a message has been received.
     * @param a - Annunciation
     */
    public void handleAnnunciation(Annunciation a)
    {
        final String description = a.description.get();
        final SeverityLevel severity = a.severity.get();
        
        String message = description; // Message to be annunciated.
        boolean noSev = false;      // Message should include alarm severity.
        boolean standout = false;   // Message should always be annunciated.
        
        int beginIndex = 0; // Beginning index of description substring.
        
        if (description.startsWith("*"))
        {
            noSev = true;
            beginIndex++;
        }
        if (description.substring(beginIndex).startsWith("!"))
        {
            standout = true;
            beginIndex++;
        }
        
        // The message should not include '*' or '!'. 
        // If '*' or '!' is the entirety of the description, message will be an empty string.
        message = description.substring(beginIndex);
        
        // Add the severity if appropriate.
        if (! noSev)
        {
            message = severity.toString() + " Alarm: " + message;
        }
        
        annunciate(new AnnunciatorMessage(standout, severity, message));
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
        // Set run to false and interrupt the annunciatorThread.
        synchronized(run)
        {
            run = false;
        }
        // The thread will shutdown having left the while(run) loop.
        annunciatorThread.interrupt();
        // Deallocate the annunciator's voice.
        annunciator.shutdown();
    }
}
