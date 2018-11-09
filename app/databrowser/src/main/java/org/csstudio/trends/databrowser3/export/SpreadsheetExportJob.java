/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.SpreadsheetIterator;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.util.time.TimestampFormats;

/** Ecipse Job for exporting data from Model to file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SpreadsheetExportJob extends PlainExportJob
{
    public SpreadsheetExportJob(final  Model model,
            final Instant start, final Instant end, final Source source,
            final int optimize_parameter, final ValueFormatter formatter,
            final String filename,
            final Consumer<Exception> error_handler)
    {
        super(model, start, end, source, optimize_parameter, formatter, filename, error_handler);
    }

    /** {@inheritDoc} */
    @Override
    protected void performExport(final JobMonitor monitor,
                                 final PrintStream out) throws Exception
    {
        // Item header
        for (ModelItem item : model.getItems())
            printItemInfo(out, item);
        out.println();
        // Spreadsheet Header
        out.print("# " + Messages.TimeColumn);
        for (ModelItem item : model.getItems())
            out.print(Messages.Export_Delimiter + item.getName() + " " + formatter.getHeader());
        out.println();

        // Create speadsheet interpolation
        final List<ValueIterator> iters = new ArrayList<>();
        for (ModelItem item : model.getItems())
        {
            monitor.beginTask(MessageFormat.format("Fetching data for {0}", item.getName()));
            iters.add(createValueIterator(item));
        }
        final SpreadsheetIterator sheet = new SpreadsheetIterator(iters.toArray(new ValueIterator[iters.size()]));
        // Dump the spreadsheet lines
        long line_count = 0;

        while (sheet.hasNext()  &&  !monitor.isCanceled())
        {
            final Instant time = sheet.getTime();
            final VType line[] = sheet.next();
            out.print(TimestampFormats.MILLI_FORMAT.format(time));

            for (int i=0; i<line.length; ++i)
                out.print(Messages.Export_Delimiter + formatter.format(line[i]));
            out.println();
            ++line_count;
            if ((line_count % PROGRESS_UPDATE_LINES) == 0)
                monitor.beginTask(MessageFormat.format("Wrote {0} samples", line_count));
            if (monitor.isCanceled())
                break;
        }
        sheet.close();
    }
}
