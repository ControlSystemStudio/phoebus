package org.phoebus.framework.preferences;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * A service which enabled the use of the file backed implementation of java {@link Preferences}. 
 * @author Kunal Shroff
 *
 */
public class PhoebusPreferenceService implements PreferencesFactory {

    /** Obtain preference node for a class
     *  @param clazz Class
     *  @return {@link Preferences} for that class
     */
    public static Preferences userNodeForClass(final Class<?> clazz)
    {
        return Preferences.userNodeForPackage(clazz).node(clazz.getSimpleName());
    }

    @Override
    public Preferences userRoot()
    {
        return FileSystemPreferences.getUserRoot();
    }

    @Override
    public Preferences systemRoot()
    {
        return FileSystemPreferences.getSystemRoot();
    }

}
