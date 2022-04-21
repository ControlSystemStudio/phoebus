/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;

import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.phoebus.framework.util.IOUtils;

/** Locate files along the EDM search path.
 *
 *  <p>When locating a web resource, it's downloaded into the `auto_converter_dir`.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AssetLocator
{
    private String prefix = "";

    /** @param prefix Name prefix (parent folder) */
    public void setPrefix(final String prefix)
    {
        this.prefix = prefix;
    }

    /** @param name Display name.
     *  @return Display name with '.edl' added when missing or when there is a different ending
     */
    public static String makeEdlEnding(final String name)
    {
        final int sep = name.lastIndexOf('.');
        if (sep < 0)
            return name + ".edl";
        return name.substring(0, sep) + ".edl";
    }

    /** @param name EDM display name. '.edl' will be added when missing
     *  @return File for that display or <code>null</code>
     *  @throws Exception on error
     */
    public File locateEdl(final String name) throws Exception
    {
        // Check EDM file search path for display_file
        return locate(makeEdlEnding(name));
    }

    /** @param name EDM resource
     *  @return File for that resource or <code>null</code>
     *  @throws Exception on error
     */
    public File locate(final String name) throws Exception
    {
        // Check relative to parent
        final String prefixed = prefix + (prefix.endsWith("/") ? name : "/" + name);
        File result = doLocate(prefixed);
        if (result != null)
            return result;
        // Fall back to plain name
        return doLocate(name);
    }

    private File doLocate(final String name) throws Exception
    {
        for (String path : ConverterPreferences.paths)
        {
            final String check = path + (path.endsWith("/") ? name : "/" + name);
            logger.log(Level.FINE, "Check " + check);
            if (check.startsWith("http"))
            {
                final InputStream stream;
                try
                {
                    stream = ModelResourceUtil.openURL(check);
                }
                catch (Exception ex)
                {
                    // Check next search path entry
                    continue;
                }
                try
                {
                    if (ConverterPreferences.auto_converter_dir == null)
                        throw new Exception("Cannot download " + check + ", no auto_converter_dir");
                    final File input = new File(ConverterPreferences.auto_converter_dir, name);
                    final File output_folder = input.getParentFile();
                    if (! output_folder.exists())
                    {
                        logger.log(Level.INFO, "Creating folder " + output_folder);
                        if (! output_folder.mkdirs())
                            throw new Exception("Cannot create folder " + output_folder + " to download " + check);
                    }
                    logger.log(Level.INFO, "Downloading " + check + " into " + input);
                    IOUtils.copy(stream, new FileOutputStream(input));
                    return input;
                }
                catch (Exception ex)
                {
                    logger.log(Level.INFO, "Cannot download " + check, ex);
                }
            }
            else
            {
                final File check_file = new File(check);
                if (check_file.canRead())
                    return check_file;
            }
        }
        return null;
    }
}
