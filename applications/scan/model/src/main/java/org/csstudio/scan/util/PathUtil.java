/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.util;

/** Scan system path utils
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PathUtil
{
    /** @param path_spec Path elements joined by ","
     *  @return Separate path elements
     *  @throws Exception on parse error (missing end of quoted string)
     */
    public static String[] splitPath(final String path_spec) throws Exception
    {
        if (path_spec == null)
            return new String[0];
        return path_spec.split("\\s*,\\s*");

    }

    /** @param paths Path elements
     *  @return Path elements joined by ","
     */
    public static String joinPaths(final String[] paths)
    {
        final StringBuilder buf = new StringBuilder();
        for (String path : paths)
        {
            if (buf.length() > 0)
                buf.append(", ");
            buf.append(path);
        }
        return buf.toString();
    }
}
