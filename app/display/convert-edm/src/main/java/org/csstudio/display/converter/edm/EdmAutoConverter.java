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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.spi.DisplayAutoConverter;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.opibuilder.converter.model.EdmModel;
import org.phoebus.framework.util.IOUtils;

/** Phoebus application for EDM converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EdmAutoConverter implements DisplayAutoConverter
{
    private static final ConcurrentHashMap<String, Semaphore> active_conversions = new ConcurrentHashMap<>();

    @Override
    public DisplayModel autoconvert(final String parent_display, final String display_file) throws Exception
    {
        // Is auto-converter enabled?
        if (ConverterPreferences.auto_converter_dir == null)
            return null;

        logger.log(Level.INFO, "For parent display " + parent_display + ", can " + display_file + " be auto-created from EDM file?");

        // Converter could be called in parallel for the same display_file
        // when a large display embeds the same content multiple times.
        // To prevent one thread clobbering the files downloaded and converted by the other,
        // exactly one thread will create the semaphore.
        final Semaphore active = active_conversions.computeIfAbsent(display_file, file -> new Semaphore(1));
        // One of them (not necessarily the one that created it) will then acquire it first and perform the conversion.
        // Others wait until the conversion completes, and then they'll find the already converted file.
        active.acquire();
        try
        {
            return doConvert(parent_display, display_file);
        }
        finally
        {
            active.release();
        }
    }

    private DisplayModel doConvert(final String parent_display, final String display_file) throws Exception
    {
        // Check if we already have an auto-converted *.bob file, so use that instead of re-creating it
        final File already_converted = new File(ConverterPreferences.auto_converter_dir, display_file);
        if (already_converted.canRead())
        {
            logger.log(Level.INFO, "Found previously converted file " + already_converted);
            return ModelLoader.loadModel(already_converted.getPath());
        }

        // Check EDM file search path for display_file
        String edl_file = display_file;
        final int sep = edl_file.lastIndexOf('.');
        if (sep < 0)
            edl_file = edl_file + ".edl";
        else
            edl_file = edl_file.substring(0, sep) + ".edl";

        File input = null;
        for (String path : ConverterPreferences.paths)
        {
            String check = path + (path.endsWith("/") ? edl_file : "/" + edl_file);
            logger.log(Level.FINE, "Check " + check);
            if (check.startsWith("http"))
            {
                try
                {
                    final InputStream stream = ModelResourceUtil.openURL(check);
                    input = new File(ConverterPreferences.auto_converter_dir, edl_file);
                    logger.log(Level.INFO, "Downloading " + check + " into " + input);
                    IOUtils.copy(stream, new FileOutputStream(input));
                    break;
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
                {
                    input = check_file;
                    break;
                }
            }
        }
        if (input == null)
            return null;

        // Determine output file name in auto_converter_dir
        final File output = new File(ConverterPreferences.auto_converter_dir, new File(display_file).getName());

        EdmModel.reloadEdmColorFile(ConverterPreferences.colors_list, new FileInputStream(ConverterPreferences.colors_list));
        new Converter(input, output);

        // Return DisplayModel of the converted file
        return ModelLoader.loadModel(output.getPath());
    }
}
