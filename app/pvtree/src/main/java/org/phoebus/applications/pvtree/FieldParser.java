/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Helper for parsing the rec type/field settings.
 *  @author Kay Kasemir
 *  @author Xinyu Wu - Original extension to support "FIELD001-X"
 */
@SuppressWarnings("nls")
public class FieldParser
{
    /** Parse preference string like
     *   "ai(INP,FLNK) ; ao (DOL, SIML , FLNK )"
     *  into list of fields to follow for each record type.
     *
     * @param field_configuration Format "record_type (field1, field2) ; record_type (...)"
     * @return HashMap od record types to list of field names
     * @throws Exception on parse error
     */
    public static Map<String, List<String>> parse(final String field_configuration) throws Exception
    {
        final Map<String, List<String>> rec_fields = new HashMap<>();
        // Split record configs on ';'
        final String[] rec_configs = field_configuration.split("\\s*;\\s*");
        for (String rec_config : rec_configs)
        {
            // Get record type
            final int i1 = rec_config.indexOf('(');
            if (i1 < 0)
                throw new Exception("Missing start of field list in '" + rec_config + "'");
            final String rec_type = rec_config.substring(0, i1).trim();
            if (rec_type.length() <= 0)
                throw new Exception("Missing record type in '" + rec_config + "'");
            final int i2 = rec_config.indexOf(')', i1);
            if (i2 < 0)
                throw new Exception("Missing end of field list in '" + rec_config + "'");
            // Get fields for that type
            final String[] field_configs = rec_config.substring(i1+1, i2).split("\\s*,\\s*");
            final ArrayList<String> fields = new ArrayList<String>();
            for (String field : field_configs)
            {
                final String field_spec = field.trim();
                // Plain 'FIELD', or 'FIELDxxx-yyy'?
                final int range_sep = field_spec.indexOf('-');
                if (range_sep > 0)
                {
                    // 'xxx' has a range_width of 3
                    final int range_width = field_spec.length() - range_sep-1;
                    // 'FIELD'
                    final String base = field_spec.substring(0,  range_sep-range_width);

                    // Numeric "001-999" or character range 'A-Z'?
                    final String start_txt = field_spec.substring(range_sep-range_width, range_sep);
                    if (start_txt.matches("[0-9]+"))
                    {
                        final int start = Integer.valueOf(start_txt);
                        final int end = Integer.valueOf(field_spec.substring(range_sep+1, range_sep+1+range_width));
                        final String format = "%0" + range_width + "d";
                        for (int i=start; i<=end; i++)
                            fields.add(base + String.format(format, i));
                    }
                    else
                    {
                        if (range_width != 1)
                            throw new Exception("Can only handle field ranges with single-character FieldA-L, not for example FieldAA-LL with 2-character ranges");
                        // 'A'
                        char first = field_spec.charAt(range_sep-1);
                        // 'L'
                        char last = field_spec.charAt(range_sep+1);
                        for (char c = first; c<=last; ++c)
                            fields.add(base + c);
                    }
                }
                else
                    fields.add(field_spec);
            }
            // Put into hash
            rec_fields.put(rec_type, fields);
        }
        return rec_fields;
    }
}
