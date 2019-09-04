/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.opva;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helper for analyzing PV Names
 *
 *  <p>Handles these types of names:
 *  <pre>
 *  pva://channel_name
 *  pva://channel_name?request=field(some.structure.element)
 *  pva://channel_name/some/structure.element
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVNameHelper
{
    final private static Pattern REQUEST_PATTERN = Pattern.compile("\\?request=field\\((.*)\\)");
    final private static int REQUEST_FIELD_START = "?request=".length();

    final private String channel, read, write;

    /** Create parser
     *
     *  @param pv_name PV name
     *  @return {@link PVNameHelper}
     *  @throws Exception on error
     */
    public static PVNameHelper forName(final String pv_name) throws Exception
    {
        // PV name that follow pvget/eget URL syntax can be
        // "pva:///the_name" with 3 '///' to allow for a "pva://host:port/the_name".
        // Strip the 3rd '/'
        final String name = pv_name.startsWith("/") ? pv_name.substring(1) : pv_name;
        // Does name include "?request.."?
        int pos = name.indexOf('?');
        if (pos >= 0)
            return PVNameHelper.forNameWithRequest(name.substring(0, pos), name.substring(pos));
        // Does name include "/some/path"?
        pos = name.indexOf('/');
        if (pos >= 0)
            return PVNameHelper.forNameWithPath(name.substring(0, pos), name.substring(pos+1));
        // Plain channel name
        return new PVNameHelper(name, "field()", "field(value)");
    }

    /** @param channel Channel name
     *  @param request "?request..."
     *  @return {@link PVNameHelper}
     *  @throws Exception on error
     */
    private static PVNameHelper forNameWithRequest(final String channel, final String request) throws Exception
    {
        final Matcher matcher = REQUEST_PATTERN.matcher(request);
        if (! matcher.matches())
            throw new Exception("Expect ?request=field(...) but got \"" + request + "\"");
        final String field = matcher.group(1);
        final String write = field.isEmpty()
            ? "field(value)"
            : "field(" + field + ".value)";
        return new PVNameHelper(channel, request.substring(REQUEST_FIELD_START), write);
    }

    /** @param channel Channel name
     *  @param path "to/some/element" (without initial '/')
     *  @return {@link PVNameHelper}
     *  @throws Exception on error
     */
    private static PVNameHelper forNameWithPath(final String channel, final String path) throws Exception
    {
        final String field = path.replace('/', '.');
        final String write = field.isEmpty()
                ? "field(value)"
                : "field(" + field + ".value)";
        return new PVNameHelper(channel, "field(" + field + ")",  write);
    }

    /** Private to enforce use of <code>forName</code> */
    private PVNameHelper(final String channel, final String read, final String write) throws Exception
    {
        if (channel.isEmpty())
            throw new Exception("Empty channel name");
        this.channel = channel;
        this.read = read;
        this.write = write;
    }

    /** @return Channel name */
    public String getChannel()
    {
        return channel;
    }

    /** @return Request "field(..)" for reading */
    public String getReadRequest()
    {
        return read;
    }

    /** @return Request "field(..)" for writing */
    public String getWriteRequest()
    {
        return write;
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return "Channel '" + channel +
                "', read request '" + read +
                "', write request '" + write + "'";
    }
}
