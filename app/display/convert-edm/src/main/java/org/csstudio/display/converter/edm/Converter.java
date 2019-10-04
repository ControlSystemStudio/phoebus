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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.opibuilder.converter.model.EdmModel;
import org.phoebus.framework.workbench.FileHelper;

/** EDM Converter 'Main' for command-line invocation
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Converter
{
    /** Logger for all the Display Builder generating code */
    public static final Logger logger = Logger.getLogger(Converter.class.getPackageName());

    /** Convert one file or directory
     *  @param infile Input file (*.opi, older *.bob)
     *  @param paths Search paths, may be empty
     *  @param force Overwrite existing file?
     *  @param depth 1: Just this file  2: Also referenced files  3: .. another level down
     *  @param output_dir Folder where to create output.bob, <code>null</code> to use folder of input file
     *  @throws Exception on error
     */
    private static void convert(final String input, final List<String> paths, final boolean force, final int depth, final File output_dir) throws Exception
    {
        if (depth <= 0)
            return;

        // Is input a folder?
        final File check_input = new File(input);
        if (check_input.isDirectory())
        {
            logger.log(Level.INFO, "Converting all files in directory " + check_input);
            final File[] files = check_input.listFiles();
            Arrays.sort(files, (a, b) -> a.getPath().compareTo(b.getPath()));
            for (File file : files)
                convert(file.getAbsolutePath(), paths, force, depth, output_dir);
            return;
        }

        // Is input referring to exiting file?
        File infile = null;
        if (check_input.canRead())
            infile = check_input;
        else if (! paths.isEmpty())
        {
            final AssetLocator locator = new AssetLocator();
            infile = locator.locate(input);
        }

        if (infile == null)
            throw new Exception("Cannot locate " + input);
        if (! infile.canRead())
            throw new Exception("Cannot read " + infile);

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

            logger.log(Level.INFO, "Convert " + infile + " -> " + outfile);
            final EdmConverter converter = new EdmConverter(infile, null);
            converter.write(outfile);
            final int next = depth - 1;
            if (next > 0)
                for (String included : converter.getLinkedDisplays())
                {
                    try
                    {
                        convert(included.replace(".bob", ".edl"), paths, force, next, output_dir);
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Cannot convert included display '" + included + "'", ex);
                    }
                }
                for (String linked : converter.getLinkedDisplays())
                {
                    try
                    {
                        convert(linked.replace(".bob", ".edl"), paths, force, next, output_dir);
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Cannot convert linked display '" + linked + "'", ex);
                    }
                }
        }
    }

    public static void main(final String[] args) throws Exception
    {
        System.setProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");

        final List<String> files = new ArrayList<>(List.of(args));
        ConverterPreferences.colors_list = "colors.list";
        boolean force = false;
        int depth = 1;
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
                System.out.println("By default, file names are taken 'as is'.");
                System.out.println("When an optional paths.list file is provided,");
                System.out.println("that file can contain a search path, listing");
                System.out.println("one path per line in the file.");
                System.out.println("*.edl files will be resolved by checking along");
                System.out.println("that search path.");
                System.out.println();
                System.out.println("Output files are created where the input file was found,");
                System.out.println("unless a designated output folder is specified.");
                System.out.println();
                System.out.println("The -depth option is best used WITHOUT force,");
                System.out.println("so it will skip files that have already been converted.");
                System.out.println();
                System.out.println("When both -depth and -force are set, the converter");
                System.out.println("will run forever when two displays call each other.");
                System.out.println();
                System.out.println("Options:");
                System.out.println("-help                        - Help");
                System.out.println("-colors /path/to/colors.list - EDM colors.list file to use");
                System.out.println("-paths /path/to/paths.list   - File that lists paths");
                System.out.println("-output /path/to/folder      - Folder into which converted files are written");
                System.out.println("-force                       - Overwrite existing files instead of stopping");
                System.out.println("-depth count                 - Convert just the listed files (1), or also referenced files (2), or more levels down");
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
            else if (files.get(0).startsWith("-p"))
            {
                if (files.size() < 2)
                {
                    System.err.println("Missing file name for -paths /path/to/paths.list");
                    return;
                }
                final String paths_file = files.get(1);
                files.remove(0);
                files.remove(0);
                ConverterPreferences.parseEdmPaths(paths_file);
            }
            else if (files.get(0).startsWith("-o"))
            {
                if (files.size() < 2)
                {
                    System.err.println("Missing folder for -output /path/to/folder");
                    return;
                }
                ConverterPreferences.setAutoConverterDir(files.get(1));
                files.remove(0);
                files.remove(0);
            }
            else if (files.get(0).startsWith("-f"))
            {
                force = true;
                files.remove(0);
            }
            else if (files.get(0).startsWith("-d"))
            {
                if (files.size() < 2)
                {
                    System.err.println("Missing count -depth count");
                    return;
                }
                depth = Integer.parseInt(files.get(1));
                files.remove(0);
                files.remove(0);
            }
        }

        try
        {
            logger.log(Level.INFO, "Reading " + ConverterPreferences.colors_list);
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
                convert(file, ConverterPreferences.paths, force, depth, ConverterPreferences.auto_converter_dir);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot convert " + file, ex);
            }
        }
    }
}
