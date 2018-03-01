/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/** Text table formatter
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TextTable
{
    /** Headers for each column */
    final private List<String> headers = new ArrayList<String>();

    /** Widths info for each column */
    final private List<Integer> widths = new ArrayList<Integer>();

    /** Rows of the table */
    final private List<List<String>> rows = new ArrayList<List<String>>();

    /** Headers for each column */
    private List<String> current_row = new ArrayList<String>();

    /** Where to write */
    final private PrintWriter writer;

    /** Initialize
     *  @param out {@link OutputStream}
     */
    public TextTable(final OutputStream out)
    {
        this(new PrintStream(out));
    }

    /** Initialize
     *  @param out {@link PrintStream}
     */
    public TextTable(final PrintStream out)
    {
        this(new PrintWriter(out));
    }

    /** Initialize
     *  @param writer {@link PrintWriter}
     */
    public TextTable(final PrintWriter writer)
    {
        this.writer = writer;
    }

    /** Add a column, sized by the header
     *  @param header Column header
     */
    public void addColumn(final String header)
    {
        headers.add(header);
        widths.add(header.length());
    }

    /** Add a row of values
     *  @param values Values for the cells in the row
     */
    public void addRow(final String... values)
    {
        for (String cell : values)
            addCell(cell);
        // Fill remaining cells in case values don't span a full row
        final int rest = values.length % widths.size();
        for (int i=0; i<rest; ++i)
            addCell("");
    }

    /** Add a cell
     *  @param value Value of a table cell
     */
    public void addCell(final String value)
    {
        final int column = current_row.size();
        current_row.add(value);

        // Update width of this column?
        final int wid = value.length();
        if (wid > widths.get(column))
            widths.set(column, wid);

        // Starting new row?
        if (current_row.size() >= widths.size())
        {
            rows.add(current_row);
            current_row = new ArrayList<String>();
        }
    }

    /** @param value Value to print
     *  @param width Desired width
     */
    private void print(final String value, final int width)
    {
        if (value.length() > width)
            writer.print(value.substring(0, width));
        else
        {
            writer.print(value);
            for (int i=value.length(); i<width; ++i)
                writer.print(' ');
        }
    }

    /** @param width Desired width of the line to print */
    private void line(final int width)
    {
        for (int i=0; i<width; ++i)
            writer.print('=');
    }

    /** Flush the table out, i.e. perform the actual printing */
    public void flush()
    {
        // Print header
        int columns = headers.size();
        for (int i=0; i<columns; ++i)
        {
            if (i > 0)
                writer.print(" ");
            print(headers.get(i), widths.get(i));
        }
        writer.println();

        for (int i=0; i<columns; ++i)
        {
            if (i > 0)
                writer.print(" ");
            line(widths.get(i));
        }
        writer.println();



        // Print rows
        for (List<String> row : rows)
        {
            columns = row.size();
            for (int i=0; i<columns; ++i)
            {
                if (i > 0)
                    writer.print(" ");
                print(row.get(i), widths.get(i));
            }
            writer.println();
        }
        writer.flush();
    }
}
