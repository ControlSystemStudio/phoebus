package org.phoebus.applications.alarm.ui.annunciator;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.alarm.talk.Annunciation;
import org.phoebus.framework.jobs.NamedThreadFactory;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class Annunciator
{
    private final VoiceManager voiceManager = VoiceManager.getInstance();
    private final Voice        voice        = voiceManager.getVoice("kevin16");
    private int threshold = 3;
    private long timeout_secs = 5;
    
    private final CopyOnWriteArrayList<Annunciation> to_annunciate = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Annunciation> message_queue = new CopyOnWriteArrayList<>();
    
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Timer"));
    private final Runnable speaker = () -> annunciate();
    
    public Annunciator(int threshold, long time)
    {
        // If sensible arguments are not given, maintain defaults.
        if (threshold > 1)
            this.threshold = threshold;
        if (timeout_secs > 1)
            timeout_secs = time;
    }
    
    public void annunciate(Annunciation a)
    {
        message_queue.add(a);
    }

    // TODO Integrate freeTTS into method.
    private void annunciate()
    {
        synchronized(message_queue)
        {
            to_annunciate.addAll(message_queue);
            message_queue.clear();
        }
        
        if (to_annunciate.size() > threshold)
        {
            String msg = "There were " + to_annunciate.size() + " messages recieved in the last " + timeout_secs + " seconds";
            System.out.println(msg);
            voice.allocate();
            voice.speak(msg);
            voice.deallocate();
        }
        else
        {
            for (Annunciation a : to_annunciate)
            {
                voice.allocate();
                voice.speak(a.message.get());
                voice.deallocate();
                System.out.println(a.time_received + " " + a.severity + " " + a.message);
            }
        }
        to_annunciate.clear();
    }
    public void start() 
    {
        timer.scheduleAtFixedRate(speaker, timeout_secs, timeout_secs, TimeUnit.SECONDS);
    }
    
    public void stop()
    {
        timer.shutdown();
    }
}
