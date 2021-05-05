/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.viewer3d;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.phoebus.framework.util.IOUtils;
import org.phoebus.framework.util.ResourceParser;

/** Resource utility class for the Viewer3dPane.
 *
 *  <p> Based on link org.csstudio.display.builder.model.util.ModelResourceUtil
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class ResourceUtil
{
    /** Schema used for the built-in display examples */
    public static final String EXAMPLES_SCHEMA = "examples";

    private static int timeout_ms = Preferences.read_timeout;

    /** Open a file, web location, ..
    *
    *  <p>In addition, understands "examples:"
    *  to load a resource from the built-in examples.
    *
    *  @param resource_name Path to file, "examples:", "http:/.."
    *  @return {@link InputStream}
    *  @throws Exception on error
    */
    public static InputStream openResource(final String resource_name) throws Exception
    {
        if (null == resource_name)
            return null;

        if (resource_name.startsWith("http") ||  resource_name.startsWith("file:"))
            return openURL(resource_name);

        final URL example = getExampleURL(resource_name);
        if (example != null)
        {
            try
            {
                return example.openStream();
            }
            catch (Exception ex)
            {
                throw new Exception("Cannot open example: '" + example + "'", ex);
            }
        }

        return new FileInputStream(resource_name);
    }

    /** Open URL for "http", "https", "ftp", ..
     *  @param resource_name URL specification
     *  @return {@link InputStream}
     *  @throws Exception on error
     */
    private static InputStream openURL(String resource_name) throws Exception
    {
        final byte[] content = readURL(resource_name);
        return new ByteArrayInputStream(content);
    }

    private static byte[] readURL(final String url) throws Exception
    {
        final InputStream in = openURL(url, timeout_ms);
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        IOUtils.copy(in, buf);
        return buf.toByteArray();
    }

    /** Open URL for "http", "https", "ftp", ..
     *  @param resource_name URL specification
     *  @param timeout_ms Read timeout [milliseconds]
     *  @return {@link InputStream}
     *  @throws Exception on error
     */
    private static InputStream openURL(final String resource_name, final int timeout_ms) throws Exception
    {
        return ResourceParser.getContent(new URL(resource_name).toURI(), timeout_ms);
    }

    /** Check for "examples:.."
     *
     *  @param resource_name Path to file that may be based on "examples:.."
     *  @return URL for the example, or <code>null</code>
     */
    private static URL getExampleURL(final String resource_name)
    {
        if (resource_name.startsWith(EXAMPLES_SCHEMA + ":"))
        {
            String example = resource_name.substring(9);
            if (example.startsWith("/"))
                example = "/3d_viewer_examples" + example;
            else
                example = "/3d_viewer_examples/" + example;
            return Viewer3dPane.class.getResource(example);
        }
        return null;
    }
}
