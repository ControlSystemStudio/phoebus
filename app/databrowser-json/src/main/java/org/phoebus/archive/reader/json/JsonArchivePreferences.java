/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.json;

import org.phoebus.framework.preferences.PreferencesReader;

import java.util.logging.Logger;

/**
 * <p>
 * Preferences used by the {@link JsonArchiveReader}.
 * </p>
 * <p>
 * Each of the parameters corresponds to a property in the preferences system,
 * using the <code>org.phoebus.archive.reader.json</code> namespace.
 * </p>
 * <p>
 * Please refer to the <code>archive_reader_json_preferences.properties</code>
 * file for a full list of available properties and their meanings.
 * </p>
 *
 * @param honor_zero_precision
 *  flag indicating whether a floating-point value specifying a precision of
 *  zero shall be printed without any fractional digits (<code>true</code>) or
 *  whether such a value should be printed using a default format
 *  (<code>false</code>).
 */
public record JsonArchivePreferences(
        boolean honor_zero_precision) {

    private final static JsonArchivePreferences DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = loadPreferences();
    }

    /**
     * Returns the default instance of the preferences. This is the instance
     * that is automatically configured through Phoebus’s
     * {@link PreferencesReader}.
     *
     * @return preference instance created using the {@link PreferencesReader}.
     */
    public static JsonArchivePreferences getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static JsonArchivePreferences loadPreferences() {
        final var logger = Logger.getLogger(
                JsonArchivePreferences.class.getName());
        final var preference_reader = new PreferencesReader(
                JsonArchivePreferences.class,
                "/archive_reader_json_preferences.properties");
        final var honor_zero_precision = preference_reader.getBoolean(
                "honor_zero_precision");
        logger.config("honor_zero_precision = " + honor_zero_precision);
        return new JsonArchivePreferences(honor_zero_precision);
    }

}
