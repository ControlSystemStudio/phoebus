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
    private final RDBInfo rdb;
    private final SQL sql;
    private final Connection connection;
    private int scan_mode_id = 0;
    private int monitor_mode_id = 1;

    /** @throws Exception on error */
    public RDBConfig() throws Exception
    {
        rdb = new RDBInfo(Preferences.url, Preferences.user, Preferences.password);
        sql = new SQL(rdb.getDialect(), Preferences.schema);
        connection = rdb.connect();

        // Determine which mode is 'monitor'
        try
        (
            final Statement stmt = connection.createStatement();
            final ResultSet result = stmt.executeQuery(sql.sample_mode_sel);
        )
        {
            while (result.next())
                if (result.getString(2).equalsIgnoreCase("Monitor"))
                    monitor_mode_id = result.getInt(1);
                else if (result.getString(2).equalsIgnoreCase("Scan"))
                    scan_mode_id = result.getInt(1);
        }
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


    /** @param config_name Name of engine to create (or use existing)
     *  @param description
     *  @param replace_engine If exists, remove existing groups & channels?
     *  @return ID of new or existing engine
     *  @throws Exception on error, which includes finding existing engine without 'replace' option
     */
    public int createEngine(final String config_name, final String description, final boolean replace_engine, final String url) throws Exception
    {
        // Check for existing engine
        int engine_id;
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

        if (engine_id >= 0)
        {
            if (replace_engine)
            {
                logger.log(Level.INFO, "Removing channels and groups from existing '" + config_name + "' (" + engine_id + ")");
                delete(config_name, false);
            }
            else
                throw new Exception("Archive engine '" + config_name + "' (" + engine_id + ") already exists. Use -replace_engine to replace");
        }
        else
        {
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.smpl_eng_next_id);
            )
            {
                ResultSet result = statement.executeQuery();
                if (result.next())
                    engine_id = result.getInt(1) + 1;
                else
                    engine_id = 1;
                result.close();
            }

            logger.log(Level.INFO, "Creating new engine '" + config_name + "' (" + engine_id + ")");
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.smpl_eng_insert);
            )
            {
                statement.setInt(1, engine_id);
                statement.setString(2, config_name);
                statement.setString(3, description);
                statement.setString(4, url);
                statement.executeUpdate();
            }
        }

        return engine_id;
    }

    /** @param engine_id Engine for which to create a new group
     *  @param group_name Name of that group
     *  @return Group ID
     *  @throws Exception on error
     */
    public int createGroup(final int engine_id, final String group_name) throws Exception
    {
        final int group_id;
        try
        (
            PreparedStatement statement = connection.prepareStatement(sql.chan_grp_next_id);
        )
        {
            ResultSet result = statement.executeQuery();
            if (result.next())
                group_id = result.getInt(1) + 1;
            else
                group_id = 1;
            result.close();
        }

        logger.log(Level.INFO, "Creating new group '" + group_name + "' (" + group_id + ")");
        try
        (
            PreparedStatement statement = connection.prepareStatement(sql.chan_grp_insert);
        )
        {
            statement.setInt(1, group_id);
            statement.setString(2, group_name);
            statement.setInt(3, engine_id);
            statement.executeUpdate();
        }

        return group_id;
    }

    /** @param group_id Group where to add channel
     *  @param steal_channels Steal from other group, if channel already attached?
     *  @param name Name of channel
     *  @param monitor
     *  @param period
     *  @param delta
     *  @param enable
     *  @throws Exception on error, including existing channel
     */
    public void addChannel(final int group_id, final boolean steal_channels, final String name,
                           final boolean monitor, final double period, final double delta,
                           final boolean enable) throws Exception
    {
        int channel_id = -1;
        try
        (
            PreparedStatement statement = connection.prepareStatement(sql.channel_sel_by_name);
        )
        {
            statement.setString(1, name);
            ResultSet result = statement.executeQuery();
            if (result.next())
                channel_id = result.getInt(1);
            result.close();
        }

        if (channel_id >= 0)
        {
            // Check if existing channel is simply an old one with data,
            // or currently listed in another engine's group
            Exception existing = null;
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.chan_grp_sel_by_channel);
            )
            {
                statement.setString(1, name);
                ResultSet result = statement.executeQuery();
                if (result.next())
                    existing = new Exception("Channel '" + name + "' is already in group '" + result.getString(2) + "' (" + result.getInt(1) + "). Use option -steal_channels to move to this engine.");
                result.close();
            }
            if (existing != null  &&  !steal_channels)
                throw existing;

            // Update channel to be in new group
            logger.log(Level.INFO, "Updating channel '" + name + "' (" + channel_id + ")");
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.channel_update);
            )
            {
                statement.setInt(1, group_id);
                statement.setString(2, name);
                statement.setInt(3, monitor ? monitor_mode_id : scan_mode_id);
                statement.setDouble(4, delta);
                statement.setDouble(5, period);
                statement.setInt(6, channel_id);
                statement.executeUpdate();
            }
        }
        else
        {   // Create new channel
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.channel_next_id);
            )
            {
                ResultSet result = statement.executeQuery();
                if (result.next())
                    channel_id = result.getInt(1) + 1;
                else
                    channel_id = 1;
                result.close();
            }

            logger.log(Level.INFO, "Adding new channel '" + name + "' (" + channel_id + ")");
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.channel_insert);
            )
            {
                statement.setInt(1, group_id);
                statement.setString(2, name);
                statement.setInt(3, monitor ? monitor_mode_id : scan_mode_id);
                statement.setDouble(4, delta);
                statement.setDouble(5, period);
                statement.setInt(6, channel_id);
                statement.executeUpdate();
            }
        }
    }

    /** @param config_name Name of engine config to delete
     *  @param complete Delete the sample engine entry itself, or leave that after unlinking all groups and channels?
     *  @throws Exception on error
     */
    public void delete(final String config_name, final boolean complete) throws Exception
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
            if (complete)
            {
                try
                (
                    PreparedStatement statement = connection.prepareStatement(sql.smpl_eng_delete);
                )
                {
                    statement.setInt(1, engine_id);
                    statement.executeUpdate();
                }
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
     *  @param model {@link EngineModel} to configure
     *  @param config_name Name of engine config
     *  @param port Current HTTPD port
     *  @param skip_last Skip reading last sample time
     *  @throws Exception on error
     */
    public void read(final EngineModel model, final String config_name, final int port, final boolean skip_last) throws Exception
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

        readGroups(model, id, skip_last);
    }

    /** @param model {@link EngineModel} to configure
     *  @param engine_id Engine for which to read groups and their channels
     *  @param skip_last Skip reading last sample time
     *  @throws Exception on error
     */
    private void readGroups(final EngineModel model, final int engine_id, final boolean skip_last) throws Exception
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
                    final SampleMode sample_mode = new SampleMode(smpl_mode_id == monitor_mode_id, smpl_val,  smpl_per);

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
