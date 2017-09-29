/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.preferences;

import java.io.InputStream;
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
}
