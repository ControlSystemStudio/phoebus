/*******************************************************************************
 * Copyright (c) 2021-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * are made available under the terms of the Eclipse Public License v1.0
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.ts.reader;

/** SQL Statements
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SQL
{
    /** Schema prefix */
    public final String prefix = "";

    /** Select numeric meta data */
    public final String numeric_meta_sel_by_channel = "SELECT low_disp_rng, high_disp_rng," +
            " low_warn_lmt, high_warn_lmt," +
            " low_alarm_lmt, high_alarm_lmt," +
            " prec, unit FROM " + prefix + "num_metadata WHERE channel_id=?";

    /** Select data by channel */
    public final String enum_sel_num_val_by_channel = "SELECT enum_nbr, enum_val FROM "
            + prefix + "enum_metadata WHERE channel_id=? ORDER BY enum_nbr";

    /** Find all status values */
    public final String sel_stati = "SELECT status_id, name FROM " + prefix + "status";

    /** Find all severity values */
    public final String sel_severities = "SELECT severity_id, name FROM " + prefix + "severity";

    /** Find channel by pattern */
    public final String channel_sel_by_like = "SELECT name FROM " + prefix + "channel WHERE name LIKE ? ORDER BY name";

    /** Find channel by name */
    public final String channel_sel_by_name = "SELECT channel_id FROM " + prefix + "channel WHERE name=?";

    /** Find time stamp at-or-before start time */
    public final String sample_sel_initial_time = "SELECT smpl_time, nanosecs" +
                                                  "   FROM " + prefix + "sample WHERE channel_id=? AND smpl_time<=?" +
                                                  "   ORDER BY smpl_time DESC, nanosecs DESC LIMIT 1";

    /** Find samples between start and end time */
    public final String sample_sel_by_id_start_end = "SELECT smpl_time, severity_id, status_id, num_val, float_val, str_val, nanosecs, datatype, array_val FROM " + prefix + "sample" +
                                                     "   WHERE channel_id=?" +
                                                     "     AND smpl_time BETWEEN ? AND ?" +
                                                     "   ORDER BY smpl_time, nanosecs";
}
