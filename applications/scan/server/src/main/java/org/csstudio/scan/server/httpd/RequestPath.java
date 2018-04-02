/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.httpd;

import javax.servlet.http.HttpServletRequest;

/** Helper for analyzing the request path
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RequestPath
{
    final private String[] elements;

    /** Initialize
     *  @param request {@link HttpServletRequest}
     */
    public RequestPath(final HttpServletRequest request)
    {
        final String path = request.getPathInfo();
        if (path == null)
            elements = new String[0];
        else
        {
            if (! path.startsWith("/"))
                throw new Error("Path does not start with '/'");
            elements = path.substring(1).split("/");
        }
    }

    /** @return Number of request path elements */
    public int size()
    {
        return elements.length;
    }

    /** @param index Path element index
     *  @return Path element as String
     */
    public String getString(final int index)
    {
        return elements[index];
    }

    /** @param index Path element index
     *  @return Path element as number
     */
    public long getLong(final int index) throws Exception
    {
        try
        {
            return Long.parseLong(getString(index));
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse path element '" + getString(index) + "'");
        }
    }
}
