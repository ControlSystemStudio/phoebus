/*******************************************************************************
 * Copyright (c) 2021-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * are made available under the terms of the Eclipse Public License v1.0
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.ts.reader;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.csstudio.archive.ts.Preferences;
import org.csstudio.trends.databrowser3.imports.ArrayValueIterator;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VEnum;
import org.epics.vtype.VInt;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.framework.rdb.RDBConnectionPool;
import org.phoebus.pv.PVPool;
import org.phoebus.util.time.TimestampFormats;

/** Archive reader for TimestampDB
 *
 *  <p>Supports raw readout.
 *  Calls SQL function for optimized readout.
 *  Adds "#1234" for name lookup by channel name.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TSArchiveReader implements ArchiveReader
{
    /** Connection pool */
    private final RDBConnectionPool pool;

    private final SQL sql = new SQL();

    /** Active statements to cancel in cancel() */
    private final List<Statement> cancellable_statements = new ArrayList<>();

    /** Map of severity IDs to Severities */
    private static final ConcurrentHashMap<Integer, AlarmSeverity> severities = new ConcurrentHashMap<>();

    /** Map of status IDs to Status strings */
    private static final ConcurrentHashMap<Integer, String> stati = new ConcurrentHashMap<>();

    /** @param url Database URL
     *  @throws Exception on error
     */
    public TSArchiveReader(final String url) throws Exception
    {
        pool = new RDBConnectionPool(url.substring(TSArchiveReaderFactory.PREFIX.length()),
                                     Preferences.user,
                                     Preferences.password);

        // Assert that sevr/stat are only initialized once.
        // Further access is read-only and doesn't need to synchronize,
        // but just in case we use ConcurrentHashMap
        synchronized (severities)
        {
            try
            {
                if (severities.isEmpty())
                    readSeverityOptions();
                if (stati.isEmpty())
                    readStatusOptions();
            }
            catch (Exception ex)
            {
                // Clear options so next time around we'll try again.
                severities.clear();
                stati.clear();
                throw ex;
            }
        }
    }

    @Override
    public String getDescription()
    {
        return "TimescaleDB Data Source";
    }

    /** @return Connection pool */
    RDBConnectionPool getPool()
    {
        return pool;
    }

    SQL getSQL()
    {
        return sql;
    }

    /** Add a statement to the list of statements-to-cancel in cancel()
     *  @param statement Statement to cancel
     *  @see #cancel()
     */
    void addForCancellation(final Statement statement)
    {
        synchronized (cancellable_statements)
        {
            cancellable_statements.add(statement);
        }
    }

    /** Remove a statement from the list of statements-to-cancel in cancel()
     *  @param statement Statement that should no longer be cancelled
     *  @see #cancel()
     */
    void removeFromCancellation(final Statement statement)
    {
        synchronized (cancellable_statements)
        {
            cancellable_statements.remove(statement);
        }
    }

    private void readSeverityOptions() throws Exception
    {
        logger.log(Level.INFO, "Reading severity options...");
        final Connection connection = pool.getConnection();
        try (final Statement statement = connection.createStatement())
        {
            addForCancellation(statement);
            try (final ResultSet result = statement.executeQuery(sql.sel_severities))
            {
                while (result.next())
                    severities.put(result.getInt(1), decodeAlarmSeverity(result.getString(2)));
            }
            finally
            {
                removeFromCancellation(statement);
            }
        }
        finally
        {
            pool.releaseConnection(connection);
        }
        logger.log(Level.INFO, "Severity options: " + severities);
    }

    private static AlarmSeverity decodeAlarmSeverity(final String text)
    {
        for (AlarmSeverity s : AlarmSeverity.values())
        {
            if (text.startsWith(s.name()))
                return s;
        }
        if ("OK".equalsIgnoreCase(text) || "".equalsIgnoreCase(text))
            return AlarmSeverity.NONE;
        logger.log(Level.FINE, "Undefined severity level {0}", text);
        return AlarmSeverity.UNDEFINED;
    }

    private void readStatusOptions() throws Exception
    {
        logger.log(Level.INFO, "Reading status options...");
        final Connection connection = pool.getConnection();
        try (final Statement statement = connection.createStatement())
        {
            addForCancellation(statement);
            try (final ResultSet result = statement.executeQuery(sql.sel_stati))
            {
                while (result.next())
                    stati.put(result.getInt(1), result.getString(2));
            }
            finally
            {
                removeFromCancellation(statement);
            }
        }
        finally
        {
            pool.releaseConnection(connection);
        }
        logger.log(Level.INFO, "Status options: " + stati);
    }

    @Override
    public Collection<String> getNamesByPattern(final String glob_pattern) throws Exception
    {
        // Escape underscores because they are SQL patterns
        String sql_pattern = glob_pattern.replace("_", "\\_");
        // Glob '?' -> SQL '_'
        sql_pattern = sql_pattern.replace('?', '_');
        // Glob '*' -> SQL '%'
        sql_pattern = sql_pattern.replace('*', '%');

        final List<String> names = new ArrayList<>();

        final Connection connection = pool.getConnection();
        try
        {
            PreparedStatement statement = null;
            try
            {
                // Special handling of "#1234" to lookup channel by ID 1234
                if (glob_pattern.matches("#[0-9]+"))
                {
                    statement = connection.prepareStatement("SELECT name FROM channel WHERE channel_id=?");
                    statement.setInt(1,  Integer.parseInt(glob_pattern.substring(1)));
                }
                else
                {
                    statement = connection.prepareStatement(sql.channel_sel_by_like);
                    statement.setString(1, sql_pattern);
                }

                addForCancellation(statement);
                try (final ResultSet result = statement.executeQuery())
                {
                    while (result.next())
                        names.add(result.getString(1));
                }
                finally
                {
                    removeFromCancellation(statement);
                }
            }
            catch (Exception ex)
            {
                if (ex.getMessage().contains("user request"))
                {
                    // Ignore Oracle/PostgreSQL error: user requested cancel of current operation
                }
                else
                    throw ex;
            }
            finally
            {
                if (statement != null)
                    statement.close();
            }
        }
        finally
        {
            pool.releaseConnection(connection);
        }

        return names;
    }

    @Override
    public ValueIterator getRawValues(final String name, Instant start, final Instant end)
            throws UnknownChannelException, Exception
    {
        final int channel_id = getChannelID(name);
        start = determineActualStart(channel_id, start);
        return new TSRawSampleIterator(this, channel_id, start, end);
    }

    // [org.csstudio.archive.timescaledb.reader] Optimized request found 28627028 raw samples
    // [org.csstudio.trends.databrowser3] Ended Read data: BTF_MEBT_Mag:PS_DCH01:I, 2018-12-16 17:34:40.059307490 - 2020-01-04 02:36:06.394053358 with 6748 samples in 168 secs
    // --> RDB handled ~170000 raw samples/sec, reducing them to just ~7000 min/max/average values

    // When re-run, MUCH faster:
    // [org.csstudio.archive.timescaledb.reader] Optimized request found 28627028 raw samples
    // [org.csstudio.trends.databrowser3] Ended Read data: BTF_MEBT_Mag:PS_DCH01:I, 2018-12-16 17:34:40.059000000 - 2020-01-04 02:36:06.394000000 with 6748 samples in 6 secs
    @Override
    public ValueIterator getOptimizedValues(final String name, Instant start, final Instant end,
                                            final int count) throws UnknownChannelException, Exception
    {
        final int channel_id = getChannelID(name);
        final DisplayInfo display = DisplayInfo.forChannel(channel_id, this);

        logger.log(Level.FINE, () -> name + ": " + count + " buckets");

        final List<VType> values = new ArrayList<>();
        final Connection connection = pool.getConnection();
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
            //      1       2            3          4    5    6    7        8        9
            "SELECT bucket, severity_id, status_id, min, max, avg, num_val, str_val, n FROM auto_optimize(?, ?::TIMESTAMPTZ, ?::TIMESTAMPTZ, ?)")
        )
        {
            statement.setFetchDirection(ResultSet.FETCH_FORWARD);
            statement.setFetchSize(Preferences.fetch_size);
            statement.setInt(1, channel_id);
            statement.setTimestamp(2, Timestamp.from(start));
            statement.setTimestamp(3, Timestamp.from(end));
            statement.setLong(4, count);

            addForCancellation(statement);

            // final VTypeFormat format = DoubleVTypeFormat.get();
            try (final ResultSet result = statement.executeQuery())
            {
                while (result.next())
                {
                    final Instant stamp = result.getTimestamp(1).toInstant();
                    final int N = result.getInt(9);
                    final VType value;

                    // Is there a string?
                    final String text = result.getString(8);
                    if (!result.wasNull() && text != null)
                    {
                        // Read severity, status unless statistics
                        final Alarm alarm = decodeAlarm(result.getInt(2), result.getInt(3));
                        value = VString.of(text, alarm, Time.of(stamp));
                    }
                    else
                    {   // Is it an integer?
                        final int num_val = result.getInt(7);
                        if (!result.wasNull())
                        {
                            final Alarm alarm = decodeAlarm(result.getInt(2), result.getInt(3));
                            // Check for enum or numeric
                            if (display.getLabels() != null)
                                value = VEnum.of(num_val, display.getLabels(), alarm, Time.of(stamp));
                            else
                                value = VInt.of(num_val, alarm, Time.of(stamp), display.getDisplay());
                        }
                        else if (N==1)
                            // 'raw' double sample
                            value = VDouble.of(result.getDouble(6),
                                               Alarm.none(),
                                               Time.of(stamp),
                                               display.getDisplay());
                        else
                            // Optimized min/max/avg sample
                            value = VStatistics.of(result.getDouble(6),
                                                   Double.NaN,
                                                   result.getDouble(4),
                                                   result.getDouble(5),
                                                   N,
                                                   Alarm.none(),
                                                   Time.of(stamp),
                                                   display.getDisplay());
                    }
                    values.add(value);
                    // System.out.println(TimestampFormats.FULL_FORMAT.format(((TimeProvider)value).getTime().getTimestamp()) +
                    //                    " " + format.format(value));
                }
            }
            finally
            {
                removeFromCancellation(statement);
            }
        }
        finally
        {
            pool.releaseConnection(connection);
        }

        return new ArrayValueIterator(values);
    }

    /** @param name Channel name
     *  @return Numeric channel ID
     *  @throws UnknownChannelException when channel not known
     *  @throws Exception on error
     */
    int getChannelID(final String name) throws UnknownChannelException, Exception
    {
        final Connection connection = pool.getConnection();
        try (final PreparedStatement statement = connection.prepareStatement(sql.channel_sel_by_name))
        {
            addForCancellation(statement);
            try
            {
                if (Preferences.timeout_secs > 0)
                    statement.setQueryTimeout(Preferences.timeout_secs);
                // Loop over variants
                for (String variant : PVPool.getNameVariants(name, org.csstudio.trends.databrowser3.preferences.Preferences.equivalent_pv_prefixes))
                {
                    statement.setString(1, variant);
                    try (final ResultSet result = statement.executeQuery())
                    {
                        if (result.next())
                        {
                            final int channel_id = result.getInt(1);
                            logger.log(Level.FINE, () -> "Found '" + name + "' as '" + variant + "' (" + channel_id + ")");
                            return channel_id;
                        }
                    }
                }
                // Nothing found
                throw new UnknownChannelException(name);
            }
            finally
            {
                removeFromCancellation(statement);
            }
        }
        finally
        {
            pool.releaseConnection(connection);
        }
    }

    /** Get actual start time
     *  @param channel_id Channel ID
     *  @param start Requested Start time
     *  @return Time of last sample at-or-before start
     *  @throws Exception on error, including cancellation
     */
    private Instant determineActualStart(final int channel_id, Instant start) throws Exception
    {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Original start time: " + TimestampFormats.FULL_FORMAT.format(start));

        // Get time of initial sample
        Connection connection = pool.getConnection();
        try (final PreparedStatement statement = connection.prepareStatement(sql.sample_sel_initial_time))
        {
            addForCancellation(statement);
            statement.setInt(1, channel_id);
            statement.setTimestamp(2, Timestamp.from(start));
            // Ignore nanosecs in query. Can only select start time at smpl_time granularity.
            try (final ResultSet result = statement.executeQuery())
            {
                if (result.next())
                {
                    final Timestamp actual_start = result.getTimestamp(1);
                    if (actual_start != null  &&  !result.wasNull())
                    {
                        actual_start.setNanos(result.getInt(2));
                        start = actual_start.toInstant();
                    }
                }
            }
            finally
            {
                removeFromCancellation(statement);
            }
        }
        finally
        {
            pool.releaseConnection(connection);
        }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Actual start time  : " + TimestampFormats.FULL_FORMAT.format(start));
        return start;
    }

    /** Decode severity and status IDs into alarm
     *  @param severity_id Severity ID
     *  @param status_id Status ID
     *  @return {@link Alarm}
     */
    Alarm decodeAlarm(final int severity_id, final int status_id)
    {
        // Decode numeric IDs into values
        AlarmSeverity severity = severities.computeIfAbsent(severity_id, id ->
        {
            logger.log(Level.WARNING, "Undefined severity ID " + id);
            return AlarmSeverity.UNDEFINED;
        });
        final String status = stati.computeIfAbsent(status_id, id ->
        {
            logger.log(Level.WARNING, "Undefined status ID " + id);
            return "<" + id + ">";
        });

        // Hard-coded knowledge:
        // When the status indicates
        // that the archive is off or channel was disconnected,
        // we use the special severity that marks a sample
        // without a value.
        if (status.equalsIgnoreCase("Archive_Off") ||
            status.equalsIgnoreCase("Disconnected") ||
            status.equalsIgnoreCase("Write_Error"))
            severity = AlarmSeverity.UNDEFINED;

        return Alarm.of(severity, AlarmStatus.CLIENT, status);
    }

    @Override
    public void cancel()
    {
        synchronized (cancellable_statements)
        {
            for (Statement statement : cancellable_statements)
            {
                try
                {
                    // Note that
                    //    statement.getConnection().close()
                    // does NOT stop an ongoing Oracle query!
                    // Only this seems to do it:
                    statement.cancel();
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Failed to cancel statement " + statement, ex);
                }
            }
            cancellable_statements.clear();
        }
    };

    @Override
    public void close()
    {
        cancel();
        pool.clear();
    }
}
