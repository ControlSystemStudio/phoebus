/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.macros.Macros;

/** Information about a display
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayInfo
{
    private static final String UTF_8 = "UTF-8";
    private final String path, name;
    private final Macros macros;
    private final boolean resolve;

    /** Parse URL for a display
     *
     *  <p>Minimal example:
     *  <code>
     *  file://path/to/file.bob
     *  </code>
     *
     *  <p>Full example:
     *  <code>
     *  file://path/to/file.bob;MACRO1=value1;MACRO2=another%20value#Display%20Name
     *  </code>

     *  <p>Path of the URL is the file or http link to
     *  the display.
     *
     *  <p>Optional path parameters, added via ';' to the path,
     *  provide macros.
     *
     *  <p>An optional '#..' reference is used as the display name,
     *  which is otherwise derived from the file name,
     *  i.e. the last segment of the path.
     *
     *  @param url
     *  @return {@link DisplayInfo}
     */
    public static DisplayInfo forURL(final URL url)
    {
        if (url.getQuery() != null)
            logger.log(Level.WARNING, "Ignoring query component '" + url.getQuery() + "' in " + url);

        // Get basic file or http 'path' and 'macros' from the URL pathname and path parameters
        final String path_and_macros = url.getPath();
        final String path;
        final Macros macros = new Macros();
        int sep = path_and_macros.indexOf(';');
        if (sep > 0)
        {
            path = path_and_macros.substring(0, sep);
            for (String macro_spec : path_and_macros.substring(sep+1).split(";"))
            {
                final int mvsep = macro_spec.indexOf('=');
                if (mvsep > 0)
                {
                    final String name = macro_spec.substring(0, mvsep);
                    final String value = decode(macro_spec.substring(mvsep+1));
                    macros.add(name, value);
                }
            }
        }
        else
            path = path_and_macros;

        // Use either provided display name, or basename of path
        String name = url.getRef();
        if (name != null)
            name = decode(name);
        if (name == null)
            name = basename(path);

        return new DisplayInfo(path, name, macros, true);
    }

    /** Decode URL text
     *  @param text Text that may contain '+' or '%20' etc.
     *  @return Decoded text
     */
    private static String decode(final String text)
    {
        try
        {
            return URLDecoder.decode(text, UTF_8);
        }
        catch (UnsupportedEncodingException ex)
        {
            logger.log(Level.WARNING, "Cannot decode " + text);
        }
        return text;
    }

    /** Encode URL text
     *  @param text Text that may contain spaces etc. in UTF-8
     *  @return URL-encoded text
     */
    private static String encode(final String text)
    {
        try
        {
            return URLEncoder.encode(text, UTF_8);
        }
        catch (UnsupportedEncodingException ex)
        {
            logger.log(Level.WARNING, "Cannot encode " + text);
        }
        return text;
    }

    private static String basename(final String path)
    {
        int sep = path.lastIndexOf('/');
        if (sep < 0)
            sep = path.lastIndexOf('\\');
        if (sep < 0)
            return path;
        return path.substring(sep+1);
    }

    /** @param model DisplayModel with macros and USER_DATA_INPUT_FILE
     *  @return DisplayInfo
     */
    public static DisplayInfo forModel(final DisplayModel model)
    {
        return new DisplayInfo(model.getUserData(DisplayModel.USER_DATA_INPUT_FILE),
                model.getDisplayName(),
                model.propMacros().getValue(),
                false);
    }

    /** @param path Path to the display
     *  @param name Name to show
     *  @param macros Macros
     *  @param resolve Resolve the display, potentially using *.bob for a *.opi path?
     */
    public DisplayInfo(final String path, final String name, final Macros macros, final boolean resolve)
    {
        this.path = path;
        this.name = name != null ? name : basename(path);
        this.macros = Objects.requireNonNull(macros);
        this.resolve = resolve;
    }

    /** @return Path to the display file or http link */
    public String getPath()
    {
        return path;
    }

    /** @return Display name */
    public String getName()
    {
        return name;
    }

    /** @return Macros */
    public Macros getMacros()
    {
        return macros;
    }

    /** @return Resolve *.opi into *.bob? */
    public boolean shouldResolve()
    {
        return resolve;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + path.hashCode();
        result = prime * result + macros.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof DisplayInfo))
            return false;
        final DisplayInfo other = (DisplayInfo) obj;
        // Ignore the name
        // Displays match if they refer to the same path and macros
        return path.equals(other.path) &&
               macros.equals(other.macros);
    }

    /** @return URL representation of display info */
    public URL toURL()
    {
        final StringBuilder buf = new StringBuilder();

        // If no schema in path, use file:
        if (! (path.startsWith("file:")  ||
               path.startsWith("http:")  ||
               path.startsWith("https:") ||
               path.startsWith("ftp:")))
            buf.append("file:");

        // In path, keep ':' and '/', but replace spaces
        buf.append(path.replace(' ', '+'));

        // Add macros as path parameters
        for (String name : macros.getNames())
            buf.append(';')
               .append(name)
               .append('=')
               .append(encode(macros.getValue(name)));

        // Have display name (other than basename of path)?
        // Add as fragment aka reference
        if (name != null  &&  !basename(path).equals(name))
            buf.append('#')
               .append(encode(name));

        try
        {
            return new URL(buf.toString());
        }
        catch (MalformedURLException ex)
        {
            logger.log(Level.SEVERE, "Internal error in Display Info URL '" + buf.toString() + "'", ex);
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "Display '" + name + "', file " + path + ", macros " + macros + (resolve ? " (resolve)" : "");
    }
}
