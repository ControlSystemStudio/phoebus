/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.update;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helper for path modifications
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PathWrangler
{
    private final List<Pattern> removals = new ArrayList<>();

    /** @param removals Comma-separated list of regular expressions.
     *                  Path section that matches an expression is removed.
     *                  Patterns are applied in the listed order.
     */
    public PathWrangler(final String removals)
    {
        for (String remove : removals.split(","))
            this.removals.add(Pattern.compile(remove));
    }

    /** Apply the removals to path
     *
     *  @param path Original path
     *  @return Path where all matching section have been removed
     */
    public String wrangle(String path)
    {
        for (Pattern remove : removals)
        {
            final Matcher matcher = remove.matcher(path);
            if (matcher.find())
                path = matcher.replaceAll("");
        }
        return path;
    }
}
