/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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
    /** @param name EDM display name. '.edl' will be added when missing
     *  @return File for that display or <code>null</code>
     *  @throws Exception on error
     */
    public File locateEdl(final String name) throws Exception
    {
        // Check EDM file search path for display_file
        String edl_file = name;
        final int sep = edl_file.lastIndexOf('.');
        if (sep < 0)
            edl_file = edl_file + ".edl";
        else
            edl_file = edl_file.substring(0, sep) + ".edl";
        return locate(edl_file);
    }

    /** @param name EDM resource
     *  @return File for that resource or <code>null</code>
     *  @throws Exception on error
     */
    public File locate(final String name) throws Exception
    {
        for (String path : ConverterPreferences.paths)
        {
            final String check = path + (path.endsWith("/") ? name : "/" + name);
            logger.log(Level.FINE, "Check " + check);
            if (check.startsWith("http"))
            {
                try
                {
                    final InputStream stream = ModelResourceUtil.openURL(check);
                    if (ConverterPreferences.auto_converter_dir == null)
                        throw new Exception("Cannot download " + check + ", no auto_converter_dir");
                    if (! ConverterPreferences.auto_converter_dir.isDirectory())
                        throw new Exception("Cannot download " + check + ", missing auto_converter_dir " + ConverterPreferences.auto_converter_dir);
                    final File input = new File(ConverterPreferences.auto_converter_dir, name);
                    logger.log(Level.INFO, "Downloading " + check + " into " + input);
                    IOUtils.copy(stream, new FileOutputStream(input));
                    return input;
                }
                catch (Exception ex)
                {
                    // Check next search path entry
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
