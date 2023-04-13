/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.preferences;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Properties;
import java.util.prefs.Preferences;

/** Load preferences from a property file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PropertyPreferenceLoader
{
    /** Load preferences from a property file
     *
     *  <p>Properties have the name "package/setting",
     *  where the package name in dot notation
     *  is used to locate the preference node.
     *
     *  @param stream Stream for property file
     *  @throws Exception on error
     */
    public static void load(final InputStream stream) throws Exception
    {
        final Properties props = new Properties();
        props.load(stream);

        for (String prop : props.stringPropertyNames())
        {
            final int sep = prop.lastIndexOf('/');
            if (sep < 0)
                throw new Exception("Expected 'package_name/setting = value', got property '" + prop + "'");

            final String pack = "/" + prop.substring(0, sep).replace('.', '/');
            final String name = prop.substring(sep+1);
            final String value = props.getProperty(prop);
            final Preferences prefs = Preferences.userRoot().node(pack);
            prefs.put(name, value);
            // System.out.println(pack + "/" + name + "=" + value);
        }
    }

    /**
     * Loads settings from file or remote URL.
     * @param location Location speciifying a file name or a remote http(s) URL.
     * @throws Exception If settings cannot be loaded, e.g. file not found or invalid URL.
     */
    public static void load(String location) throws Exception{
        if(location.substring(0, 4).equalsIgnoreCase("http")){
            loadFromRemoteURL(location);
        }
        else{
            load(new FileInputStream(location));
        }
    }

    /**
     * Load settings from remote URL.
     * @param uri A remote URL.
     * @throws Exception If settings cannot be loaded, e.g. invalid URL.
     */
    private static void loadFromRemoteURL(final String uri) throws Exception{

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();
        HttpResponse<String> response =
                httpClient.send(httpRequest, BodyHandlers.ofString());
        load(new ByteArrayInputStream(response.body().getBytes()));
    }
}
