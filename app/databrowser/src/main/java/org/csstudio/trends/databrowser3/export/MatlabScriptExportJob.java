/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

import java.io.PrintStream;
import java.sql.Date;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.function.Consumer;

import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.framework.jobs.JobMonitor;

/** Eclipse Job for exporting data from Model to Matlab-format file.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MatlabScriptExportJob extends ExportJob
{
    public MatlabScriptExportJob(final Model model, final Instant start,
            final Instant end, final Source source,
            final int optimize_parameter, final String filename,
            final Consumer<Exception> error_handler)
    {
        super("% ", model, start, end, source, optimize_parameter, filename, error_handler);
    }

    /** {@inheritDoc} */
    @Override
    protected void printExportInfo(final PrintStream out)
    {
        super.printExportInfo(out);
        out.println(comment);
        out.println(comment + "This file can be loaded into Matlab");
        out.println(comment);
        out.println(comment + "It defines a 'Time Series' object for each channel");
        out.println(comment + "which can be displayed via the 'plot' command.");
        out.println(comment + "Time series can be analyzed further with the Matlab");
        out.println(comment + "Time Series Tools, see Matlab manual.");
        out.println();
    }

    /** {@inheritDoc} */
    @Override
    protected void performExport(final JobMonitor monitor,
                                 final PrintStream out) throws Exception
    {
        final DateFormat date_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        int count = 0;
        for (ModelItem item : model.getItems())
        {
            // Item header
            if (count > 0)
                out.println();
            printItemInfo(out, item);
            // Get data
            monitor.beginTask(MessageFormat.format("Fetching data for {0}", item.getName()));
            final ValueIterator values = createValueIterator(item);
            // Dump all values
            MatlabQualityHelper qualities = new MatlabQualityHelper();
            long line_count = 0;
            out.println("clear t;");
            out.println("clear v;");
            out.println("clear q;");
            while (values.hasNext()  &&  !monitor.isCanceled())
            {
                final VType value = values.next();
                ++line_count;
                // t(1)='2010/03/15 13:30:10.123';
                out.println("t{" + line_count + "}='" +
                    date_format.format(Date.from(org.phoebus.core.vtypes.VTypeHelper.getTimestamp(value))) + "';");
                // v(1)=4.125;
                final double num = VTypeHelper.toDouble(value);
                if (Double.isNaN(num) || Double.isInfinite(num))
                    out.println("v(" + line_count + ")=NaN;");
                else
                    out.println("v(" + line_count + ")=" + num +";");
                // q(1)=0;
                out.println("q(" + line_count + ")=" + qualities.getQualityCode(org.phoebus.core.vtypes.VTypeHelper.getSeverity(value), VTypeHelper.getMessage(value)) +";");
                if (line_count % PROGRESS_UPDATE_LINES == 0)
                    monitor.beginTask(MessageFormat.format("{0}: Wrote {1} samples", item.getName(), line_count));
            }

            out.println(comment + "Convert time stamps into 'date numbers'");
            out.println("tn=datenum(t, 'yyyy/mm/dd HH:MM:SS.FFF');");
            out.println(comment + "Prepare patched data because");
            out.println(comment + "timeseries() cannot handle duplicate time stamps");
            out.println("[xx, idx]=unique(tn, 'last');");
            out.println("pt=tn(idx);");
            out.println("pv=v(idx);");
            out.println("pq=q(idx);");
            out.println("clear xx idx");
            out.println(comment + "Convert into time series and plot");
            // Patch "_" in name because Matlab plot will interprete it as LaTeX sub-script
            final String channel_name = item.getResolvedDisplayName().replace("_", "\\_");
            out.println("channel"+count+"=timeseries(pv', pt', pq', 'IsDatenum', true, 'Name', '"+channel_name+"');");

            out.print("channel"+count+".QualityInfo.Code=[");
            for (int q=0; q<qualities.getNumCodes(); ++q)
                out.print(" " + q);
            out.println(" ];");

            out.print("channel"+count+".QualityInfo.Description={");
            for (int q=0; q<qualities.getNumCodes(); ++q)
                out.print(" '" + qualities.getQuality(q) + "'");
            out.println(" };");

            out.println();
            ++count;
        }
        out.println(comment + "Example for plotting the data");
        for (int i=0; i<count; ++i)
        {
            out.println("subplot(1, " + count + ", " + (i+1) + ");");
            out.println("plot(channel" + i + ");");
        }
    }
}
