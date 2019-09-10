/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.opibuilder.converter.model.EdmDisplay;
import org.csstudio.opibuilder.converter.model.EdmModel;
import org.csstudio.opibuilder.converter.parser.EdmDisplayParser;
import org.phoebus.framework.workbench.FileHelper;

/** EDM Converter
 *
 *  <p>Can be called as 'Main',
 *  also used by converter app.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Converter
{
    /** Logger for all the Display Builder generating code */
    public static final Logger logger = Logger.getLogger(Converter.class.getPackageName());
    private Collection<String> linked_displays;

    public Converter(final File input, final File output) throws Exception
    {
        logger.log(Level.INFO, "Convert " + input + " -> " + output);
        final EdmDisplayParser parser = new EdmDisplayParser(input.getPath(), new FileInputStream(input));
        final EdmDisplay edm = new EdmDisplay(parser.getRoot());

        final String title = input.getName()
                                  .replace(".edl", "")
                                  .replace('_', ' ');
        final EdmConverter converter = new EdmConverter(title, edm);
        final ModelWriter writer = new ModelWriter(new FileOutputStream(output));
        writer.writeModel(converter.getDisplayModel());
        writer.close();
        linked_displays = converter.getLinkedDisplays();
        for (String linked : linked_displays)
            logger.log(Level.INFO, "Linked display: " + linked);

    }

    /** @return Displays that were linked from this display */
    public Collection<String> getLinkedDisplays()
    {
        return linked_displays;
    }

    /** @param infile Input file (*.opi, older *.bob)
     *  @param force Overwrite existing file?
     *  @param output_dir Folder where to create output.bob, <code>null</code> to use folder of input file
     *  @throws Exception on error
     */
    private static void convert(final String input, final boolean force, final File output_dir) throws Exception
    {
        final File infile = new File(input);
        if (! infile.canRead())
            throw new Exception("Cannot read " + infile);

        if (infile.isDirectory())
        {
            logger.log(Level.INFO, "Converting all files in directory " + infile);
            for (File file : infile.listFiles())
                convert(file.getAbsolutePath(), force, output_dir);
            return;
        }

        // Convert *.edl file
        // Copy other file types, which could be *.gif etc.
        if (! input.endsWith(".edl"))
        {
            if (output_dir != null)
            {
                final File existing = new File(output_dir, new File(input).getName());
                if (existing.exists()  &&  force)
                {
                    logger.log(Level.INFO, "Deleting existing " + existing);
                    FileHelper.delete(existing);
                }
                logger.log(Level.INFO, "Copying file " + input + " into " + output_dir);
                FileHelper.copy(new File(input), output_dir);
                return;
            }
        }
        else
        {
            File outfile = new File(input.substring(0, input.length()-4) + ".bob");

            if (output_dir != null)
                outfile = new File(output_dir, outfile.getName());
            if (outfile.canRead())
            {
                if (force)
                {
                    logger.log(Level.INFO, "Deleting existing " + outfile);
                    FileHelper.delete(outfile);
                }
                else
                    throw new Exception("Output file " + outfile + " exists");
            }

            new Converter(infile, outfile);
        }
    }

    public static void main(final String[] args)
    {
        final List<String> files = new ArrayList<>(List.of(args));
        ConverterPreferences.colors_list = "colors.list";
        File output_dir = null;
        boolean force = false;
        if (files.isEmpty())
            files.add("-h");
        while (files.size() > 0  &&  files.get(0).startsWith("-"))
        {
            if (files.get(0).startsWith("-h"))
            {
                System.out.println("Usage: -main org.csstudio.display.converter.edm.Converter [options] <files>");
                System.out.println();
                System.out.println("Converts EDM *.edl files to Display Builder *.bob format.");
                System.out.println();
                System.out.println("Files to convert may be individual files, or folder names,");
                System.out.println("in which case the complete folder is converted.");
                System.out.println();
                System.out.println("Output files are created where the input file was found,");
                System.out.println("unless a designated output folder is specified.");
                System.out.println();
                System.out.println("Options:");
                System.out.println("-help                        - Help");
                System.out.println("-colors /path/to/colors.list - EDM colors.list file to use");
                System.out.println("-output /path/to/folder      - Folder into which converted files are written");
                System.out.println("-force                       - Overwrite existing files instead of stopping");
                return;
            }
            else if (files.get(0).startsWith("-c"))
            {
                if (files.size() < 2)
                {
                    System.err.println("Missing file for -colors /path/to/colors.list");
                    return;
                }
                ConverterPreferences.colors_list = files.get(1);
                files.remove(0);
                files.remove(0);
            }
            else if (files.get(0).startsWith("-o"))
            {
                if (files.size() < 2)
                {
                    System.err.println("Missing folder for -output /path/to/folder");
                    return;
                }
                output_dir = new File(files.get(1));
                files.remove(0);
                files.remove(0);
            }
            else if (files.get(0).startsWith("-f"))
            {
                force = true;
                files.remove(0);
            }
        }

        try
        {
            EdmModel.reloadEdmColorFile(ConverterPreferences.colors_list, new FileInputStream(ConverterPreferences.colors_list));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot parse color file " + ConverterPreferences.colors_list, ex);
            return;
        }

        for (String file : files)
        {
            try
            {
                convert(file, force, output_dir);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot convert " + file, ex);
            }
        }
    }
}
