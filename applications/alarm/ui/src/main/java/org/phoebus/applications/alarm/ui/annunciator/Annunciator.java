/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

/**
 * Annunciator class. Uses freeTTS to annunciate passed messages.
 * @author Evan Smith
 * 
 */
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
    
    /**
     * Annunciate the message. Only returns once speaking finishes.
     * @param message
     */
    public void speak(String message)
    {
        voice.speak(message);
    }
}
