/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.preferences;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.prefs.Preferences;

/** Write preferences in property file format
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PropertyPreferenceWriter
{
    /** Save preferences in property file format
     *
     *  <p>Properties have the name "package/setting",
     *  where the package name in dot notation
     *  is used to locate the preference node.
     *
     *  <p>Value is the corresponding preference setting.
     *
     *  @param stream Stream for property file
     *  @throws Exception on error
     */
    public static void save(final OutputStream stream) throws Exception
    {
        try
        (
            final OutputStreamWriter out = new OutputStreamWriter(stream);
        )
        {
            out.append("# Preference settings\n");
            out.append("# Format:\n");
            out.append("# the.package.name/key=value\n");
            listSettings(out, Preferences.userRoot());
            out.append("# End.\n");
            out.flush();
        }
    }

    private static void listSettings(final Writer out, final Preferences node) throws Exception
    {
        for (String key : node.keys())
            formatSetting(out, node, key);
        for (String child : node.childrenNames())
            listSettings(out, node.node(child));
    }

    private static void formatSetting(final Writer out, final Preferences node, final String key) throws Exception
    {
        final String path = node.absolutePath();
        out.append(path.substring(1).replace('/', '.'))
           .append('/')
           .append(key)
           .append('=')
           .append(node.get(key, ""))
           .append('\n');
    }
}
