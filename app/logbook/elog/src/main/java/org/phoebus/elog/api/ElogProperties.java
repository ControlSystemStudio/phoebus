package org.phoebus.elog.api;

import org.phoebus.framework.preferences.PreferencesReader;

public class ElogProperties {

    final PreferencesReader prefs;

    ElogProperties() {
        prefs = new PreferencesReader(ElogProperties.class, "/elog.properties");
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
