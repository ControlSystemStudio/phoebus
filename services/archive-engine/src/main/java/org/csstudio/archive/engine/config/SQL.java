/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.config;

import org.phoebus.framework.rdb.RDBInfo;

/** All the SQL strings in one place.
 *  @author Kay Kasemir
 *  @author Lana Abadie (PostgreSQL in original org.csstudio.archive.rdb code)
 */
@SuppressWarnings("nls")
public class SQL
{
    // 'smpl_eng' table
    final public String smpl_eng_list;
    final public String smpl_eng_sel_by_name;
    final public String smpl_eng_delete;
    final public String smpl_eng_insert;
    final public String smpl_eng_next_id;
    final public String smpl_eng_sel_by_group_id;

    // 'chan_grp' table
    final public String chan_grp_sel_by_eng_id;
    final public String chan_grp_delete_by_engine_id;
    final public String chan_grp_insert;
    final public String chan_grp_next_id;
    final public String chan_grp_sel_by_channel;
    final public String chan_grp_set_enable_channel;

    // 'channel' table
    final public String channel_sel_by_group_id;
    final public String channel_sel_by_id;
    final public String channel_sel_by_name;
    final public String channel_clear_grp_for_engine;
    final public String channel_next_id;
    final public String channel_insert;
    final public String channel_update;

    // 'sample mode' table
    final public String sample_mode_sel;

    // 'sample' table
    final public String sel_last_sample_time_by_id;

    /** Initialize
     *  @param dialect RDB dialect
     *  @param schema Schema prefix, does not include "."
     */
    public SQL(final RDBInfo.Dialect dialect, String schema)
    {
        if (schema.length() > 0)
            schema = schema + ".";
        // 'smpl_eng' table
        smpl_eng_list = "SELECT eng_id, name, descr, url FROM " + schema + "smpl_eng";
        smpl_eng_sel_by_name = "SELECT eng_id, descr, url FROM " + schema + "smpl_eng WHERE name=?";
        smpl_eng_delete = "DELETE FROM " + schema + "smpl_eng WHERE eng_id=?";
        smpl_eng_insert = "INSERT INTO " + schema + "smpl_eng(eng_id, name, descr, url) VALUES (?,?,?,?)";
        smpl_eng_next_id = "SELECT MAX(eng_id) FROM " + schema + "smpl_eng";
        smpl_eng_sel_by_group_id = "SELECT e.name, e.descr, e.url, e.eng_id FROM " + schema + "smpl_eng e " +
                                   "JOIN " + schema + "chan_grp g ON e.eng_id = g.eng_id " +
                                   "WHERE g.grp_id=?";

        // 'chan_grp' table
        chan_grp_sel_by_eng_id = "SELECT grp_id, name, enabling_chan_id FROM " + schema + "chan_grp WHERE eng_id=? ORDER BY name";
        chan_grp_delete_by_engine_id = "DELETE FROM " + schema + "chan_grp WHERE eng_id=?";
        chan_grp_insert = "INSERT INTO " + schema + "chan_grp (grp_id, name, eng_id, enabling_chan_id) VALUES (?,?,?,null)";
        chan_grp_next_id = "SELECT MAX(grp_id) FROM " + schema + "chan_grp";
        chan_grp_sel_by_channel = "SELECT g.grp_id, g.name FROM " + schema + "chan_grp g " +
                                  "JOIN " + schema + "channel c ON g.grp_id = c.grp_id " +
                                  "WHERE c.name=?";
        chan_grp_set_enable_channel = "UPDATE " + schema + "chan_grp SET enabling_chan_id=? WHERE grp_id=?";

        // 'channel' table
        channel_sel_by_group_id = "SELECT channel_id, name, smpl_mode_id, smpl_val, smpl_per FROM " + schema + "channel WHERE grp_id=? ORDER BY name";
        channel_sel_by_id = "SELECT name FROM " + schema + "channel WHERE channel_id=?";
        channel_sel_by_name = "SELECT channel_id FROM " + schema + "channel WHERE name=?";
        channel_clear_grp_for_engine =
            "UPDATE " + schema + "channel SET grp_id=null WHERE grp_id IN " +
                "(SELECT grp_id FROM " + schema + "chan_grp WHERE eng_id=?)";
        channel_next_id = "SELECT MAX(channel_id) FROM " + schema + "channel";
        channel_insert = "INSERT INTO " + schema + "channel(grp_id, name, smpl_mode_id, smpl_val, smpl_per, channel_id) VALUES (?,?,?,?,?,?)";
        channel_update = "UPDATE " + schema + "channel SET grp_id=?, name=?, smpl_mode_id=?, smpl_val=?, smpl_per=? WHERE channel_id=?";

        // 'sample mode' table
        sample_mode_sel = "SELECT smpl_mode_id, name FROM " + schema + "smpl_mode";

        // 'sample' table
        sel_last_sample_time_by_id = "SELECT MAX(smpl_time) FROM " + schema + "sample WHERE channel_id=?";
    }
}
