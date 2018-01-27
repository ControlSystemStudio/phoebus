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

    // Meta data tables
    final public String numeric_meta_sel_by_channel;
    final public String enum_sel_num_val_by_channel;

    // 'channel' table
    final public String channel_sel_by_like;
    final public String channel_sel_by_name;

    // 'sample' table
    final public String sample_sel_initial_time;
    final public String sample_sel_by_id_start_end;
    final public String sample_sel_by_id_start_end_with_blob;
    final public String sample_sel_array_vals;
    final public String sample_count_by_id_start_end;


    SQL(final Dialect dialect, String prefix)
    {
        // MySQL never uses prefix.
        // This allows setting a prefix for Oracle,
        // but then still access some MySQL URLs.
        if (dialect == Dialect.MySQL)
            prefix = "";
        // 'status' table
        sel_stati = "SELECT status_id, name FROM " + prefix + "status";

        // 'severity' table
        sel_severities = "SELECT severity_id, name FROM " + prefix + "severity";

        // Meta data tables
        numeric_meta_sel_by_channel = "SELECT low_disp_rng, high_disp_rng," +
        " low_warn_lmt, high_warn_lmt," +
        " low_alarm_lmt, high_alarm_lmt," +
        " prec, unit FROM " + prefix + "num_metadata WHERE channel_id=?";

        enum_sel_num_val_by_channel = "SELECT enum_nbr, enum_val FROM "
            + prefix + "enum_metadata WHERE channel_id=? ORDER BY enum_nbr";

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

        // 'sample' table
        if (dialect == Dialect.Oracle)
        {   // For Oracle, the stored procedure package
            // also includes a function for determining
            // the initial sample time
            sample_sel_initial_time = RDBPreferences.starttime_function.isEmpty()
                ? "SELECT smpl_time FROM (SELECT smpl_time FROM " +
                  prefix + "sample WHERE channel_id=? AND smpl_time<=?" +
                  " ORDER BY smpl_time DESC) WHERE ROWNUM=1"
                : RDBPreferences.starttime_function;
            sample_sel_by_id_start_end =
                "SELECT smpl_time, severity_id, status_id, num_val, float_val, str_val FROM " + prefix + "sample"+
                "   WHERE channel_id=?" +
                "     AND smpl_time BETWEEN ? AND ?" +
                "   ORDER BY smpl_time";
            sample_sel_by_id_start_end_with_blob =
                "SELECT smpl_time, severity_id, status_id, num_val, float_val, str_val, datatype, array_val" +
                "   FROM " + prefix + "sample" +
                "   WHERE channel_id=?" +
                "     AND smpl_time>=? AND smpl_time<=?" +
                "   ORDER BY smpl_time";
        sample_sel_array_vals = "SELECT float_val FROM " + prefix + "array_val" +
                " WHERE channel_id=? AND smpl_time=? ORDER BY seq_nbr";
        }
        else
        {    // MySQL, Postgres
            sample_sel_initial_time =
                "SELECT smpl_time, nanosecs" +
                "   FROM " + prefix + "sample WHERE channel_id=? AND smpl_time<=?" +
                "   ORDER BY smpl_time DESC, nanosecs DESC LIMIT 1";
            sample_sel_by_id_start_end =
                "SELECT smpl_time, severity_id, status_id, num_val, float_val, str_val, nanosecs FROM " + prefix + "sample" +
                "   WHERE channel_id=?" +
                "     AND smpl_time>=? AND smpl_time<=?" +
                "   ORDER BY smpl_time, nanosecs";
            sample_sel_by_id_start_end_with_blob =
                "SELECT smpl_time, severity_id, status_id, num_val, float_val, str_val, nanosecs, datatype, array_val" +
                "   FROM " + prefix + "sample" +
                "   WHERE channel_id=?" +
                "     AND smpl_time>=? AND smpl_time<=?" +
                "   ORDER BY smpl_time, nanosecs";
            sample_sel_array_vals = "SELECT float_val FROM " + prefix + "array_val" +
                " WHERE channel_id=? AND smpl_time=? AND nanosecs=? ORDER BY seq_nbr";
        }
        // Rough count, ignoring nanosecs for the non-Oracle dialects
        sample_count_by_id_start_end = "SELECT COUNT(*) FROM " + prefix + "sample" +
          "   WHERE channel_id=? AND smpl_time BETWEEN ? AND ?";
    }
}
