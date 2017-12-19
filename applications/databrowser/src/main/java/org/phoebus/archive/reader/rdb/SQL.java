/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.rdb;

import org.phoebus.framework.rdb.RDBInfo.Dialect;

/** SQL statements for RDB archive access
 *  @author Kay Kasemir
 *  @author Lana Abadie (PostgreSQL)
 */
@SuppressWarnings("nls")
class SQL
{
    // 'status' table
    final public String sel_stati;

    // 'severity' table
    final public String sel_severities;

    // 'channel' table
    final public String channel_sel_by_like;
    final public String channel_sel_by_name;


    SQL(final Dialect dialect, final String prefix)
    {
        // 'status' table
        sel_stati = "SELECT status_id, name FROM " + prefix + "status";

        // 'severity' table
        sel_severities = "SELECT severity_id, name FROM " + prefix + "severity";

        // 'channel' table
        if (dialect == Dialect.Oracle)
        {   // '\\' because Java swallows one '\', be case-insensitive by using all lowercase
            channel_sel_by_like = "SELECT name FROM " + prefix + "channel WHERE LOWER(name) LIKE LOWER(?) ESCAPE '\\' ORDER BY name";
        }
        else
        {   // MySQL uses '\' by default, and everything is  by default case-insensitive
            channel_sel_by_like = "SELECT name FROM " + prefix + "channel WHERE name LIKE ? ORDER BY name";
        }

        channel_sel_by_name = "SELECT channel_id FROM " + prefix + "channel WHERE name=?";

    }
}
