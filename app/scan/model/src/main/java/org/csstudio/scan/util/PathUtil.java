/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.util;

import java.util.List;
import java.util.stream.Collectors;

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
    public static List<String> splitPath(final String path_spec) throws Exception
    {
        if (path_spec == null)
            return List.of();
        return List.of(path_spec.split("\\s*,\\s*"));
    }

    /** @param paths Path elements
     *  @return Path elements joined by ","
     */
    public static String joinPaths(final List<String> paths)
    {
        return paths.stream().collect(Collectors.joining(", "));
    }
}
