/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.viewer3d;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preferences class for the org.phoebus.app.viewer3d package.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class Preferences
{
    @Preference public static int read_timeout;
    @Preference public static String default_dir;
    @Preference public static int cone_faces;

    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/3d_viewer_preferences.properties");
    }
}
