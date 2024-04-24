/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.freetts.annunciator;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import org.phoebus.applications.alarm.ui.annunciator.Annunciator;
import org.phoebus.applications.alarm.ui.annunciator.AnnunciatorMessage;

/**
 * Annunciator class. Uses freeTTS to annunciate passed messages.
 * @author Evan Smith, Kunal Shroff
 */
@SuppressWarnings("nls")
public class FreeTTSAnnunciator implements Annunciator
{
    private final VoiceManager  voiceManager;
    private final Voice         voice;
    private static final String voice_name = "kevin16";

    /** Constructor */
    public FreeTTSAnnunciator()
    {
        // Define the voices directory.
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        voiceManager = VoiceManager.getInstance();
        voice = voiceManager.getVoice(voice_name);
        voice.allocate();
    }

    /**
     * Annunciate the message. Only returns once speaking finishes.
     * @param message Message text
     */
    @Override
    public void speak(final AnnunciatorMessage message)
    {
        if (null != message)
            voice.speak(message.message);
    }

    /**
     * Deallocates the voice.
     */
    @Override
    public void shutdown()
    {
        voice.deallocate();
    }
}
