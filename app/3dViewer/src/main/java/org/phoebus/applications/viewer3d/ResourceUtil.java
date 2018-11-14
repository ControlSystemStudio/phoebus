/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
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
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.phoebus.framework.util.IOUtils;

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
    private static boolean trusting_anybody = false;

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
        if (resource_name.startsWith("https"))
            trustAnybody();

        final URL url = new URL(resource_name);
        final URLConnection connection = url.openConnection();
        connection.setReadTimeout(timeout_ms);
        return connection.getInputStream();
    }

    /** Allow https:// access to self-signed certificates
     *  @throws Exception on error
     */
    // From Eric Berryman's code in org.csstudio.opibuilder.util.ResourceUtil.
    private static synchronized void trustAnybody() throws Exception
    {
        if (trusting_anybody)
            return;

        // Create a trust manager that does not validate certificate chains.
        final TrustManager[] trustAllCerts = new TrustManager[]
        {
            new X509TrustManager()
            {
                @Override
                public void checkClientTrusted(X509Certificate[] arg0,
                                               String arg1) throws CertificateException
                { /* NOP */ }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0,
                                               String arg1) throws CertificateException
                { /* NOP */ }

                @Override
                public X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }
            }
        };
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // All-trusting host name verifier
        final HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        trusting_anybody = true;
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
