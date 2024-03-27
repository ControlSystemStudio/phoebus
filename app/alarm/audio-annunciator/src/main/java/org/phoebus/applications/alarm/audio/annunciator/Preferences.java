/*******************************************************************************
 * Copyright (c) 2010-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.audio.annunciator;


import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;

/**
 * Helper for reading preference settings
 *
 * @author Kunal Shroff
 */
@SuppressWarnings("nls")
public class Preferences {

    /**
     * Setting
     */
    @Preference
    public static String alarm_sound_url;
    @Preference
    public static String minor_alarm_sound_url;
    @Preference
    public static String major_alarm_sound_url;
    @Preference
    public static String invalid_alarm_sound_url;
    @Preference
    public static String undefined_alarm_sound_url;
    @Preference
    public static int volume;

    static {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(AudioAnnunciator.class, Preferences.class, "/audio_annunciator_preferences.properties");
        alarm_sound_url = useLocalResourceIfUnspecified(alarm_sound_url);
        minor_alarm_sound_url = useLocalResourceIfUnspecified(minor_alarm_sound_url);
        major_alarm_sound_url = useLocalResourceIfUnspecified(major_alarm_sound_url);
        invalid_alarm_sound_url = useLocalResourceIfUnspecified(invalid_alarm_sound_url);
        undefined_alarm_sound_url = useLocalResourceIfUnspecified(undefined_alarm_sound_url);
    }

    private static String useLocalResourceIfUnspecified(String alarmResource) {
        if (alarmResource == null || alarmResource.isEmpty()) {
            return Preferences.class.getResource("/sounds/mixkit-classic-alarm-995.wav").toString();
        } else {
            return alarmResource;
        }
    }

}
