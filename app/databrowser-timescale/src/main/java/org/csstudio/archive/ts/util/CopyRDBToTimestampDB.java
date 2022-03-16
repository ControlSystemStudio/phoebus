/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.ts.util;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

import org.phoebus.framework.rdb.RDBInfo;

/** Example for importing samples from plain RDB
 *
 *  <p>Severity, status and smpl_mode tables must match
 *  because severity_id and status_id values are simply copied.
 *
 *  <p>Channels in destination database (TimescaleDB)
 *  are created, and only new data is added after checking
 *  the most recent sample that's already in the destination database.
 *
 *  Run from within IDE, or start product as
 *
 *  phoebus.sh -main org.csstudio.archive.ts.util.CopyRDBToTimestampDB /path/to/copy_rdb_to_ts.ini
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CopyRDBToTimestampDB
{
    private final RDBInfo src, dest;
    private final Connection sc, dc;
    private int channel_count = 0;

    private final Timestamp start;
    private final Timestamp end;

    private final int batch_size = 500;


    CopyRDBToTimestampDB(final Properties settings) throws Exception
    {
        start = Timestamp.valueOf(settings.getProperty("start"));
        end = Timestamp.valueOf(settings.getProperty("end"));

        System.out.println("Copying data from " + start + " to " + end);

        src = new RDBInfo(settings.getProperty("source_url"),
                          settings.getProperty("source_user"),
                          settings.getProperty("source_password"));
        dest = new RDBInfo(settings.getProperty("dest_url"),
                           settings.getProperty("dest_user"),
                           settings.getProperty("dest_password"));

        sc = src.connect();
        dc = dest.connect();

        DatabaseMetaData db = sc.getMetaData();
        System.out.println("Source " + db.getDatabaseProductName() + " " + db.getDatabaseProductVersion());

        db = dc.getMetaData();
        System.out.println("Destination " + db.getDatabaseProductName() + " " + db.getDatabaseProductVersion());

        try ( PreparedStatement statement = sc.prepareStatement("SELECT name, channel_id FROM CHAN_ARCH.channel ORDER BY name") )
        {
            statement.setFetchDirection(ResultSet.FETCH_FORWARD);
            statement.setFetchSize(10000);
            try ( ResultSet result = statement.executeQuery() )
            {
                while (result.next())
                {
                    final String name = result.getString(1);
                    // Skip known bad names
                    if (name.startsWith(" ")  ||
                        name.startsWith("\""))
                        continue;
                    copy(name, result.getInt(2));
                }
            }
        }
    }

    private void copy(final String name, final int src_id) throws Exception
    {
        ++channel_count;
        System.out.printf("%6d: '%s' [%d] ",  channel_count, name, src_id);
        System.out.flush();

        final int dest_id = getOrCreateChannel(name);
        System.out.printf("-> [%d] ... ",  dest_id);
        updateChannelSettings(name, src_id, dest_id);

        copyMeta(name, src_id, dest_id);
        System.out.flush();

        // Are there already copied samples for that channel?
        Timestamp actual_start = start;
        try
        (
            // Or better SELECT smpl_time FROM sample WHERE channel_id =10 ORDER BY smpl_time DESC LIMIT 1
            PreparedStatement existing = dc.prepareStatement(
                "SELECT max(smpl_time) FROM sample WHERE channel_id = ?");
        )
        {
            existing.setInt(1, dest_id);
            try ( ResultSet result = existing.executeQuery() )
            {
                if (result.next())
                {
                    // Start 100ms after that last sample
                    final Timestamp last = result.getTimestamp(1);
                    if (result.wasNull())
                        actual_start = start;
                    else
                        actual_start  = Timestamp.from(last.toInstant().plus(100, ChronoUnit.MILLIS));
                    System.out.print("(found " + last + ", looking from " + actual_start + " on) ");
                    System.out.flush();
                }
            }
        }

        try
        (
            PreparedStatement read = sc.prepareStatement(
                "SELECT /*+ parallel full(sample) push_pred*/ smpl_time, severity_id, status_id, num_val, float_val, str_val" +
                " FROM CHAN_ARCH.sample" +
                " WHERE channel_id = ?  AND smpl_time >= ? AND smpl_time < ?" +
                " ORDER BY smpl_time");

            PreparedStatement write = dc.prepareStatement(
                "INSERT INTO sample(smpl_time, nanosecs, channel_id, severity_id, status_id, num_val, float_val, str_val) VALUES (?,?,?,?,?,?,?,?)")
        )
        {
            read.setFetchDirection(ResultSet.FETCH_FORWARD);
            read.setFetchSize(10000);

            read.setInt(1, src_id);
            read.setTimestamp(2, actual_start);
            read.setTimestamp(3, end);


            int samples = 0, batch = 0;
            try ( ResultSet result = read.executeQuery() )
            {
                while (result.next())
                {
                    // Time
                    write.setTimestamp(1, result.getTimestamp(1));
                    write.setInt(2, result.getTimestamp(1).getNanos());
                    // Channel
                    write.setInt(3, dest_id);
                    // Sev, Stat
                    write.setInt(4, result.getInt(2));
                    write.setInt(5, result.getInt(3));

                    // num_val
                    write.setInt(6, result.getInt(4));
                    if (result.wasNull())
                        write.setNull(6, Types.INTEGER);

                    // float_val
                    write.setDouble(7, result.getDouble(5));
                    if (result.wasNull())
                        write.setNull(7, Types.DOUBLE);

                    // str_val
                    write.setString(8, result.getString(6));
                    if (result.wasNull())
                        write.setNull(8, Types.VARCHAR);

                    // Batch update
                    write.addBatch();
                    ++batch;

                    // Commit when reaching batch size
                    if (batch >= batch_size)
                    {
                        write.executeBatch();
                        batch = 0;
                    }

                    ++samples;
                }

                // Commit remaining batched entries
                if (batch > 0)
                {
                    write.executeBatch();
                    batch = 0;
                }
            }
            System.out.printf("%d samples\n",  samples);
        }
    }

    /** Copy channel settings (sampling, retention)
     *  @param name Channel name
     *  @param src_id Original channel ID
     *  @param dest_id New channel ID
     *  @throws Exception on error
     */
    private void updateChannelSettings(final String name, final int src_id, final int dest_id) throws Exception
    {
        try
        (
            PreparedStatement read = sc.prepareStatement(
                "SELECT descr, smpl_mode_id, smpl_val, smpl_per, retent_id, retent_val FROM CHAN_ARCH.channel WHERE channel_id=?");
            PreparedStatement write = dc.prepareStatement(
                "UPDATE channel SET descr=?, smpl_mode_id=?, smpl_val=?, smpl_per=?, retent_id=?, retent_val=? WHERE channel_id=?");
        )
        {
            read.setInt(1, src_id);
            try ( ResultSet result = read.executeQuery() )
            {
                if (result.next())
                {
                    write.setString(1, result.getString(1));
                    write.setInt(2,    result.getInt(2));
                    write.setDouble(3, result.getDouble(3));
                    write.setDouble(4, result.getDouble(4));
                    write.setInt(5,    result.getInt(5));
                    write.setDouble(6, result.getDouble(6));
                    write.setInt(7,    dest_id);
                    write.executeUpdate();
                    System.out.print("update sample settings, ");
                }
            }
        }
        catch (Exception ex)
        {
            throw new Exception("Cannot update settings for channel " + name, ex);
        }
    }

    /** Get 'new' channel ID, create channel if not already known
     *  @param name Channel name
     *  @return New channel ID
     *  @throws Exception on error
     */
    private int getOrCreateChannel(final String name) throws Exception
    {
        try ( PreparedStatement statement = dc.prepareStatement("SELECT channel_id FROM channel WHERE name=?") )
        {
            statement.setString(1, name);
            try ( ResultSet result = statement.executeQuery() )
            {
                if (result.next())
                    return result.getInt(1);
            }
        }


        try ( PreparedStatement statement = dc.prepareStatement("INSERT INTO channel(name) VALUES (?) RETURNING channel_id") )
        {
            statement.setString(1, name);
            try ( ResultSet result = statement.executeQuery() )
            {
                if (result.next())
                    return result.getInt(1);
            }
        }

        throw new Exception("Cannot obtain ID for channel '" + name + "'");
    }

    private void copyMeta(final String name, final int src_id, final int dest_id) throws Exception
    {
        try
        (
            PreparedStatement read_num = sc.prepareStatement(
                "SELECT low_disp_rng, high_disp_rng, low_warn_lmt, high_warn_lmt, low_alarm_lmt, high_alarm_lmt, prec, unit " +
                "FROM CHAN_ARCH.num_metadata WHERE channel_id=?");
            PreparedStatement check_num = dc.prepareStatement(
                    "SELECT channel_id FROM num_metadata WHERE channel_id=?");
            PreparedStatement write_num = dc.prepareStatement(
                    "INSERT INTO num_metadata(channel_id, low_disp_rng, high_disp_rng, low_warn_lmt, high_warn_lmt, low_alarm_lmt, high_alarm_lmt, prec, unit) " +
                    "VALUES (?,?,?,?,?,?,?,?,?)");
            PreparedStatement update_num = dc.prepareStatement(
                    "UPDATE num_metadata SET low_disp_rng=?, high_disp_rng=?, low_warn_lmt=?, high_warn_lmt=?, low_alarm_lmt=?, high_alarm_lmt=?, prec=?, unit=? " +
                    "WHERE channel_id=?");

            PreparedStatement read_enum = sc.prepareStatement(
                    "SELECT enum_nbr, enum_val " +
                    "FROM CHAN_ARCH.enum_metadata WHERE channel_id=?");
            PreparedStatement clear_enum = dc.prepareStatement(
                    "DELETE FROM enum_metadata WHERE channel_id=?");
            PreparedStatement write_enum = dc.prepareStatement(
                    "INSERT INTO enum_metadata(channel_id, enum_nbr, enum_val) VALUES(?,?,?)");
        )
        {
            // Numeric?
            read_num.setInt(1, src_id);
            try ( ResultSet result = read_num.executeQuery() )
            {
                if (result.next())
                {
                    // System.out.println(name + " (" + channel_id + ") has numeric metadata...");
                    check_num.setInt(1, dest_id);
                    try (ResultSet check = check_num.executeQuery())
                    {
                        if (!check.next())
                        {
                            write_num.setInt(1, dest_id);
                            write_num.setDouble(2, result.getDouble(1));
                            write_num.setDouble(3, result.getDouble(2));
                            write_num.setDouble(4, result.getDouble(3));
                            write_num.setDouble(5, result.getDouble(4));
                            write_num.setDouble(6, result.getDouble(5));
                            write_num.setDouble(7, result.getDouble(6));
                            write_num.setInt(8, result.getInt(7));
                            write_num.setString(9, result.getString(8));
                            write_num.executeUpdate();
                            System.out.print("numeric metadata inserted ");
                        }
                        else
                        {
                            update_num.setDouble(1, result.getDouble(1));
                            update_num.setDouble(2, result.getDouble(2));
                            update_num.setDouble(3, result.getDouble(3));
                            update_num.setDouble(4, result.getDouble(4));
                            update_num.setDouble(5, result.getDouble(5));
                            update_num.setDouble(6, result.getDouble(6));
                            update_num.setInt(7, result.getInt(7));
                            update_num.setString(8, result.getString(8));
                            update_num.setInt(9, dest_id);
                            update_num.executeUpdate();
                            System.out.print("numeric metadata updated ");
                        }
                    }

                    return;
                }
            }

            // Enumerated?
            read_enum.setInt(1, src_id);
            try ( ResultSet result = read_enum.executeQuery() )
            {
                if (result.next())
                {
                    // System.out.println(name + " (" + channel_id + ") has enumerated metadata...");
                    clear_enum.setInt(1, dest_id);
                    clear_enum.executeUpdate();

                    int i=0;
                    do
                    {
                        ++i;
                        write_enum.setInt(1, dest_id);
                        write_enum.setInt(2, result.getInt(1));
                        write_enum.setString(3, result.getString(2));
                        write_enum.executeUpdate();
                    }
                    while (result.next());
                    System.out.print(i + " enum labels set ");

                    return;
                }
            }

            // String, or erroneously lacking metadata
            System.out.print("no metadata ");
        }
    }



    public static void main(final String[] args) throws Exception
    {
        if (args.length == 1)
        {
            final Properties settings = new Properties();
            settings.load(new FileInputStream(args[0]));
            new CopyRDBToTimestampDB(settings);
        }
        else
            System.out.println("USAGE: CopyRDBToTimestampDB copy_rdb_to_ts.ini");
    }
}
