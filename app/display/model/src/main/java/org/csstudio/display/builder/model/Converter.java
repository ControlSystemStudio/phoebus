/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;

/** 'Main' for converting *.opi or older *.bob files into the current format
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Converter
{
    /** @param infile Input file (*.opi, older *.bob)
     *  @param outfile Output file (*.bob to write)
     *  @throws Exception on error
     */
    private static void convert(final File infile, final File outfile) throws Exception
    {
        System.out.println("Converting: " + infile + " => " + outfile);

        final ModelReader reader = new ModelReader(new FileInputStream(infile));
        DisplayModel model = reader.readModel();
        ModelWriter writer = new ModelWriter(new FileOutputStream(outfile));
        writer.writeModel(model);
        writer.close();
    }

    /** @param infile Input file (*.opi, older *.bob)
     *  @param output_dir Folder where to create output.bob, <code>null</code> to use folder of input file
     *  @throws Exception on error
     */
    private static void convert(final String input, final File output_dir) throws Exception
    {
        final File infile = new File(input);
        if (! infile.canRead())
            throw new Exception("Cannot read " + infile);
        File outfile;
        if (input.endsWith(".opi"))
            outfile = new File(input.substring(0, input.length()-4) + ".bob");
        else
            outfile = new File(input);
        if (output_dir != null)
            outfile = new File(output_dir, outfile.getName());
        if (outfile.canRead())
            throw new Exception("Output file " + outfile + " exists");

        convert(infile, outfile);
    }

    public static void main(final String[] args)
    {
        if (args.length == 0  || args[0].startsWith("-h"))
        {
            System.out.println("Usage: -main org.csstudio.display.builder.model.Converter [-help] [-output /path/to/folder] <files>");
            System.out.println();
            System.out.println("Converts BOY *.opi files to Display Builder *.bob format");
            System.out.println();
            System.out.println("-output /path/to/folder   - Folder into which converted files are written");
            System.out.println("<files>                   - One or more files to convert");
            return;
        }
        final List<String> files = new ArrayList<>(List.of(args));
        final File output_dir;
        if (files.get(0).startsWith("-o"))
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
        else
            output_dir = null;
        for (String file : files)
        {
            try
            {
                convert(file, output_dir);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot convert " + file, ex);
            }
        }
    }
}
