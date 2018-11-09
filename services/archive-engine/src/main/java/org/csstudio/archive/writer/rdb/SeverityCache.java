/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.epics.vtype.AlarmSeverity;

/** Caching RDB interface to severity info.
 *  @author Kay Kasemir
 */
public class SeverityCache
{
    /** RDB Helper. */
    final private StringIDHelper helper;

    /** Cache that maps names to severities */
    final private Map<AlarmSeverity, Integer> cache_by_name =
        new HashMap<>();

    /** Constructor */
    public SeverityCache(final Connection connection, final SQL sql)
    {
        helper = new StringIDHelper(connection,
            sql.severity_table, sql.severity_id_column, sql.severity_name_column);
    }

   /** Close prepared statements, clear cache. */
    public void dispose()
    {
        cache_by_name.clear();
    }

    /** Find or create a severity by name.
     *  @param alarmSeverity Severity name
     *  @return Severity
     *  @throws Exception on error
     */
    public int findOrCreate(final AlarmSeverity severity) throws Exception
    {
        // Check cache
        final Integer id = cache_by_name.get(severity);
        if (id != null)
            return id;
        // Find and memorize
        final StringID found = helper.find(severity.name());
        if (found != null)
        {
            cache_by_name.put(severity, found.getId());
            return found.getId();
        }
        // New entry
        final StringID added = helper.add(severity.name());
        cache_by_name.put(severity, added.getId());
        return added.getId();
    }
}
