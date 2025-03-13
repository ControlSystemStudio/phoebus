/*******************************************************************************
 * Copyright (c) 2010-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.function.Consumer;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.util.time.TimestampFormats;

/** Eclipse Job for exporting data from Model to file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlainExportJob extends ExportJob
{
    final protected ValueFormatter formatter;

    /** @param model Model
     *  @param start Start time
     *  @param end End time
     *  @param source Data source
     *  @param optimize_parameter Bin count
     *  @param formatter  Value formatter
     *  @param filename Export file name
     *  @param error_handler Error handler
     *  @param unixTimeStamp Use UNIX time stamp epoch?
     */
    public PlainExportJob(final Model model,
            final Instant start, final Instant end, final Source source,
            final double optimize_parameter, final ValueFormatter formatter,
            final String filename,
            final Consumer<Exception> error_handler,
            final boolean unixTimeStamp)
    {   // MS Excel fails to recognize tab-separated data columns
    	// unless the initial header rows also contain at least one tab per row,
    	// so add that to comment
        super("#\t", model, start, end, source, optimize_parameter, filename, error_handler, unixTimeStamp);
        this.formatter = formatter;
    }

    @Override
    protected void printExportInfo(final PrintStream out)
    {
        super.printExportInfo(out);
        out.println(comment + "Format     : " + formatter.toString());
        out.println(comment + "Spreadsheet: TAB-delimited import and format time as yyyy-mm-d h:mm:ss.000");
        out.println();
    }

    @Override
    protected void performExport(final JobMonitor monitor,
                                 final PrintStream out) throws Exception
    {
        int count = 0;
        for (ModelItem item : model.getItems())
        {   // Item header
            if (count > 0)
                out.println();
            printItemInfo(out, item);
            // Get data
            monitor.beginTask(MessageFormat.format("Fetching data for {0}", item.getResolvedName()));
            final ValueIterator values = createValueIterator(item);
            // Dump all values
            out.println(comment + Messages.TimeColumn + Messages.Export_Delimiter + formatter.getHeader());
            long line_count = 0;
            while (values.hasNext()  &&  !monitor.isCanceled())
            {
                final VType value = values.next();

                final String time = unixTimeStamp ? Long.toString(VTypeHelper.getTimestamp(value).toEpochMilli()) :
                        TimestampFormats.MILLI_FORMAT.format(VTypeHelper.getTimestamp(value));
                out.println(time + Messages.Export_Delimiter + formatter.format(value));
                ++line_count;
                if (++line_count % PROGRESS_UPDATE_LINES == 0)
                    monitor.beginTask(MessageFormat.format("{0}: Wrote {1} samples", item.getResolvedName(), line_count));
            }
            ++count;
        }
    }
}
