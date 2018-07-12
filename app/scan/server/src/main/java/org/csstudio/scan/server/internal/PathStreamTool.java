/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.internal;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.csstudio.scan.server.ScanServerInstance;

/** Tool for opening path as stream
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PathStreamTool
{
    /** Prefix used for built-in scan server examples */
    public static final String EXAMPLES = "examples:";

    /** @param path "examples:/.." path for built-in scan server examples
     *  @return Path to actual resource
     */
    public static String patchExamplePath(final String path)
    {
        return "/examples/" + path.substring(9);
    }

    /** Open stream for path
     *  @param path Either "examples:file.ext" or plain file path
     *  @return InputStream for the path
     *  @throws Exception
     */
    public static InputStream openStream(final String path) throws Exception
    {
        // Handle examples
        if (path.startsWith(EXAMPLES))
            return ScanServerInstance.class.getResourceAsStream(patchExamplePath(path));
        else
            return new FileInputStream(path);
    }

    /** Open stream for a location, using multiple search paths
     *  @param paths Search paths, may start with "platform:/plugin/some.plugin.name/path/file.ext" or plain file path
     *  @param filename File name
     *  @return InputStream for the file
     *  @throws Exception
     */
    public static InputStream openStream(final List<String> paths, final String filename) throws Exception
    {
        try
        {
            return openStream(filename);
        }
        catch (Exception ex)
        {
            // Ignore, try search path
        }
        for (String path : paths)
        {
            try
            {
                if (path.endsWith("/"))
                    return openStream(path + filename);
                else
                    return openStream(path + "/" + filename);

            }
            catch (Exception ex)
            {
                // Ignore, try next search path element
            }
        }
        throw new Exception("Cannot open " + filename + ", paths: " + paths);
    }
}
