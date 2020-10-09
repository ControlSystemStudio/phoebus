/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import org.phoebus.framework.preferences.PreferencesReader;

/**
 * The CFProperties objects holds the properties associated with the
 * channelfinder client library initialized using the channelfinder_preferences.properties
 * or default values.
 * 
 * The order in which these files will be read. 1. properties file specified
 * using the system property <tt>channelfinder_preferences.properties</tt>. 2.
 * channelfinder_preferences.properties file in the users home direcotory. 3.
 * channelfinder_preferences.properties file in the C:/ on windows and /etc/ on linux. 4.
 * channelfinder_preferences.properties default file packaged with the library.
 * 
 * @author shroffk
 * 
 */
class CFProperties {
    final PreferencesReader prefs;

    CFProperties() {

        prefs = new PreferencesReader(CFProperties.class, "/channelfinder_preferences.properties");
    }

    /**
     * check java preferences for the requested key
     * @param key preferences key
     * @return preferences value
     */
    String getPreferenceValue(String key) {
        return prefs.get(key);
    }

}
