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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Annunciator class. Uses Audio files to annunciate passed messages.
 *
 * @author Kunal Shroff
 */
@SuppressWarnings("nls")
public class AudioAnnunciator implements Annunciator {
    private static final Logger logger = Logger.getLogger(AudioAnnunciator.class.getName());
    private final MediaPlayer alarmSound;
    private final MediaPlayer minorAlarmSound;
    private final MediaPlayer majorAlarmSound;
    private final MediaPlayer invalidAlarmSound;
    private final MediaPlayer undefinedAlarmSound;

    /**
     * Constructor
     */
    public AudioAnnunciator() {
        alarmSound = createMediaPlayer(Preferences.alarm_sound_url);
        minorAlarmSound = createMediaPlayer(Preferences.minor_alarm_sound_url);
        majorAlarmSound = createMediaPlayer(Preferences.major_alarm_sound_url);
        invalidAlarmSound = createMediaPlayer(Preferences.invalid_alarm_sound_url);
        undefinedAlarmSound = createMediaPlayer(Preferences.undefined_alarm_sound_url);
        // configure the media players for the different alarm sounds
        List.of(alarmSound, minorAlarmSound, majorAlarmSound, invalidAlarmSound, undefinedAlarmSound)
                .forEach(sound -> {
                    if (sound != null) {
                        sound.setStopTime(Duration.seconds(Preferences.max_alarm_duration));
                        sound.setVolume(Preferences.volume);
                    }
                });
    }

    /**
     * Create a MediaPlayer for the given URL
     *
     * @param url URL of the audio file
     * @return MediaPlayer
     */
    private MediaPlayer createMediaPlayer(String url) {
        try {
            MediaPlayer player = new MediaPlayer(new Media(url));
            if (player.getError() == null) {
                player.setOnError(() -> logger.log(Level.SEVERE, "Error playing alarm sound: " + url, player.getError()));
                player.setOnPlaying(() -> logger.log(Level.FINE, "Playing alarm sound: " + url));
                player.setOnStopped(() -> logger.log(Level.FINE, "Alarm sound stopped: " + url));
                player.setOnEndOfMedia(() -> logger.log(Level.FINE, "Alarm sound finished: " + url));
                return player;
            }
            else {
                logger.log(Level.SEVERE, "Error creating MediaPlayer for URL: " + url, player.getError());
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create MediaPlayer for URL: " + url, e);
            return null;
        }
    }

    /**
     * Annunciate the message.
     *
     * @param message Message text
     */
    @Override
    public void speak(final AnnunciatorMessage message) {
        if (message == null) {
            logger.log(Level.WARNING, "Received null AnnunciatorMessage");
            return;
        }
        if (message.severity == null) {
            logger.log(Level.WARNING, "Received AnnunciatorMessage with null severity: " + message + ". Playing default alarm sound");
            speakAlone(alarmSound); // Play the default alarm sound
            return;
        }
        switch (message.severity) {
            case MINOR -> speakAlone(minorAlarmSound);
            case MAJOR -> speakAlone(majorAlarmSound);
            case INVALID -> speakAlone(invalidAlarmSound);
            case UNDEFINED -> speakAlone(undefinedAlarmSound);
            default -> speakAlone(alarmSound);
        }
    }

    /**
     * Play the alarm sound alone by first stopping any alarm sounds.
     *
     * @param alarm Alarm sound
     */
    synchronized private void speakAlone(MediaPlayer alarm) {
        if (alarm == null) {
            logger.log(Level.WARNING, "Alarm sound is null, cannot play sound");
            return;
        }
        List.of(alarmSound, minorAlarmSound, majorAlarmSound, invalidAlarmSound, undefinedAlarmSound)
                .forEach(sound -> {
                    if (sound != null) {
                        sound.stop();
                    }
                });
        try {
            alarm.play();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to play alarm sound", e);
        }
    }

    /**
     * Deallocates the voice.
     */
    @Override
    public void shutdown() {
        List.of(alarmSound, minorAlarmSound, majorAlarmSound, invalidAlarmSound, undefinedAlarmSound)
                .forEach(sound -> {
                    if (sound != null) {
                        try {
                            sound.stop();
                            sound.dispose();
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Failed to stop and dispose alarm sound" , e);
                        }
                    }
                });
    }
}
