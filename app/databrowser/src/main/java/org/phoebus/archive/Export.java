/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.archive;

import java.io.FileInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.csstudio.trends.databrowser3.export.ExportJob;
import org.csstudio.trends.databrowser3.export.Source;
import org.csstudio.trends.databrowser3.export.SpreadsheetExportJob;
import org.csstudio.trends.databrowser3.export.ValueFormatter;
import org.csstudio.trends.databrowser3.export.ValueWithInfoFormatter;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.ArchiveReaders;
import org.phoebus.archive.vtype.Style;
import org.phoebus.framework.jobs.BasicJobMonitor;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;

/** 'Main' for exporting archived data to files
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Export
{
    private static void help()
    {
        System.out.println("Usage: -main " + Export.class.getName() + " [options]");
        System.out.println();
        System.out.println("Export archived data into files");
        System.out.println();
        System.out.println("Command-line options:");
        System.out.println();
        System.out.println("-help                                    -  This text");
        System.out.println("-settings settings.xml                   -  Import settings from file, either exported XML or property file format");
        System.out.println("-archives                                -  Set archive URLs, separated by '*'");
        System.out.println("-list [pattern]                          -  List channel names, with optional pattern ('.', '*')");
        System.out.println("-export /path/to/file channel <channels> -  Export data for one or more channels into file");
    }

    public static void main(final String[] original_args) throws Exception
    {
        final String archives = Preferences.archives
                                           .stream()
                                           .map(ds -> ds.getUrl())
                                           .collect(Collectors.joining("*"));

        final List<String> args = new ArrayList<>(List.of(original_args));
        if (args.isEmpty())
        {
            help();
            return;
        }
        String list = null;
        String export = null;
        final Iterator<String> iter = args.iterator();
        while (iter.hasNext())
        {
            while (iter.hasNext())
            {
                final String cmd = iter.next();
                if (cmd.startsWith("-h"))
                {
                    help();
                    return;
                }
                else if (cmd.equals("-settings"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -settings file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();

                    System.out.println("Loading settings from " + filename);
                    if (filename.endsWith(".xml"))
                        java.util.prefs.Preferences.importPreferences(new FileInputStream(filename));
                    else
                        PropertyPreferenceLoader.load(new FileInputStream(filename));
                }
                else if (cmd.equals("-list"))
                {
                    iter.remove();
                    if (iter.hasNext())
                    {
                        list = iter.next();
                        iter.remove();
                    }
                    else
                        list = "*";
                }
                else if (cmd.equals("-export"))
                {
                    iter.remove();
                    if (! iter.hasNext())
                    {
                        System.out.println("Missing -export filename");
                        help();
                        return;
                    }
                    export = iter.next();
                    iter.remove();
                    // Remaining args are channel names to export
                    break;
                }
            }
        }

        final List<ArchiveDataSource> sources = Preferences.parseArchives(archives);
        final ArchiveDataSource[] sources_array = sources.toArray(new ArchiveDataSource[sources.size()]);
        if (list != null)
        {
            System.out.println("# Channels matching pattern '" + list + "'");
            for (ArchiveDataSource source : sources)
            {
                System.out.println("# Archive: " + source.getUrl());
                final ArchiveReader reader = ArchiveReaders.createReader(source.getUrl());
                for (String name : reader.getNamesByPattern(list))
                    System.out.println(name);
            }
            System.out.println("# Done.");
        }
        if (export != null)
        {
            System.out.println("# Exporting: " + args.stream().collect(Collectors.joining(", ")));
            System.out.println("# To: " + export);
            // TODO Command line options for all this

            Instant end = Instant.now();
            Instant start = end.minus(Duration.ofDays(365*20));
            Source source = Source.RAW_ARCHIVE;
            int optimize_parameter = 800;
            int precision = 3;
            boolean with_status = true;


            final ValueFormatter formatter = with_status
                                           ? new ValueWithInfoFormatter(Style.Decimal, precision)
                                           : new ValueFormatter(Style.Decimal, precision);
            // Minimal model, just containing items to export
            final Model model = new Model();
            for (String name : args)
            {
                final PVItem item = new PVItem(name, 0.0);
                item.setArchiveDataSource(sources_array);
                model.addItem(item);
            }
            Consumer<Exception> error_handler = ex -> ex.printStackTrace();
            final ExportJob job = new SpreadsheetExportJob(model, start, end, source, optimize_parameter, formatter, export, error_handler);
            final BasicJobMonitor monitor = new BasicJobMonitor()
            {
                @Override
                public void updateTaskName(String task_name)
                {
                    super.updateTaskName(task_name);
                    System.out.println("# " + this);
                }

                @Override
                public void worked(int worked_steps)
                {
                    super.worked(worked_steps);
                    System.out.println("# " + this);
                }
            };
            job.run(monitor);
            System.out.println("# Done.");
        }
    }
}
