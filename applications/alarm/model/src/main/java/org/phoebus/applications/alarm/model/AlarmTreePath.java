/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.util.ArrayList;
import java.util.List;

/** Helper for handling the path names of alarm tree elements.
 *  Path looks like "/root/area/system/subsystem/pv_name".
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTreePath
{
    /** Separator used to create path names to items in the alarm tree */
    final public static String PATH_SEP = "/";

    /** @param path String to test
     *  @return <code>true</code> if a string is a path or just a plain name
     */
    public static boolean isPath(final String path)
    {
        return path.startsWith(PATH_SEP);
    }

    /** Build path name
     *  @param path Parent path or <code>null</code> when starting at root
     *  @param item Name of item at end of path
     *  @return Full path name to item
     */
    public static String makePath(final String path, String item)
    {
        final StringBuilder result = new StringBuilder();
        if (path != null)
        {
            if (! isPath(path))
                result.append(PATH_SEP);
            // Skip path it it's only '/'
            if (!PATH_SEP.equals(path))
                result.append(path);
        }
        result.append(PATH_SEP);
        if (item != null  &&  !item.isEmpty())
        {
            // If item already starts with '/', skip it
            if (item.startsWith(PATH_SEP))
                item = item.substring(1);
            // Escape any path-seps inside item with backslashes
            result.append(item.replace(PATH_SEP, "\\/"));
        }
        return result.toString();
    }

    /** Build path name
     *  @param path_items Path elements
     *  @param length_to_use How many elements of path_items to use,
     *                       0 ... path_items.length-1
     *  @return Full path name to item
     */
    public static String makePath(final String path_items[], int length_to_use)
    {
        final int N = Math.min(path_items.length, length_to_use);
        final StringBuilder path = new StringBuilder();
        for (int i=0; i<N; ++i)
        {
            path.append(PATH_SEP);
            // Escape any path-seps inside item with backslashes
            path.append(path_items[i].replace(PATH_SEP, "\\/"));
        }
        return path.toString();
    }

    /** Split full path into pieces
     *  @param path Full path to an item
     *  @return Path elements
     */
    public static String[] splitPath(final String path)
    {
        // Split on '/', but only those that are NOT preceded by '\'.
        // '(?<!x)' means 'not preceded by x',
        // and in this case the x=\ must be escaped twice:
        // Once to get into the Java string, once more to pass to the regex.
        // Also skip the initial '/'
        final List<String> items = new ArrayList<>();
        for (String item : path.split("(?<!\\\\)/+"))
        {
            // Skip empty items
            if (item.isEmpty())
                continue;
            // Un-escape any PATH_SEP that's inside each item
            items.add(item.replace("\\/", PATH_SEP));
        }
        return items.toArray(new String[items.size()]);
    }

    /** Get last path element
     *  @param path Full path to an item
     *  @return Name, i.e. last path element
     */
    public static String getName(final String path)
    {
        final String elements[] = splitPath(path);
        return elements[elements.length-1];
    }

    /** Determine modified path
     *  @param path Original path
     *  @param modifier Path modifier: "segments/to/add", "/absolute/new/path", ".."
     *  @return Path based on pwd and modifier
     */
    public static String update(String path, String modifier)
    {
        if (modifier == null  ||  modifier.isEmpty())
            return makePath(null, path);
        // New complete path "/..."?
        if (modifier.startsWith(AlarmTreePath.PATH_SEP))
            return modifier;
        else
        {
            if ("..".equals(modifier))
            {   // Go one level 'up'
                final String[] elements = AlarmTreePath.splitPath(path);
                if (elements.length <= 0)
                    return AlarmTreePath.PATH_SEP;
                return AlarmTreePath.makePath(elements, elements.length-1);
            }
            else // Append to pwd
            {
                for (String element : AlarmTreePath.splitPath(modifier))
                    path = AlarmTreePath.makePath(path, element);
                return path;
            }
        }
    }
}
