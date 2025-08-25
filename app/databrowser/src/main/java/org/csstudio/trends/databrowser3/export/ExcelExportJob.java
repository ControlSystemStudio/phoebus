/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.SpreadsheetIterator;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimestampFormats;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/** Export Job for exporting data from Model as Excel file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExcelExportJob extends ExportJob
{
    private Workbook wb = null;
    private CellStyle comment_style, header_style, timestamp_style;
    private Sheet sheet;
    private Row row;
    private ZoneId zone = ZoneId.systemDefault();
    private final boolean tabular, min_max, sevr_stat;

    /** @param model Model
     *  @param start Start time
     *  @param end End time
     *  @param source Data source
     *  @param tabular Create one combined table? Otherwise one table per channel
     *  @param min_max Show min/max info (error) for statistical data?
     *  @param sevr_stat Include alarm severity and status?
     *  @param optimize_parameter Bin count
     *  @param filename Export file name
     *  @param error_handler Error handler
     *  @param unixTimeStamp Use UNIX time stamp epoch?
     */
    public ExcelExportJob(final Model model,
            final Instant start, final Instant end, final Source source,
            final boolean tabular, final boolean min_max, final boolean sevr_stat,
            final double optimize_parameter,
            final String filename,
            final Consumer<Exception> error_handler,
            final boolean unixTimeStamp)
    {
        super("", model, start, end, source, optimize_parameter, filename, error_handler, unixTimeStamp);
        this.tabular = tabular;
        this.min_max = min_max;
        this.sevr_stat = sevr_stat;
    }

    private void addComment(final Row row, final String label, final String text)
    {
        Cell cell = row.createCell(0, CellType.STRING);
        cell.setCellValue(label);
        cell.setCellStyle(comment_style);
        if (text != null)
        {
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue(text);
            cell.setCellStyle(comment_style);
        }
    }

    /** {@inheritDoc}  */
    @Override
    protected void printExportInfo(final PrintStream out) throws Exception
    {
        // Called first and may throw Exception, so create workbook etc in here
        wb = new HSSFWorkbook();

        comment_style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        comment_style.setFont(font);

        header_style = wb.createCellStyle();
        font = wb.createFont();
        font.setItalic(true);
        header_style.setFont(font);
        header_style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header_style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        timestamp_style = wb.createCellStyle();
        timestamp_style.setDataFormat(
                wb.getCreationHelper()
                  .createDataFormat()
                  .getFormat("yyyy-mm-dd hh:mm:ss.000"));

        // Create sheet with summary of exported data
        sheet = wb.createSheet("Archive Data");

        addComment(row = sheet.createRow(0),                   "Created by CS-Studio Data Browser", null);
        addComment(row = sheet.createRow(row.getRowNum() + 2), "Start time", TimestampFormats.MILLI_FORMAT.format(start));
        addComment(row = sheet.createRow(row.getRowNum() + 1), "End time", TimestampFormats.MILLI_FORMAT.format(end));
        addComment(row = sheet.createRow(row.getRowNum() + 1), "Source", source.toString());

        if (source == Source.OPTIMIZED_ARCHIVE)
            addComment(row = sheet.createRow(row.getRowNum() + 1), "Desired Value Count", Double.toString(optimize_parameter));
        else if (source == Source.LINEAR_INTERPOLATION)
            addComment(row = sheet.createRow(row.getRowNum() + 1), "Interpolation Interval", SecondsParser.formatSeconds(optimize_parameter));
    }

    /** @param row Row where to create time stamp cell
     *  @param time Timestamp to place in cell
     *  @return The cell in column 0 of row
     */
    private Cell createTimeCell(final Row row, final Instant time)
    {
        Cell cell = row.createCell(0, CellType.NUMERIC);
        if (unixTimeStamp)
            cell.setCellValue(time.toEpochMilli());
        else
        {
            cell.setCellValue(LocalDateTime.ofInstant(time, zone));
            cell.setCellStyle(timestamp_style);
        }
        return cell;
    }

    /** @param row Row where to create value cell
     *  @param column Column index
     *  @param value Value to show
     *  @return Cell that was created for the value
     */
    private Cell createValueCell(final Row row, final int column, final VType value)
    {
        final Cell cell;
        if (value instanceof VNumber v)
        {
            cell = row.createCell(column, CellType.NUMERIC);
            cell.setCellValue(v.getValue().doubleValue());
        }
        else if (value instanceof VStatistics v)
        {
            cell = row.createCell(column, CellType.NUMERIC);
            cell.setCellValue(v.getAverage());
        }
        else if (value instanceof VEnum v)
        {
            cell = row.createCell(column, CellType.NUMERIC);
            cell.setCellValue(v.getIndex());
        }
        else if (value instanceof VString v)
        {
            cell = row.createCell(column, CellType.STRING);
            cell.setCellValue(v.getValue());
        }
        else if (value == null)
            cell = row.createCell(column, CellType.BLANK);
        else
        {
            cell = row.createCell(column, CellType.STRING);
            cell.setCellValue(Objects.toString(value));
        }
        return cell;
    }

    /** Create basic value cell as well as optional min/max and sevr/stat cells
     *  @param row Row where to create cells
     *  @param column Index of first cell column
     *  @param value Value to show in cell(s)
     *  @return Last cell created
     */
    private Cell createValueCells(final Row row, final int column, final VType value)
    {
        Cell cell = createValueCell(row, column, value);
        if (min_max)
        {
            if (value instanceof VStatistics stats)
            {   // Turn min..max into negative & positive error
                cell = row.createCell(cell.getColumnIndex()+1, CellType.NUMERIC);
                cell.setCellValue(stats.getAverage() - stats.getMin());
                cell = row.createCell(cell.getColumnIndex()+1, CellType.NUMERIC);
                cell.setCellValue(stats.getMax() - stats.getAverage());
            }
            else
            {
                cell = row.createCell(cell.getColumnIndex()+1, CellType.BLANK);
                cell = row.createCell(cell.getColumnIndex()+1, CellType.BLANK);
            }
        }
        if (sevr_stat)
        {
            cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
            cell.setCellValue(Objects.toString(org.phoebus.core.vtypes.VTypeHelper.getSeverity(value)));

            cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
            cell.setCellValue(VTypeHelper.getMessage(value));
        }
        return cell;
    }


    @Override
    protected void performExport(final JobMonitor monitor, final PrintStream out) throws Exception
    {
        // Item header
        for (ModelItem item : model.getItems())
        {
            addComment(row = sheet.createRow(row.getRowNum() + 2), "Channel", item.getResolvedName());
            if (! item.getName().equals(item.getDisplayName()))
                addComment(row = sheet.createRow(row.getRowNum() + 1), "Name", item.getResolvedDisplayName());

            if (item instanceof PVItem)
            {
                final PVItem pv = (PVItem) item;
                addComment(row = sheet.createRow(row.getRowNum() + 1), "Archives:", null);

                int i=1;
                for (ArchiveDataSource archive : pv.getArchiveDataSources())
                {
                    addComment(row = sheet.createRow(row.getRowNum() + 1),
                               i + ") " + archive.getName(),
                               "URL " + archive.getUrl());
                    ++i;
                }
            }
        }

        if (tabular)
            exportTable(monitor);
        else
            exportList(monitor);

        wb.write(out);
    }

    /** Export data in combined table */
    private void exportTable(final JobMonitor monitor) throws Exception
    {
        // Spreadsheet data header
        row = sheet.createRow(row.getRowNum() + 2);
        Cell cell = row.createCell(0, CellType.STRING);
        cell.setCellStyle(header_style);
        cell.setCellValue(Messages.TimeColumn);
        for (ModelItem item : model.getItems())
        {
            cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
            cell.setCellStyle(header_style);
            cell.setCellValue(item.getResolvedName());

            if (min_max)
            {
                cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
                cell.setCellStyle(header_style);
                cell.setCellValue(Messages.NegErrColumn);

                cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
                cell.setCellStyle(header_style);
                cell.setCellValue(Messages.PosErrColumn);
            }
            if (sevr_stat)
            {
                cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
                cell.setCellStyle(header_style);
                cell.setCellValue(Messages.SeverityColumn);

                cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
                cell.setCellStyle(header_style);
                cell.setCellValue(Messages.StatusColumn);
            }
        }

        // Create spreadsheet interpolation
        final List<ValueIterator> iters = new ArrayList<>();
        for (ModelItem item : model.getItems())
        {
            monitor.beginTask(MessageFormat.format("Fetching data for {0}", item.getName()));
            iters.add(createValueIterator(item));
        }
        final SpreadsheetIterator iter = new SpreadsheetIterator(iters.toArray(new ValueIterator[iters.size()]));
        // Dump the spreadsheet lines
        long line_count = 0;
        while (iter.hasNext()  &&  !monitor.isCanceled())
        {
            final Instant time = iter.getTime();
            final VType line[] = iter.next();

            cell = createTimeCell(row = sheet.createRow(row.getRowNum() + 1), time);
            for (int i=0; i<line.length; ++i)
                cell = createValueCells(row, cell.getColumnIndex()+1, line[i]);
            ++line_count;
            if ((line_count % PROGRESS_UPDATE_LINES) == 0)
                monitor.beginTask(MessageFormat.format("Wrote {0} samples", line_count));
            if (monitor.isCanceled())
                break;
        }
        iter.close();
    }

    /** Export data as list of single-channel tables */
    private void exportList(final JobMonitor monitor) throws Exception
    {
        for (ModelItem item : model.getItems())
        {
            // Item data header
            row = sheet.createRow(row.getRowNum() + 2);
            Cell cell = row.createCell(0, CellType.STRING);
            cell.setCellStyle(header_style);
            cell.setCellValue(Messages.TimeColumn);
            cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
            cell.setCellStyle(header_style);
            cell.setCellValue(item.getResolvedName());

            if (min_max)
            {
                cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
                cell.setCellStyle(header_style);
                cell.setCellValue(Messages.NegErrColumn);

                cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
                cell.setCellStyle(header_style);
                cell.setCellValue(Messages.PosErrColumn);
            }
            if (sevr_stat)
            {
                cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
                cell.setCellStyle(header_style);
                cell.setCellValue(Messages.SeverityColumn);

                cell = row.createCell(cell.getColumnIndex()+1, CellType.STRING);
                cell.setCellStyle(header_style);
                cell.setCellValue(Messages.StatusColumn);
            }

            // Dump data lines
            monitor.beginTask(MessageFormat.format("Fetching data for {0}", item.getName()));
            final ValueIterator iter = createValueIterator(item);
            long line_count = 0;
            while (iter.hasNext()  &&  !monitor.isCanceled())
            {
                final VType value = iter.next();
                final Instant time = org.phoebus.core.vtypes.VTypeHelper.getTimestamp(value);

                cell = createTimeCell(row = sheet.createRow(row.getRowNum() + 1), time);
                cell = createValueCells(row, cell.getColumnIndex()+1, value);
                ++line_count;
                if ((line_count % PROGRESS_UPDATE_LINES) == 0)
                    monitor.beginTask(MessageFormat.format("Wrote {0} samples", line_count));
                if (monitor.isCanceled())
                    break;
            }
            iter.close();
            if (monitor.isCanceled())
                break;
        }
    }
}
