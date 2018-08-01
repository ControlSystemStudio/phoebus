/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.config;

import static org.csstudio.archive.Engine.logger;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.archive.Preferences;
import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.Enablement;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.model.SampleMode;
import org.csstudio.archive.writer.rdb.TimestampHelper;
import org.phoebus.framework.rdb.RDBInfo;

@SuppressWarnings("nls")
public class RDBConfig implements AutoCloseable
{
    private final EngineModel model;
    private final RDBInfo rdb;
    private final SQL sql;
    private final Connection connection;

    /** @param model {@link EngineModel} to configure
     *  @throws Exception on error
     */
    public RDBConfig(final EngineModel model) throws Exception
    {
        this.model = model;
        rdb = new RDBInfo(Preferences.url, Preferences.user, Preferences.password);
        sql = new SQL(rdb.getDialect(), Preferences.schema);
        connection = rdb.connect();
    }

    /** List available configuration names
     *  @return Information for each configuration
     *  @throws Exception on error
     */
    public List<String> list() throws Exception
    {
        final List<String> configs = new ArrayList<>();

        try
        (
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(sql.smpl_eng_list);
        )
        {   // eng_id, name, descr, url
            while (result.next())
                configs.add(String.format("%3d %-20s %-40s %-30s",
                                          result.getInt(1), result.getString(2),
                                          result.getString(3), result.getString(4)));
        }
        return configs;
    }


    /** @param config_name Name of engine config to delete
     *  @throws Exception on error
     */
    public void delete(final String config_name) throws Exception
    {
        // Find engine
        final int engine_id;
        try
        (
            PreparedStatement statement = connection.prepareStatement(sql.smpl_eng_sel_by_name);
        )
        {
            statement.setString(1, config_name);
            ResultSet result = statement.executeQuery();
            if (result.next())
                engine_id = result.getInt(1);
            else
                engine_id = -1;
            result.close();
        }

        if (engine_id < 0)
        {
            System.out.println("Engine config '" + config_name + "' does not exist");
            return;
        }

        // Unlink all channels from engine's groups
        connection.setAutoCommit(false);
        try
        {
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.channel_clear_grp_for_engine);
            )
            {
                statement.setInt(1, engine_id);
                statement.executeUpdate();
            }
            // Delete all groups under engine...
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.chan_grp_delete_by_engine_id);
            )
            {
                statement.setInt(1, engine_id);
                statement.executeUpdate();
            }
            // Delete Engine entry
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.smpl_eng_delete);
            )
            {
                statement.setInt(1, engine_id);
                statement.executeUpdate();
            }
            connection.commit();
        }
        catch (Exception ex)
        {
            connection.rollback();
            throw ex;
        }
        finally
        {
            connection.setAutoCommit(true);
        }
    }


    /** Read configuration of model from RDB.
     *  @param config_name Name of engine config
     *  @param port Current HTTPD port
     *  @param skip_last Skip reading last sample time
     *  @throws Exception on error
     */
    public void read(final String config_name, final int port, final boolean skip_last) throws Exception
    {
        logger.info("Archive Configuration '" + config_name + "'");
        logger.info("RDB: " + Preferences.url);
        logger.info("User: " + Preferences.user);

        final int id;
        try
        (
            PreparedStatement stmt = connection.prepareStatement(sql.smpl_eng_sel_by_name);
        )
        {
            stmt.setString(1, config_name);
            final ResultSet result = stmt.executeQuery();
            if (! result.next())
                throw new Exception("Unknown archive engine '" + config_name + "'");

            id = result.getInt(1);
            final URI url = new URI(result.getString(3));
            logger.info("ID: " + id);
            logger.info("Description: " + result.getString(2));
            logger.info("Web Server : " + url);
            result.close();

            if (url.getPort() != port)
                throw new Exception("Engine running on port " + port +
                        " while configuration requires " + url);
        }

        readGroups(id, skip_last);
    }

    /** @param engine_id Engine for which to read groups and their channels
     *  @param skip_last Skip reading last sample time
     *  @throws Exception on error
     */
    private void readGroups(final int engine_id, final boolean skip_last) throws Exception
    {
        try
        (
            PreparedStatement sel_groups = connection.prepareStatement(sql.chan_grp_sel_by_eng_id);
            PreparedStatement sel_chann = connection.prepareStatement(sql.channel_sel_by_group_id);
            PreparedStatement sel_last_sample_time = connection.prepareStatement(sql.sel_last_sample_time_by_id);
        )
        {
            sel_groups.setInt(1, engine_id);
            final ResultSet grp_result = sel_groups.executeQuery();
            while (grp_result.next())
            {
                final int grp_id = grp_result.getInt(1);
                final String grp_name = grp_result.getString(2);
                final int enabling_chan_id = grp_result.getInt(3);
                logger.log(Level.INFO, "Group '" + grp_name + "' (" + grp_id + ")");

                // Add channels to group
                final ArchiveGroup group = model.addGroup(grp_name);

                sel_chann.setInt(1, grp_id);
                final ResultSet chann_result = sel_chann.executeQuery();
                while (chann_result.next())
                {
                    final int channel_id = chann_result.getInt(1);
                    final String name = chann_result.getString(2);
                    final int smpl_mode_id = Math.max(1, chann_result.getInt(3));
                    final double smpl_val = chann_result.getDouble(4);
                    final double smpl_per = chann_result.getDouble(5);

                    Instant last_sample_time = null;
                    if (! skip_last)
                    {
                        sel_last_sample_time.setInt(1, channel_id);
                        final ResultSet result = sel_last_sample_time.executeQuery();
                        if (result.next())
                            last_sample_time = TimestampHelper.fromSQLTimestamp(result.getTimestamp(1));
                        result.close();
                    }

                    Enablement enablement = Enablement.Passive;
                    if (channel_id == enabling_chan_id)
                        enablement = Enablement.Enabling;
                    final SampleMode sample_mode = new SampleMode(smpl_mode_id == 2, smpl_val,  smpl_per);

                    logger.log(Level.INFO, "Channel '" + name + "' (" + channel_id + "), " + sample_mode +
                                           (last_sample_time != null ? ", last written " + last_sample_time : ""));
                    model.addChannel(name, group, enablement, sample_mode, last_sample_time);
                }
                chann_result.close();

            }
            grp_result.close();
        }
    }

    @Override
    public void close() throws Exception
    {
        connection.close();
    }
}
