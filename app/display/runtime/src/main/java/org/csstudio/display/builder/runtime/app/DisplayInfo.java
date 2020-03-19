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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.util.ResourceParser;

/** Information about a display
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayInfo
{
    private static final String UTF_8 = "UTF-8";
    private final String path;
    private final String name;
    private final Macros macros;
    private final boolean resolve;

    /** Parse URI for a display
     *
     *  <p>Minimal example:
     *  <code>
     *  file://path/to/file.bob
     *  </code>
     *
     *  <p>Full example:
     *  <code>
     *  file://path/to/file.bob?MACRO1=value1&MACRO2=another%20value
     *  </code>

     *  <p>Path of the URI is the file or http link to
     *  the display.
     *
     *  <p>Optional query parameters, added via '?' to the path,
     *  and then separated by '&', provide macros.
     *
     *  @param uri
     *  @return {@link DisplayInfo}
     */
    public static DisplayInfo forURI(final URI uri)
    {
        // Get basic file or http 'path' from path
        final String path;
        if (uri.getScheme() == null  ||  uri.getScheme().equals("file"))
            path = uri.getRawPath();
        else
        {
            final StringBuilder buf = new StringBuilder();
            buf.append(uri.getScheme()).append(':');
            if (uri.getHost() != null)
                buf.append("//").append(uri.getHost());
            if (uri.getPort() >= 0)
                buf.append(':').append(uri.getPort());
            buf.append(uri.getPath());
            path = buf.toString();
        }

        // Check query for macros
        final Macros macros = new Macros();
        ResourceParser.getQueryItemStream(uri)
                      .filter(item -> item.getValue() != null)
                      .forEach(item -> macros.add(item.getKey(),
                                                  item.getValue()));

        return new DisplayInfo(path, null, macros, true);
    }

    /** Encode URI text
     *  @param text Text that may contain spaces etc. in UTF-8
     *  @return URI-encoded text
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
     *  @param name Display name or <code>null</code> to use basename of path
     *  @param macros Macros
     *  @param resolve Resolve the display, potentially using *.bob for a *.opi path?
     */
    public DisplayInfo(final String path, final String name, final Macros macros, final boolean resolve)
    {
        this.path = path;
        if (name == null  ||  name.isEmpty())
            this.name = basename(path);
        else
            this.name = name;
        this.macros = Objects.requireNonNull(macros);
        this.resolve = resolve;
    }

    /** @return Path to the display file or http link */
    public String getPath()
    {
        return path;
    }

    /** @return Display name to show to user */
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
        // Displays match if they refer to the same path and macros,
        // regardless of 'name'
        return path.equals(other.path) &&
               macros.equals(other.macros);
    }

    /** @return URI representation of display info */
    public URI toURI()
    {
        final StringBuilder buf = new StringBuilder();

        // If no schema in path, use file:
        if (! (path.startsWith("file:")  ||
               path.startsWith("http:")  ||
               path.startsWith("https:") ||
               path.startsWith("ftp:")   ||
               path.startsWith("examples:")))
        {
            buf.append("file:");
            // Windows platform tweak
            if (path.contains(":/"))
                buf.append("//");
            else if (path.contains(":"))
                buf.append("///");
        }

        // In path, keep ':' and '/', but replace spaces
        // Windows platform tweak replace \ with /
        buf.append(path.replace(" ", "%20").replace('\\', '/'));

        // Add macros as path parameters
        boolean first = true;
        for (String name : macros.getNames())
        {
            if (first)
            {
                buf.append('?');
                first = false;
            }
            else
                buf.append('&');
            buf.append(name)
               .append('=')
               .append(encode(macros.getValue(name)));
        }

        try
        {
            return new URI(buf.toString());
        }
        catch (URISyntaxException ex)
        {
            logger.log(Level.SEVERE, "Internal error in Display Info URI '" + buf.toString() + "'", ex);
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "Display '" + name + "' " + path + ", macros " + macros + (resolve ? " (resolve)" : "");
    }
}
