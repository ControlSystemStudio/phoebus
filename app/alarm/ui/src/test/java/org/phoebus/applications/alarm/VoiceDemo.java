/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

/** Demo of FreeTTS voice
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class VoiceDemo
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
