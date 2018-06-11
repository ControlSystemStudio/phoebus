package org.phoebus.applications.alarm;

import com.sun.speech.freetts.VoiceManager;

public class VoiceTest
{   
    public static void main(String[] args)
    {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        final VoiceManager voiceManager = VoiceManager.getInstance();
        final com.sun.speech.freetts.Voice[] voices = voiceManager.getVoices();
        for (int i = 0; i < voices.length; i++)
        {
        System.out.println(voices[i].getName() +
                ": " + voices[i].getGender() + ", " + voices[i].getAge());
        }
    }
}
