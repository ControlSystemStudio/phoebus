/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

import java.util.logging.Logger;

import org.csstudio.archive.Preferences;
import org.phoebus.framework.rdb.RDBInfo.Dialect;

/** SQL statements for writing archive data
 *  @author Kay Kasemir
 *  @author Lana Abadie (PostgreSQL for original RDBArchive code)
 */
@SuppressWarnings("nls")
public class SQL
{
    // 'channel' table
    final String channel_sel_by_name;

    // 'enum_metadata' table
    final public String enum_delete_by_channel;
    final public String enum_insert_channel_num_val;

    // 'num_metadata' table
    final public String numeric_meta_insert;
    final public String numeric_meta_delete_by_channel;

    // 'severity' table
    final public String severity_table;
    final public String severity_id_column;
    final public String severity_name_column;

    // 'status' table
    final public String status_table;
    final public String status_id_column;
    final public String status_name_column;

    // 'sample' table
    final public String sample_insert_double_blob;
    final public String sample_insert_double;
    final public String sample_insert_double_array_element;
    final public String sample_insert_int;
    final public String sample_insert_string;

    /** Initialize
     *  @param dialect RDB Dialect
     *  @param schema Schema prefix (May be ""), not including "."
     */
    public SQL(final Dialect dialect, String schema)
    {
        if (schema == null)
            schema = "";
        else if (schema.length() > 0)
            schema = schema + ".";
        channel_sel_by_name = "SELECT channel_id FROM " + schema + "channel WHERE name=?";

        // 'enum_metadata' table
        enum_delete_by_channel = "DELETE FROM " + schema + "enum_metadata WHERE channel_id=?";
        enum_insert_channel_num_val = "INSERT INTO " + schema
            + "enum_metadata(channel_id,enum_nbr, enum_val) VALUES(?,?,?)";

        // 'num_metadata' table
        numeric_meta_insert = "INSERT INTO " + schema + "num_metadata " +
                "(channel_id, low_disp_rng, high_disp_rng," +
                " low_warn_lmt, high_warn_lmt," +
                " low_alarm_lmt, high_alarm_lmt," +
                " prec, unit) VALUES (?,?,?,?,?,?,?,?,?)";
        numeric_meta_delete_by_channel = "DELETE FROM "
            + schema + "num_metadata WHERE channel_id=?";

        // 'severity' table
        severity_table = schema + "severity";
        severity_id_column = "severity_id";
        severity_name_column = "name";

        // 'status' table
        status_table = schema + "status";
        status_name_column = "name";
        status_id_column = "status_id";

        // 'sample' table
        final String sample = Preferences.write_sample_table;
        Logger.getLogger(getClass().getName()).fine("Writing to table '" + sample + "'");
        switch (dialect)
        {
        case Oracle:
            // Order of initial columns must match for all dialects
            // 6th parameter is 'nanosecs' for all but Oracle
            sample_insert_double_blob =
            "INSERT INTO " + schema + sample + " " +
                    "(channel_id, smpl_time, severity_id, status_id, float_val, datatype, array_val)" +
                    " VALUES (?,?,?,?,?,?,?)";
            sample_insert_double =
                "INSERT INTO " + schema + sample + " " +
                " (channel_id, smpl_time, severity_id, status_id, float_val)" +
                " VALUES (?,?,?,?,?)";
            sample_insert_double_array_element =
                "INSERT INTO " + schema + "array_val" +
                " (channel_id, smpl_time, seq_nbr, float_val)" +
                " VALUES (?,?,?,?)";
            sample_insert_int =
                "INSERT INTO " + schema + sample + " " +
                " (channel_id, smpl_time, severity_id, status_id, num_val)" +
                " VALUES (?,?,?,?,?)";
            sample_insert_string =
                "INSERT INTO " + schema + sample + " " +
                " (channel_id, smpl_time, severity_id, status_id, str_val)" +
                " VALUES (?,?,?,?,?)";
            break;
        case PostgreSQL:
            // Nanosecs are listed last to preserve the order of common columns
            sample_insert_double_blob =
                "INSERT INTO " + schema + sample + " " +
                "(channel_id, smpl_time, severity_id, status_id, float_val, nanosecs, datatype, array_val)" +
                " VALUES (?,?,?,?,?,?,?,?)";
            sample_insert_double =
                "INSERT INTO " + schema + sample + " " +
                "(channel_id, smpl_time, severity_id, status_id, float_val, nanosecs)" +
                " VALUES (?,?,?,?,?,?)";
            sample_insert_double_array_element =
                "INSERT INTO " + schema + "array_val " +
                "(channel_id, smpl_time,  seq_nbr, float_val, nanosecs)" +
                " VALUES (?,?,?,?,?)";
            sample_insert_int =
                "INSERT INTO " + schema + sample + " " +
                "(channel_id, smpl_time, severity_id, status_id, num_val, nanosecs)" +
                " VALUES (?,?,?,?,?,?)";
            sample_insert_string =
                "INSERT INTO " + schema + sample + " " +
                "(channel_id, smpl_time, severity_id, status_id, str_val, nanosecs)" +
                " VALUES (?,?,?,?,?,?)";
            break;
        case MySQL:
            // channel_id, smpl_time, severity_id, status_id are common columns.
            // Param 5 changes depending on the data type.
            // Nanosecs must be param 6 to preserve the order of common columns.
            sample_insert_double_blob =
                "INSERT INTO " + schema + sample + " " +
                "(channel_id, smpl_time, severity_id, status_id, float_val, nanosecs, datatype, array_val)" +
                " VALUES (?,?,?,?,?,?,?,?)";
            sample_insert_double =
                "INSERT INTO " + schema + sample + " " +
                "(channel_id, smpl_time, severity_id, status_id, float_val, nanosecs)" +
                " VALUES (?,?,?,?,?,?)";
            sample_insert_double_array_element =
                "INSERT INTO " + schema + "array_val " +
                "(channel_id, smpl_time, seq_nbr, float_val, nanosecs)" +
                " VALUES (?,?,?,?,?)";
            sample_insert_int =
                "INSERT INTO " + schema + sample + " " +
                "(channel_id, smpl_time, severity_id, status_id, num_val, nanosecs)" +
                " VALUES (?,?,?,?,?,?)";
            sample_insert_string =
                "INSERT INTO " + schema + sample + " " +
                "(channel_id, smpl_time, severity_id, status_id, str_val, nanosecs)" +
                " VALUES (?,?,?,?,?,?)";
            break;

        default:
             throw new Error("Unknown RDB Dialect " + dialect);
        }
    }
}
