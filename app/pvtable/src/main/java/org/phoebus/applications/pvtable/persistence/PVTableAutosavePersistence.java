/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.persistence;

import static org.phoebus.applications.pvtable.PVTableApplication.logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.phoebus.applications.pvtable.Settings;
import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.SavedArrayValue;
import org.phoebus.applications.pvtable.model.SavedScalarValue;
import org.phoebus.applications.pvtable.model.SavedValue;
import org.phoebus.applications.pvtable.model.TimestampHelper;

/** Persist PVTableModel as EPICS Autosave file
 *
 *  <p>This file format is used by the EPICS synApps 'autosave' module,
 *  written by APS/AOD/BCDA Tim Mooney,
 *  http://www.aps.anl.gov/bcda/synApps/autosave/autosave.html
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableAutosavePersistence extends PVTablePersistence
{
    /** File extension used for autosave files */
    final public static String FILE_EXTENSION = "sav";

    /** Start of array, includes final ' ' */
    final private static String ARRAY_START = "@array@ { ";

    /** End of array, not including initial ' ' */
    final private static String ARRAY_END = "}";

    /** End-of-file marker */
    final private static String END_MARKER = "<END>";

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION;
    }

    @Override
    public void read(final PVTableModel model, final InputStream stream) throws Exception
    {
        final BufferedReader input = new BufferedReader(new InputStreamReader(stream));

        int line_no = 0;
        for (String line = input.readLine(); line != null; line = input.readLine())
        {
            ++line_no;
            line = line.trim();
            // Skip comments, empty lines
            if (line.startsWith("#") || line.isEmpty())
                continue;
            // Ignore the end marker (and stop reading)
            if (line.startsWith(END_MARKER))
                break;

            // Parse "PV Value" based on first space
            final int sep = line.indexOf(' ');
            if (sep < 0)
            {
                logger.log(Level.WARNING, "Missing space after PV name in line {0}", line_no);
                continue;
            }
            final String pv_name = line.substring(0, sep);
            final String value_text = line.substring(sep + 1);
            final SavedValue value;
            try
            {
                value = parseValue(value_text);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error parsing value in line " + line_no, ex);
                continue;
            }
            model.addItem(pv_name, Settings.tolerance, value, "");
        }
        input.close();
    }

    /** Parse a channel's value from the file
     *
     *  <p>Example values are <code>3.14 10 @array@ { "72" "101" "108" "108" "111" "0" }</code>
     *
     *  @param text Value text
     *  @return {@link SavedValue}
     *  @throws Exception on error
     */
    public SavedValue parseValue(final String text) throws Exception
    {
        if (text.startsWith(ARRAY_START))
            return parseArray(text);
        else
            return new SavedScalarValue(text);
    }

    /** Parse array value from the file
     *
     *  <p>Example values are <code>array@ { "72" "101" "108" "108" "111" "0" }</code>
     *
     *  @param text Value text
     *  @return {@link SavedValue}
     *  @throws Exception on error
     */
    private SavedValue parseArray(final String text) throws Exception
    {
        final int end = text.lastIndexOf(ARRAY_END);
        if (end < 0)
            throw new Exception("Missing end-of-array marker");

        final List<String> items = new ArrayList<>();
        int i = ARRAY_START.length();
        while (i < end)
        {
            // locate start & end of next item
            final int item_start = text.indexOf('"', i) + 1;
            if (item_start <= 0)
                break;

            int item_end = item_start + 1;
            while (item_end < end)
            {
                char c = text.charAt(item_end);
                if (c == '"')
                    break;
                // Skip escaped character
                if (c == '\\')
                    ++item_end;

                ++item_end;
            }
            if (item_end >= end)
                throw new Exception("Missing end of item");

            // Remove escape markers from item
            final String item = text.substring(item_start, item_end).replaceAll("\\\\", "");
            items.add(item);
            i = item_end + 1;
        }
        return new SavedArrayValue(items);
    }

    @Override
    public void write(final PVTableModel model, final OutputStream stream) throws Exception
    {
        final PrintWriter out = new PrintWriter(stream);
        out.println("# save/restore file generated by CSS PVTable, " + TimestampHelper.format(Instant.now()));
        for (PVTableItem item : model.getItems())
        {
            final SavedValue saved = item.getSavedValue().orElse(null);
            if (saved == null)
                out.println("# " + item.getName() + " - No saved value");
            else
            {
                if (saved instanceof SavedScalarValue)
                    out.println(item.getName() + " " + saved.toString());
                else if (saved instanceof SavedArrayValue)
                {
                    final SavedArrayValue array = (SavedArrayValue) saved;
                    out.print(item.getName() + " ");
                    out.print(ARRAY_START);
                    for (int e = 0; e < array.size(); ++e)
                    {
                        out.print('"');
                        out.print(array.get(e).replace("\"", "\\\""));
                        out.print("\" ");
                    }
                    out.println(ARRAY_END);
                }
                else
                    throw new Exception("Cannot persist saved value of type " + saved.getClass().getName());
            }
        }
        out.println(END_MARKER);
        out.close();
    }
}
