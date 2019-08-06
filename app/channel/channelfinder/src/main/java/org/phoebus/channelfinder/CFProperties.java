/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.phoebus.framework.preferences.PreferencesReader;

/**
 * The CFProperties objects holds the properties associated with the
 * channelfinder client library initialized using the channelfinder.properties
 * or default values.
 * 
 * The order in which these files will be read. 1. properties file specified
 * using the system property <tt>channelfinder.properties</tt>. 2.
 * channelfinder.properties file in the users home direcotory. 3.
 * channelfinder.properties file in the C:/ on windows and /etc/ on linux. 4.
 * channelfinder.properties default file packaged with the library.
 * 
 * @author shroffk
 * 
 */
class CFProperties {
    final PreferencesReader prefs;

    CFProperties() {

        prefs = new PreferencesReader(CFProperties.class, "/channelfinder.properties");
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
