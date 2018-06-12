package org.phoebus.applications.alarm.ui.annunciator;

import java.util.concurrent.CopyOnWriteArrayList;

public class AnnunciatorController
{
    //private final Annunciator annunciator;
    private final Thread thread;
    private final CopyOnWriteArrayList<String> to_annunciate = new CopyOnWriteArrayList<String>();
    private int threshold;    

    private final Runnable speaker = () -> 
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
            }
            
            if (to_annunciate.size() > threshold)
            {
                // Simulate speaking.
                try
                {
                    Thread.sleep(2500);
                } 
                catch (InterruptedException e)
                {
                }
                System.out.println("There are " + to_annunciate.size() + " messages.");
                to_annunciate.clear();
            }
            else
            {
             // Simulate speaking.
                try
                {
                    Thread.sleep(2500);
                } 
                catch (InterruptedException e)
                {
                }
                System.out.println(to_annunciate.remove(0));
            }
        }
    };
    public AnnunciatorController(Annunciator a, int threshold)
    {
        //annunciator = a;
        this.threshold = threshold;
        thread = new Thread(speaker);
        thread.setDaemon(true);
        thread.start();
    }
    
    public void annunciate(String message)
    {
        to_annunciate.add(message);
        synchronized(to_annunciate)
        {
            to_annunciate.notify();
        }
    }
}
