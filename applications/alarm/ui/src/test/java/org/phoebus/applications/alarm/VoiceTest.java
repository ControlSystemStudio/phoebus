package org.phoebus.applications.alarm;

import org.phoebus.applications.alarm.ui.annunciator.NotifyingConcurrentArrayList;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class VoiceTest
{   
    public static void main(String[] args)
    {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        final VoiceManager voiceManager = VoiceManager.getInstance();
        final Voice voice = voiceManager.getVoice("kevin16");
        voice.allocate();

        NotifyingConcurrentArrayList<String> messages = new NotifyingConcurrentArrayList<>();
        
        messages.setOnAddition(() ->
        {
            String message = messages.back();
            voice.speak(message);
        });
        
        messages.setOnRemoval(() ->
        {
            voice.speak("Message removed from list.");
        });
        messages.add("Test 1 2 3 Test 1 2 3");
        String message = messages.popBack();
        System.out.println(message);
    }
}
