/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.preferences;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.phoebus.framework.workbench.Locations;

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
            String value = props.getProperty(prop);

            if (value.contains("$(phoebus.install)"))
                value = value.replace("$(phoebus.install)", Locations.install().toString()).replace("\\", "/").replace(" ", "%20");
            if (value.contains("$(phoebus.user)"))
                value = value.replace("$(phoebus.user)", Locations.user().toString()).replace("\\", "/").replace(" ", "%20");
            if (value.contains("$(user.home)"))
                value = value.replace("$(user.home)", System.getProperty("user.home").toString()).replace("\\", "/").replace(" ", "%20");

            
            final Preferences prefs = Preferences.userRoot().node(pack);
            prefs.put(name, value);
            // System.out.println(pack + "/" + name + "=" + value);
        }
    }

    /**
     * Loads settings from file or remote URL.
     * @param location Location specifying a file name or a remote http(s) URL. If it identifies a file, it
     *                 must be an absolute path, a file in current directory, or a file in user's home directory.
     * @throws Exception If settings cannot be loaded, e.g. file not found or invalid URL.
     */
    public static void load(String location) throws Exception {
        if (location.substring(0, 7).equalsIgnoreCase("http://") ||
                location.substring(0, 8).equalsIgnoreCase("https://")) {
            loadFromRemoteURL(location);
        } else {
            // Assume location is absolute or a file is in current directory
            if (new File(location).exists()) {
                load(new FileInputStream(location));
            }
            // If not absolute or current directory, try user's home
            else if(new File(new File(System.getProperty("user.home")), location).exists()){
                load(new File(new File(System.getProperty("user.home")), location).getAbsolutePath());
            }
            else {
                throw new RuntimeException("Unable to locate settings file: " + location);
            }
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
