package org.phoebus.applications.alarm;


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

        voice.speak("Hello. My name is kevin.");
    }
}
