package org.phoebus.applications.alarm.ui.annunciator;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class Annunciator
{
    private final VoiceManager voiceManager; 
    private final Voice        voice;
    
    public Annunciator()
    {
        // Define the voices directory.
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        voiceManager = VoiceManager.getInstance();
        voice = voiceManager.getVoice("kevin16");
        voice.allocate();
    }
    
    public void speak(String message)
    {
        voice.speak(message);
    }
}
