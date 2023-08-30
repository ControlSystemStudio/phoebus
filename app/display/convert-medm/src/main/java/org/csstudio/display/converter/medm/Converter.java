/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.medm;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.opibuilder.adl2boy.translator.Arc2Model;
import org.csstudio.opibuilder.adl2boy.translator.Bar2Model;
import org.csstudio.opibuilder.adl2boy.translator.Byte2Model;
import org.csstudio.opibuilder.adl2boy.translator.CartesianPlot2Model;
import org.csstudio.opibuilder.adl2boy.translator.ChoiceButton2Model;
import org.csstudio.opibuilder.adl2boy.translator.Composite2Model;
import org.csstudio.opibuilder.adl2boy.translator.Display2Model;
import org.csstudio.opibuilder.adl2boy.translator.Image2Model;
import org.csstudio.opibuilder.adl2boy.translator.Indicator2Model;
import org.csstudio.opibuilder.adl2boy.translator.Menu2Model;
import org.csstudio.opibuilder.adl2boy.translator.MessageButton2Model;
import org.csstudio.opibuilder.adl2boy.translator.Meter2Model;
import org.csstudio.opibuilder.adl2boy.translator.Oval2Model;
import org.csstudio.opibuilder.adl2boy.translator.Placeholder;
import org.csstudio.opibuilder.adl2boy.translator.PolyLine2Model;
import org.csstudio.opibuilder.adl2boy.translator.Polygon2Model;
import org.csstudio.opibuilder.adl2boy.translator.Rectangle2Model;
import org.csstudio.opibuilder.adl2boy.translator.RelatedDisplay2Model;
import org.csstudio.opibuilder.adl2boy.translator.ShellCommand2Model;
import org.csstudio.opibuilder.adl2boy.translator.StripChart2Model;
import org.csstudio.opibuilder.adl2boy.translator.Text2Model;
import org.csstudio.opibuilder.adl2boy.translator.TextEntry2Model;
import org.csstudio.opibuilder.adl2boy.translator.TextUpdate2Model;
import org.csstudio.opibuilder.adl2boy.translator.TranslatorUtils;
import org.csstudio.opibuilder.adl2boy.translator.Valuator2Model;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.ColorMap;
import org.csstudio.utility.adlparser.fileParser.ParserADL;
import org.phoebus.framework.workbench.FileHelper;

/** MEDM Converter
 *
 *  <p>Can be called as 'Main',
 *  also used by converter app.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Converter
{
    public static final Logger logger = Logger.getLogger(Converter.class.getPackageName());

    private DisplayModel display = new DisplayModel();
    private final WidgetColor[] colorMap;

    public Converter(final File input, final File output) throws Exception
    {
        logger.log(Level.INFO, "Convert " + input + " -> " + output);

        // Parse ADL
        final ADLWidget root = ParserADL.getNextElement(input);

        // Get color map
        colorMap = getColorMap(root);
        logger.log(Level.FINE, "Color map: " + Arrays.toString(colorMap));

        // Get overall display info
        initializeDisplayModel(input.getName(), root);
        logger.log(Level.FINE, "Display '" + display.getName() + "' size " + display.propWidth().getValue() + " x " + display.propHeight().getValue());

        // Convert all widgets
        convertChildren(root.getObjects(), display, colorMap);

        // Write to output
        logger.log(Level.FINE, "Writing " + output);
        final ModelWriter writer = new ModelWriter(new FileOutputStream(output));
        writer.writeModel(display);
        writer.close();
    }

    /** @param root
     *  @return {@link WidgetColor}s
     *  @throws Exception on error
     */
    private WidgetColor[] getColorMap(final ADLWidget root) throws Exception
    {
        WidgetColor[] colorMap = new WidgetColor[0];
        for (ADLWidget adlWidget : root.getObjects())
        {
            String widgetType = adlWidget.getType();
            if (widgetType.equals("color map"))
            {
                ColorMap tempColorMap = new ColorMap(adlWidget);
                colorMap = tempColorMap.getColors();
            }
        }
        return colorMap;
    }

    private void initializeDisplayModel(String filename, final ADLWidget root) throws Exception
    {
        for (ADLWidget adlWidget : root.getObjects())
        {
            final String widgetType = adlWidget.getType();
            if (widgetType.equals("display"))
            {
                display = (new Display2Model(adlWidget, colorMap, null)).getWidgetModel();
                if (filename.endsWith(".adl"))
                    filename = filename.substring(0, filename.length()-4);
                display.propName().setValue(filename);
                return;
            }
        }
    }

    public static void convertChildren(final List<ADLWidget> childWidgets, final Widget parentModel, final WidgetColor[] colorMap)
    {
        for (ADLWidget adlWidget : childWidgets)
        {
            try
            {
                final String widgetType = adlWidget.getType();

                // Top-level entries that are already handled or ignored
                if (widgetType.equals("display"))
                    continue;

                // Alphabetical order
                if (widgetType.equals("arc"))
                    new Arc2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("bar"))
                    new Bar2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("basic attribute"))
                {
                    for (ADLWidget child : adlWidget.getObjects())
                        TranslatorUtils.setDefaultBasicAttribute(child);
                }
                else if (widgetType.equals("byte"))
                    new Byte2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("cartesian plot"))
                    new CartesianPlot2Model(adlWidget, colorMap,parentModel);
                else if (widgetType.equals("choice button"))
                    new ChoiceButton2Model(adlWidget, colorMap,parentModel);
                else if (widgetType.equals("color map"))
                    continue;
                else if (widgetType.equals("composite"))
                    new Composite2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("dynamic attribute"))
                {
                    for (ADLWidget child : adlWidget.getObjects())
                        TranslatorUtils.setDefaultDynamicAttribute(child);
                }
                else if (widgetType.equals("file"))
                    continue;
                else if (widgetType.equals("image"))
                    new Image2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("menu"))
                    new Menu2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("meter"))
                    new Meter2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("message button"))
                    new MessageButton2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("oval"))
                    new Oval2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("polygon"))
                    new Polygon2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("polyline"))
                    new PolyLine2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("rectangle"))
                    new Rectangle2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("related display"))
                    new RelatedDisplay2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("shell command"))
                    new ShellCommand2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("strip chart"))
                    new StripChart2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("text"))
                    new Text2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("text entry"))
                    new TextEntry2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("text update"))
                    new TextUpdate2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("valuator"))
                    new Valuator2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("indicator"))
                    new Indicator2Model(adlWidget, colorMap, parentModel);
                else if (widgetType.equals("children"))
                {
                    // TODO 'children' is not quite the same as 'composite'
                    new Composite2Model(adlWidget, colorMap, parentModel);
                }
                else
                {
                    // TODO Add all the widgets
                    logger.log(Level.WARNING, "Ignoring #" + adlWidget.getObjectNr() + " " + widgetType);
                    new Placeholder(adlWidget, colorMap, parentModel);
                }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot convert #" + adlWidget.getObjectNr() + " " + adlWidget.getType(), ex);
            }
        }
    }

    /** @param infile Input file (*.opi, older *.bob)
     *  @param output_dir Folder where to create output.bob, <code>null</code> to use folder of input file
     *  @param force Overwrite existing files?
     *  @throws Exception on error
     */
    private static void convert(final String input, final File output_dir, final boolean force) throws Exception
    {
        final File infile = new File(input);
        if (! infile.canRead())
            throw new Exception("Cannot read " + infile);

        if (infile.isDirectory())
        {
            logger.log(Level.INFO, "Converting all files in directory " + infile);
            for (File file : infile.listFiles())
                convert(file.getAbsolutePath(), output_dir, force);
            return;
        }

        // Convert *.adl file
        // Copy other file types, which could be *.gif etc.
        if (! input.endsWith(".adl"))
        {
            if (output_dir != null)
            {
                logger.log(Level.INFO, "Copying file " + input + " into " + output_dir);
                final File target = new File(output_dir, new File(input).getName());
                if (target.exists() && force)
                {
                    logger.log(Level.INFO, "Overwriting existing file " + target);
                    target.delete();
                }
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
                    logger.log(Level.INFO, "Overwriting existing file " + outfile);
                    outfile.delete();
                }
                else
                    throw new Exception("Output file " + outfile + " exists. Use option -force to overwrite existing files.");
            }

            new Converter(infile, outfile);
        }
    }

    private static void help()
    {
        System.out.println("Usage: -main org.csstudio.display.converter.medm.Converter [-help] [-output /path/to/folder] <files>");
        System.out.println();
        System.out.println("Converts MEDM *.adl files to Display Builder *.bob format");
        System.out.println();
        System.out.println("-output /path/to/folder   - Folder into which converted files are written (must exist)");
        System.out.println("-force                    - Overwrite existing output files");
        System.out.println("<files>                   - One or more files to convert");
    }

    public static void main(final String[] original_args)
    {
        final List<String> files = new ArrayList<>();
        File output_dir = null;
        boolean force = false;

        final List<String> args = new ArrayList<>(List.of(original_args));
        final Iterator<String> iter = args.iterator();
        while (iter.hasNext())
        {
            final String cmd = iter.next();
            if (cmd.startsWith("-h"))
            {
                help();
                return;
            }
            else if (cmd.startsWith("-o"))
            {
                if (! iter.hasNext())
                {
                    System.err.println("Missing folder for -output /path/to/folder");
                    return;
                }
                iter.remove();
                output_dir = new File(iter.next());
                iter.remove();
            }
            else if (cmd.startsWith("-f"))
            {
                force = true;
                iter.remove();
            }
            else if (cmd.startsWith("-"))
            {
                System.err.println("Unknown option " + cmd);
                help();
                return;
            }
            else
                files.add(cmd);
        }

        if (files.isEmpty())
        {
            help();
            return;
        }
        if (files.get(0).startsWith("-o"))
        {
            if (files.size() < 2)
            files.remove(0);
            files.remove(0);
        }

        if (output_dir != null  &&  force  && !output_dir.exists())
        {
            logger.log(Level.INFO, "Creating output directory " + output_dir);
            output_dir.mkdirs();
        }

        for (String file : files)
        {
            try
            {
                convert(file, output_dir, force);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot convert " + file, ex);
            }
        }
    }
}
