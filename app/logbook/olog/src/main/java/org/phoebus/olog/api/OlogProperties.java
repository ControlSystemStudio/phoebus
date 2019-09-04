package org.phoebus.olog.api;

import org.phoebus.framework.preferences.PreferencesReader;

public class OlogProperties {

    final PreferencesReader prefs;

    OlogProperties() {

        prefs = new PreferencesReader(OlogProperties.class, "/olog.properties");
    }

    /**
     * check java preferences for the requested key
     * @param key
     * @return
     */
    String getPreferenceValue(String key) {
        return prefs.get(key);
    }

}
