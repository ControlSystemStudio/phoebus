/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.audio.annunciator;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
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
    private final MediaPlayer alarmSound;
    private final MediaPlayer minorAlarmSound;
    private final MediaPlayer majorAlarmSound;
    private final MediaPlayer invalidAlarmSound;
    private final MediaPlayer undefinedAlarmSound;

    /**
     * Constructor
     */
    public AudioAnnunciator() {
        alarmSound = new MediaPlayer(new Media(Preferences.alarm_sound_url));
        minorAlarmSound = new MediaPlayer(new Media(Preferences.minor_alarm_sound_url));
        majorAlarmSound = new MediaPlayer(new Media(Preferences.major_alarm_sound_url));
        invalidAlarmSound = new MediaPlayer(new Media(Preferences.invalid_alarm_sound_url));
        undefinedAlarmSound = new MediaPlayer(new Media(Preferences.undefined_alarm_sound_url));
        // configure the media players for the different alarm sounds
        List.of(alarmSound, minorAlarmSound, majorAlarmSound, invalidAlarmSound, undefinedAlarmSound)
                .forEach(sound -> {
                    sound.setStopTime(Duration.seconds(Preferences.max_alarm_duration));
                    sound.setVolume(Preferences.volume);
                });
    }

    /**
     * Annunciate the message.
     *
     * @param message Message text
     */
    @Override
    public void speak(final AnnunciatorMessage message) {
        switch (message.severity) {
            case MINOR -> speakAlone(minorAlarmSound);
            case MAJOR -> speakAlone(majorAlarmSound);
            case INVALID -> speakAlone(invalidAlarmSound);
            case UNDEFINED -> speakAlone(undefinedAlarmSound);
            default -> speakAlone(alarmSound);
        }
    }

    synchronized private void speakAlone(MediaPlayer alarm) {
        List.of(alarmSound, minorAlarmSound, majorAlarmSound, invalidAlarmSound, undefinedAlarmSound)
                .forEach(sound -> {
                    sound.stop();
                });
        alarm.play();
    }

    /**
     * Deallocates the voice.
     */
    @Override
    public void shutdown() {
        List.of(alarmSound, minorAlarmSound, majorAlarmSound, invalidAlarmSound, undefinedAlarmSound)
                .forEach(sound -> {
                    sound.stop();
                    sound.dispose();
                });
    }
}
