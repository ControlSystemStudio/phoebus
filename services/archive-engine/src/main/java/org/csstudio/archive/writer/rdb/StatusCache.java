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

/** Caching RDB interface to status info.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StatusCache
{
    /** Helper. */
    final private StringIDHelper helper;

    /** Cache that maps names to stati */
    final private HashMap<String, Status> cache_by_name =
        new HashMap<>();

    /** Constructor */
    public StatusCache(final Connection connection, final SQL sql)
    {
        helper = new StringIDHelper(connection,
            sql.status_table, sql.status_id_column, sql.status_name_column);
    }

    /** Close prepared statements, clear cache. */
    public void dispose()
    {
        cache_by_name.clear();
    }

    /** Add Status to cache */
    public void memorize(final Status status)
    {
        cache_by_name.put(status.getName(), status);
    }

    /** Get status by name.
     *  @param name status name
     *  @return status or <code>null</code>
     *  @throws Exception on error
     */
    private Status find(String name) throws Exception
    {
        if (name.length() == 0)
            name = "";
        // Check cache
        Status status = cache_by_name.get(name);
        if (status != null)
            return status;
        final StringID found = helper.find(name);
        if (found != null)
        {
            status = new Status(found.getId(), found.getName());
            memorize(status);
        }
        // else: Nothing found
        return status;
    }

    /** Find or create a status by name.
     *  @param name Status name
     *  @return Status
     *  @throws Exception on error
     */
    public Status findOrCreate(String name) throws Exception
    {
        if (name.length() == 0)
            name = "";
        // Existing entry?
        Status status = find(name);
        if (status != null)
            return status;
        final StringID added = helper.add(name);
        status = new Status(added.getId(), added.getName());
        memorize(status);
        return status;
    }
}
