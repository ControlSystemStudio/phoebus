package org.phoebus.applications.alarm.ui.annunciator;

import java.util.concurrent.CopyOnWriteArrayList;

public class AnnunciatorController
{
    private final Annunciator annunciator;
    private final Thread thread;
    private final CopyOnWriteArrayList<String> to_annunciate = new CopyOnWriteArrayList<String>();
    private int threshold; 
    private volatile boolean muted = false;

    public AnnunciatorController(Annunciator a, int threshold)
    {
        annunciator = a;
        this.threshold = threshold;
        final Runnable speaker = () -> 
        {
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
    
    public void annunciate(String message)
    {
        synchronized(to_annunciate)
        {
            to_annunciate.add(message);
            to_annunciate.notify();
        }
    }
    
    public void mute(boolean val)
    {
        muted = val;
    }
}
