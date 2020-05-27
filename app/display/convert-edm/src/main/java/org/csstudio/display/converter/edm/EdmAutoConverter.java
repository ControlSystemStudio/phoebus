/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.spi.DisplayAutoConverter;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.opibuilder.converter.model.EdmModel;

/** Phoebus application for EDM converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EdmAutoConverter implements DisplayAutoConverter
{
    private static final ConcurrentHashMap<String, Semaphore> active_conversions = new ConcurrentHashMap<>();

    @Override
    public DisplayModel autoconvert(final String parent_display, String display_file) throws Exception
    {
        // Is auto-converter enabled?
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
        // Assert that the target directory exists
        ConverterPreferences.auto_converter_dir.mkdirs();

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
        final File output = new File(ConverterPreferences.auto_converter_dir, new File(display_file).getName());

        // Load colors
        EdmModel.reloadEdmColorFile(ConverterPreferences.colors_list,
                                    ModelResourceUtil.openResourceStream(ConverterPreferences.colors_list));

        // Convert EDM input
        new EdmConverter(input, locator).write(output);

        // Return DisplayModel of the converted file
        return ModelLoader.loadModel(output.getPath());
    }
}
