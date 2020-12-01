package org.phoebus.olog.es.api;

import org.phoebus.framework.preferences.PreferencesReader;

/**
 * The OlogProperties objects holds the properties associated with the
 * olog client library initialized using the olog_es_preferences.properties
 * or default values.
 * 
 * @author shroffk
 * 
 */
public class OlogProperties {

    final PreferencesReader prefs;

    OlogProperties() {
        prefs = new PreferencesReader(OlogProperties.class, "/olog_es_preferences.properties");
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
