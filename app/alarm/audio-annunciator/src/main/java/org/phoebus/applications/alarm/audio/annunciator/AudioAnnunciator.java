/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.audio.annunciator;

import javafx.scene.media.AudioClip;
import org.phoebus.applications.alarm.ui.annunciator.Annunciator;
import org.phoebus.applications.alarm.ui.annunciator.AnnunciatorMessage;

import java.util.List;

/**
 * Annunciator class. Uses Audio files to annunciate passed messages.
 *
 * @author Kunal Shroff
 */
@SuppressWarnings("nls")
public class AudioAnnunciator implements Annunciator {
    private final AudioClip alarmSound;
    private final AudioClip minorAlarmSound;
    private final AudioClip majorAlarmSound;
    private final AudioClip invalidAlarmSound;
    private final AudioClip undefinedAlarmSound;

    /**
     * Constructor
     */
    public AudioAnnunciator() {
        alarmSound = new AudioClip(Preferences.alarm_sound_url);
        minorAlarmSound = new AudioClip(Preferences.minor_alarm_sound_url);
        majorAlarmSound = new AudioClip(Preferences.major_alarm_sound_url);
        invalidAlarmSound = new AudioClip(Preferences.invalid_alarm_sound_url);
        undefinedAlarmSound = new AudioClip(Preferences.undefined_alarm_sound_url);
    }

    /**
     * Annunciate the message.
     *
     * @param message Message text
     */
    @Override
    public void speak(final AnnunciatorMessage message) {
        switch (message.severity) {
            case MINOR -> minorAlarmSound.play(Preferences.volume);
            case MAJOR -> majorAlarmSound.play(Preferences.volume);
            case INVALID -> invalidAlarmSound.play(Preferences.volume);
            case UNDEFINED -> undefinedAlarmSound.play(Preferences.volume);
            default -> alarmSound.play(Preferences.volume);
        }
    }

    /**
     * Deallocates the voice.
     */
    @Override
    public void shutdown() {
        List.of(alarmSound, minorAlarmSound, majorAlarmSound, invalidAlarmSound, undefinedAlarmSound)
                .forEach(sound -> sound.stop());
    }
}
