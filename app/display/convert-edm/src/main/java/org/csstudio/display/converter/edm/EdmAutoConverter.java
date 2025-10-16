/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.spi.DisplayAutoConverter;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.opibuilder.converter.model.EdmModel;

/** EDM auto-converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EdmAutoConverter implements DisplayAutoConverter
{
    private static final ConcurrentHashMap<String, Semaphore> active_conversions = new ConcurrentHashMap<>();

    @Override
    public DisplayModel autoconvert(final String parent_display, String display_file) throws Exception
    {
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
            // Since June 2021, EDM supports relative paths.
            if (parent_display != null)
            {
                final File parent_file = new File(parent_display);
                if (parent_file.canRead())
                {
                    // Check for EDM file relative to parent
                    final File input = new File(parent_file.getParentFile(), AssetLocator.makeEdlEnding(display_file));
                    if (input.canRead())
                    {
                        final File output = new File(parent_file.getParentFile(), display_file);
                        if (! output.exists())
                        {
                            if (! output.getParentFile().canWrite())
                            {
                                logger.log(Level.WARNING, "Lacking write permission to auto-convert " + input + " to " + output);
                                return null;
                            }
                            logger.log(Level.INFO, "Auto-converting " + input + " to " + output);
                            new EdmConverter(input, null).write(output);
                        }
                        return ModelLoader.loadModel(output.getPath());
                    }
                }
            }

            // Is auto-converter for search path, writing into auto_converter_dir, enabled?
            if (ConverterPreferences.auto_converter_dir == null)
                return null;

            // Special case:
            // Parent display is null, and display_file is the complete path
            // to a file within the auto_converter_dir.
            // This can for example happen when opening an auto-converted file
            // from the alarm display.
            if (parent_display == null)
            {
                final File the_file = new File(display_file);
                if (ConverterPreferences.auto_converter_dir.equals(the_file.getParentFile()))
                {   // Use just the file name. doConvert() will add the auto_converter_dir
                    display_file = the_file.getName();
                }
            }
            logger.log(Level.INFO, "For parent display " + parent_display + ", can " + display_file + " be auto-created from EDM file?");
            if (parent_display != null)
            {   // Since June 2021, EDM supports relative paths.
                final String relative = new File(new File(parent_display).getParentFile(), display_file).getAbsolutePath();
                final DisplayModel converted = doConvert(relative);
                if (converted != null)
                    return converted;
            }
            return doConvert(display_file);
        }
        finally
        {
            active.release();
        }
    }

    private DisplayModel doConvert(final String display_path) throws Exception
    {
        String display_file = display_path;
        // Strip either complete path or specific prefix
        if (ConverterPreferences.auto_converter_strip.isBlank())
        {
            int sep = display_file.lastIndexOf('/');
            if (sep < 0)
                sep = display_file.lastIndexOf('\\');
            if (sep >= 0)
                display_file = display_file.substring(sep + 1);
        }
        else
        {
            if (display_file.startsWith(ConverterPreferences.auto_converter_strip))
                display_file = display_file.substring(ConverterPreferences.auto_converter_strip.length());
        }

        // Check if we already have an auto-converted *.bob file, so use that instead of re-creating it
        final File already_converted = new File(ConverterPreferences.auto_converter_dir, display_file);
        if (already_converted.canRead())
        {
            logger.log(Level.INFO, "Found previously converted file " + already_converted);
            return ModelLoader.loadModel(already_converted.getPath());
        }

        // Check EDM file search path for display_file
        final AssetLocator locator = new AssetLocator();
        final File input = locator.locateEdl(display_file);
        if (input == null)
            return null;

        // Determine output file name in auto_converter_dir
        final File output = new File(ConverterPreferences.auto_converter_dir, display_file);

        // Load colors
        EdmModel.reloadEdmColorFile(ConverterPreferences.colors_list,
                                    ModelResourceUtil.openResourceStream(ConverterPreferences.colors_list));

        // Convert EDM input
        logger.log(Level.INFO, "Converting " + input + " into " + output);
        locator.setPrefix(new File(display_file).getParent());
        final EdmConverter converter = new EdmConverter(input, locator);

        // Save EDM file to potentially new folder
        Files.createDirectories(output.getParentFile().toPath());
        converter.write(output);

        // Return DisplayModel of the converted file
        return ModelLoader.loadModel(output.getPath());
    }
}
