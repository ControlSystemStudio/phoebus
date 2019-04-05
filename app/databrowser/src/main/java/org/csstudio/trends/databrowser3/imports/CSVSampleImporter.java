/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.imports;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.VNumber;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VType;
import org.phoebus.pv.TimeHelper;
import org.phoebus.util.time.TimestampFormats;

/** {@link SampleImporter} for comma, space, tab separated value file of time, value
 *  @author Jaka Bobnar - Parse min, max
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CSVSampleImporter implements SampleImporter
{
    final private Logger logger = Logger.getLogger(getClass().getName());
    final private Display meta_data = Display.none();

    @Override
    public String getType()
    {
        return "csv";
    }

    @Override
    public List<VType> importValues(final InputStream input) throws Exception
    {
        final Pattern pattern = Pattern.compile(
                //    YYYY-MM-DD HH:MM:SS.SSS   value  ignore
                // or
                //    YYYY/MM/DD HH:MM:SS.SSSSSSSSS   value  ignore
                "\\s*([0-9][0-9][0-9][0-9][-/][0-9][0-9][-/][0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9]\\.[0-9]*)[ \\t,]+([-+0-9.,eE]+)\\s*.*");

        final Pattern statisticsPattern = Pattern.compile(
                //    YYYY-MM-DD HH:MM:SS.SSS   value    negativeError    positiveError    ignore
                // or
                //    YYYY/MM/DD HH:MM:SS.SSSSSSSSS   value    negativeError    positiveError    ignore
                "\\s*([0-9][0-9][0-9][0-9][-/][0-9][0-9][-/][0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9]\\.[0-9]*)[ \\t,]+([-+0-9.,eE]+)[ \\t,]+([-+0-9.,eE]+)[ \\t,]+([-+0-9.,eE]+)\\s*.*");


        final List<VType> values = new ArrayList<>();

        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(input));
        String line;
        char groupingSeparator = DecimalFormatSymbols.getInstance().getGroupingSeparator();
        char decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
        boolean statistics = true;
        while ((line = reader.readLine()) != null)
        {
            line = line.trim();
            // Skip empty lines, comments
            if (line.length() <= 0  ||  line.startsWith("#"))
                continue;
            statistics = true;
            // Locate time and value
            // Is statistical data?
            Matcher matcher = statisticsPattern.matcher(line);
            if (! matcher.matches())
            {
                // Not statistical data, try normal
                matcher = pattern.matcher(line);
                if (! matcher.matches())
                {
                    logger.log(Level.INFO, "Ignored input: {0}", line);
                    continue;
                }
                statistics = false;
            }


            // Parse
            // Date may use '-' or '/' as separator. Force '-'
            String date_text = matcher.group(1).replace('/', '-');
            // Can only parse up to millisecs, so limit length
            if (date_text.length() > 23)
                date_text = date_text.substring(0, 23);

            final Instant time = Instant.from(TimestampFormats.MILLI_FORMAT.parse(date_text));

            // Double.parseDouble only parses numbers in format #.#... or #.#...#E0, meaning
            // that you cannot have any grouping separators, and the decimal separator must be '.'
            // First remove all grouping separators, then replace the decimal separator with a '.'
            final double number = Double.parseDouble(
                    remove(matcher.group(2),groupingSeparator).replace(decimalSeparator, '.'));
            if (statistics)
            {
                final double min = Double.parseDouble(
                        remove(matcher.group(3),groupingSeparator).replace(decimalSeparator, '.'));
                final double max = Double.parseDouble(
                        remove(matcher.group(4),groupingSeparator).replace(decimalSeparator, '.'));
                values.add(VStatistics.of(number, 0.0, number-min, number+max, 1, Alarm.none(), TimeHelper.fromInstant(time), Display.none()));
            }
            else
                values.add(VNumber.of(number, Alarm.none(), TimeHelper.fromInstant(time), meta_data));
        }
        reader.close();

        return values;
    }

    /** Remove all occurrences of the character from the string.
     *
     *  @param source the string to remove the characters from
     *  @param charToRemove the character to remove
     *  @return the string without any occurrence of the given character
     */
    private static String remove(final String source, final char charToRemove)
    {
        if (source.indexOf(charToRemove) < 0)
            return source;
        final char[] chars = source.toCharArray();
        int pos = 0;
        for (int j = 0; j < chars.length; j++)
        {
            if (chars[j] != charToRemove)
                chars[pos++] = chars[j];
        }
        return new String(chars,0,pos);
    }
}
