/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.client;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/**
 * HTTP preferences for save-and-restore client.
 */
public class Preferences {

    /**
     * Where to find the service...
     */
    @Preference(name = "jmasar.service.url")
    public static String jmasarServiceUrl;

    /**
     * Timeout to read response from service. This may need to be increased from default 5000 ms
     * if client is handling snapshots with very large number of PVs.
     */
    @Preference(name = "httpClient.readTimeout")
    public static int httpClientReadTimeout;

    /**
     * Timeout for client connection to service.
     */
    @Preference(name = "httpClient.connectTimeout")
    public static int httpClientConnectTimeout;

    static {
        AnnotatedPreferences.initialize(Preferences.class, "/save_and_restore_client_preferences.properties");
    }
}
