/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final Thread thread;
    private final CopyOnWriteArrayList<String> to_annunciate = new CopyOnWriteArrayList<String>();
    private int threshold; 
    // Muted is only ever set in the application thread so it doesn't need to be thread safe.
    // Muted _IS_ read from multiple threads, so it should always be fetched from memory.
    private volatile boolean muted = false;

    MessageComparator messageComparator = new MessageComparator();
    
    /**
     * Constructor. The annunciator must be non null and the threshold must be greater than 0.
     * @param a - Annunciator.
     * @param threshold - Integer value that the length of the queue should not exceed.
     */
    public AnnunciatorController(Annunciator a, int threshold)
    {
        annunciator = Objects.requireNonNull(a);
        if (threshold > 0)
            this.threshold = threshold;
        else
            threshold = 3;
        
        // Runnable that will execute in another thread. Handles speaking and message queue.
        final Runnable speaker = () -> 
        {
            // Wait for new messages until shutdown.
            while(true)
            {
                try
                {
                    synchronized(to_annunciate)
                    {
                        to_annunciate.wait();
                    }
                } 
                catch (InterruptedException e1)
                {
                    // What do?
                }
                
                // Make size based annunciation decisions quickly not holding list for long. 
                // AnnunciatorTable thread waits on this.
                int size = 0;
                synchronized (to_annunciate)
                {
                    size = to_annunciate.size();
                    if (size > this.threshold)
                    {
                        to_annunciate.clear();
                    }
                }
                
                // Speak based on size vs. threshold.
                if (size > this.threshold)
                {
                    synchronized (annunciator)
                    {
                        if (! muted)
                            annunciator.speak("There are " + size + " new messages.");
                    }
                }
                else
                {
                    synchronized (annunciator)
                    {
                        if(! muted)
                            annunciator.speak(to_annunciate.remove(0));
                    }
                }
            }
        };
        thread = new Thread(speaker);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Annunciate the passed message.
     * @param message
     */
    public void annunciate(String message)
    {
        // Add the new message and notify the speech thread.
        synchronized(to_annunciate)
        {
            to_annunciate.add(message);
            to_annunciate.sort(messageComparator);
            to_annunciate.notify();
        }
    }
    
    /**
     * Mute the annunciator.
     * @param val True for muted, False for not muted.
     */
    public void mute(boolean val)
    {
        muted = val;
    }
}
