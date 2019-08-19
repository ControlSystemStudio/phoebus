/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.archive;

import java.io.FileInputStream;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.csstudio.trends.databrowser3.export.ExportJob;
import org.csstudio.trends.databrowser3.export.MatlabFileExportJob;
import org.csstudio.trends.databrowser3.export.MatlabScriptExportJob;
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
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;

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
        System.out.println("General command-line options:");
        System.out.println("-help                                    -  This text");
        System.out.println("-settings settings.xml                   -  Import settings from file, either exported XML or property file format");
        System.out.println("-archives                                -  Set archive URLs, separated by '*'");
        System.out.println();
        System.out.println("Archive Information options:");
        System.out.println("-list [pattern]                          -  List channel names, with optional pattern ('.', '*')");
        System.out.println();
        System.out.println("Data Export options:");
        System.out.println("-start '2019-01-02 08:00:00'             -  Start time");
        System.out.println("-end '2019-02-03 16:15:20'               -  End time (defaults to 'now')");
        System.out.println("-bin bin_count                           -  Export 'optimized' data for given bin count");
        System.out.println("-linear HH:MM:SS                         -  Export linearly extrapolated data for time intervals");
        System.out.println("-decimal precision                       -  Decimal format with given precision");
        System.out.println("-exponential precision                   -  Exponential format with given precision");
        System.out.println("-nostate                                 -  Do not include status/severity in tab-separated file");
        System.out.println("-export /path/to/file channel <channels> -  Export data for one or more channels into file");

        System.out.println();
        System.out.println("File names ending in *.m or *.mat generate Matlab files.");
        System.out.println("All other file name endings create tab-separated data files.");
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
        Source source = Source.RAW_ARCHIVE;
        int optimize_parameter = 800;

        String start = "-1 week", end = "now";
        Style style = Style.Decimal;
        int precision = 3;
        boolean with_status = true;

        String export = null;
        final Iterator<String> iter = args.iterator();
        while (iter.hasNext())
        {
            final String cmd = iter.next();
            if (cmd.startsWith("-h"))
            {
                help();
                return;
            }
            else if (cmd.startsWith("-set"))
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
            else if (cmd.startsWith("-lis"))
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
            else if (cmd.startsWith("-nos"))
            {
                with_status = false;
                iter.remove();
            }
            else if (cmd.startsWith("-b"))
            {
                iter.remove();
                if (! iter.hasNext())
                {
                    System.out.println("Missing -optimized bins");
                    help();
                    return;
                }
                source = Source.OPTIMIZED_ARCHIVE;
                optimize_parameter = Integer.parseInt(iter.next());
                iter.remove();
            }
            else if (cmd.startsWith("-lin"))
            {
                iter.remove();
                if (! iter.hasNext())
                {
                    System.out.println("Missing -linear HH:MM:SS");
                    help();
                    return;
                }
                source = Source.LINEAR_INTERPOLATION;
                optimize_parameter = Math.max(1, (int)SecondsParser.parseSeconds(iter.next()));
                iter.remove();
            }
            else if (cmd.startsWith("-dec"))
            {
                iter.remove();
                if (! iter.hasNext())
                {
                    System.out.println("Missing -decimal precision");
                    help();
                    return;
                }
                style = Style.Decimal;
                precision = Integer.parseInt(iter.next());
                iter.remove();
            }
            else if (cmd.startsWith("-start"))
            {
                iter.remove();
                if (! iter.hasNext())
                {
                    System.out.println("Missing -start YYYY-MM-DD HH:MM:SS");
                    help();
                    return;
                }
                start = iter.next();
                iter.remove();
            }
            else if (cmd.startsWith("-end"))
            {
                iter.remove();
                if (! iter.hasNext())
                {
                    System.out.println("Missing -end YYYY-MM-DD HH:MM:SS");
                    help();
                    return;
                }
                end = iter.next();
                iter.remove();
            }
            else if (cmd.startsWith("-expon"))
            {
                iter.remove();
                if (! iter.hasNext())
                {
                    System.out.println("Missing -exponential precision");
                    help();
                    return;
                }
                style = Style.Exponential;
                precision = Integer.parseInt(iter.next());
                iter.remove();
            }
            else if (cmd.startsWith("-expor"))
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
            else
            {
                System.out.println("Unknown parameters " + args);
                help();
                return;
            }
        }

        final List<ArchiveDataSource> sources = Preferences.parseArchives(archives);
        final ArchiveDataSource[] sources_array = sources.toArray(new ArchiveDataSource[sources.size()]);
        if (list != null)
        {
            System.out.println("# Channels matching pattern '" + list + "'");
            for (ArchiveDataSource arch : sources)
            {
                System.out.println("# Archive: " + arch.getUrl());
                final ArchiveReader reader = ArchiveReaders.createReader(arch.getUrl());
                for (String name : reader.getNamesByPattern(list))
                    System.out.println(name);
            }
            System.out.println("# Done.");
        }
        if (export != null)
        {
            System.out.println("# Exporting: " + args.stream().collect(Collectors.joining(", ")));
            System.out.println("# To: " + export);

            TimeRelativeInterval range = null;
            final Object s = TimeParser.parseInstantOrTemporalAmount(start);
            final Object e = TimeParser.parseInstantOrTemporalAmount(end);
            if (s instanceof Instant)
            {
                if (e instanceof Instant)
                    range = TimeRelativeInterval.of((Instant)s, (Instant)e);
                else if (e instanceof TemporalAmount)
                    range = TimeRelativeInterval.of((Instant)s, (TemporalAmount)e);
            }
            else if (s instanceof TemporalAmount)
            {
                if (e instanceof Instant)
                    range = TimeRelativeInterval.of((TemporalAmount) s, (Instant) e);
                else if (e instanceof TemporalAmount)
                    range = TimeRelativeInterval.of((TemporalAmount) s, (TemporalAmount) e);
            }

            if (range == null)
            {
                System.out.println("Invalid time range " + start + " .. " + end);
                help();
                return;
            }

            System.out.println("# Time range: " + range);

            final TimeInterval abs_range = range.toAbsoluteInterval();

            final ValueFormatter formatter = with_status
                                           ? new ValueWithInfoFormatter(style, precision)
                                           : new ValueFormatter(style, precision);
            // Minimal model, just containing items to export
            final Model model = new Model();
            for (String name : args)
            {
                final PVItem item = new PVItem(name, 0.0);
                item.setArchiveDataSource(sources_array);
                model.addItem(item);
            }

            final ExportJob job;
            final Consumer<Exception> error_handler = ex -> ex.printStackTrace();
            if (export.endsWith(".mat"))
            {
                System.out.println("# Creating binary MatLab data file");
                job = new MatlabFileExportJob(model, abs_range.getStart(), abs_range.getEnd(), source, optimize_parameter, export, error_handler);
            }
            else if (export.endsWith(".m"))
            {
                System.out.println("# Creating MatLab file");
                job = new MatlabScriptExportJob(model, abs_range.getStart(), abs_range.getEnd(), source, optimize_parameter, export, error_handler);
            }
            else
            {
                System.out.println("# Creating tab-separated file");
                job = new SpreadsheetExportJob(model, abs_range.getStart(), abs_range.getEnd(), source, optimize_parameter, formatter, export, error_handler);
            }

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
            // Run job in this thread to allow Ctrl-C
            job.run(monitor);
            System.out.println("# Done.");
        }
    }
}
