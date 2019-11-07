/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.viewer3d;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preferences class for the org.phoebus.app.viewer3d package.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static String READ_TIMEOUT = "read_timeout";
    public static String DEFAULT_DIR = "default_dir";
    public static String CONE_FACES = "cone_faces";

    public final static int read_timeout;
    public final static String default_dir;
    public final static int cone_faces;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/3d_viewer_preferences.properties");

        read_timeout = prefs.getInt(READ_TIMEOUT);
        default_dir = prefs.get(DEFAULT_DIR);
        cone_faces = prefs.getInt(CONE_FACES);
    }
}
