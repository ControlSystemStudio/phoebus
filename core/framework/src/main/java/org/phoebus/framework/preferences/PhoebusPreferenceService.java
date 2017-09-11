package org.phoebus.framework.preferences;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * 
 * @author Kunal Shroff
 *
 */
public class PhoebusPreferenceService implements PreferencesFactory {

    public Preferences userRoot() {
        return FileSystemPreferences.getUserRoot();
    }

    public Preferences systemRoot() {
        return FileSystemPreferences.getSystemRoot();
    }

}
