/*******************************************************************************
 * Copyright (c) 2011-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

import static org.csstudio.archive.Engine.logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.archive.Preferences;
import org.csstudio.archive.writer.ArchiveWriter;
import org.csstudio.archive.writer.WriteChannel;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VEnum;
import org.epics.vtype.VFloat;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.framework.rdb.RDBInfo;
import org.phoebus.framework.rdb.RDBInfo.Dialect;
import org.phoebus.pv.LongString;

/** ArchiveWriter implementation for RDB
 *  @author Kay Kasemir
 *  @author Lana Abadie - PostgreSQL for original RDBArchive code. Disable autocommit as needed.
 *  @author Laurent Philippe (Use read-only connection when possible for MySQL load balancing)
 */
@SuppressWarnings("nls")
public class RDBArchiveWriter implements ArchiveWriter
{
    /** Status string for <code>Double.NaN</code> samples */
    final private static String NOT_A_NUMBER_STATUS = "NaN";

    final private boolean use_array_blob;

    final private Dialect dialect;

    /** RDB connection */
    final private Connection connection;

    /** SQL statements */
    final private SQL sql;

    /** Cache of channels by name */
    final private Map<String, RDBWriteChannel> channels = new HashMap<>();

    /** Severity (ID, name) cache */
    private SeverityCache severities;

    /** Status (ID, name) cache */
    private StatusCache stati;

    /** Counter for accumulated samples in 'double' batch */
    private int batched_double_inserts = 0;

    /** Counter for accumulated samples in 'double array' batch */
    private int batched_double_array_inserts = 0;

    /** Counter for accumulated samples in 'long' batch */
    private int batched_long_inserts = 0;

    /** Counter for accumulated samples in 'String' batch */
    private int batched_txt_inserts = 0;

    /** Copy of batched samples, used to display batch errors */
    private final List<RDBWriteChannel> batched_channel = new ArrayList<>();
    private final List<VType> batched_samples = new ArrayList<>();


    private final PreparedStatement insert_double_sample, insert_array_sample, insert_long_sample, insert_txt_sample;

    /** Initialize
     *  @param url RDB URL
     *  @param user .. user name
     *  @param password .. password
     *  @param schema Schema/table prefix, not including ".". May be empty
     *  @param use_array_blob Use BLOB for array elements?
     *  @throws Exception on error, for example RDB connection error
     */
    public RDBArchiveWriter(final String url, final String user, final String password,
            final String schema, boolean use_array_blob) throws Exception
    {
        this.use_array_blob = use_array_blob;
        final RDBInfo info = new RDBInfo(url, user, password);
        dialect = info.getDialect();
        connection = info.connect();

        // Contrary to default, operate mostly with auto-commit off
        connection.setAutoCommit(false);

        sql = new SQL(dialect, schema);
        severities = new SeverityCache(connection, sql);
        stati = new StatusCache(connection, sql);

        if (Preferences.use_array_blob)
            insert_double_sample = createInsertPrepareStatement(sql.sample_insert_double_blob);
        else
            insert_double_sample = createInsertPrepareStatement(sql.sample_insert_double);

        insert_array_sample = connection.prepareStatement(sql.sample_insert_double_array_element);
        insert_long_sample = createInsertPrepareStatement(sql.sample_insert_int);
        insert_txt_sample = createInsertPrepareStatement(sql.sample_insert_string);
    }

    /** Create a new prepared statement. For PostgreSQL connections, this method
     *  create a PGCopyPreparedStatement to improve insert speed using COPY
     *  instead of INSERT.
     *
     *  @param sqlQuery
     *  @return
     *  @throws SQLException
     *  @throws Exception
     */
    @SuppressWarnings("resource")
    private PreparedStatement createInsertPrepareStatement(final String sqlQuery)
            throws SQLException, Exception
    {
        final PreparedStatement statement;
        if (dialect == Dialect.PostgreSQL  &&  Preferences.use_postgres_copy)
            statement = new PGCopyPreparedStatement(connection, sqlQuery);
        else
            statement = connection.prepareStatement(sqlQuery);
        if (Preferences.timeout_secs > 0)
            statement.setQueryTimeout(Preferences.timeout_secs);
        return statement;
    }

    @Override
    public WriteChannel getChannel(final String name) throws Exception
    {
        // Check cache
        RDBWriteChannel channel = channels.get(name);
        if (channel == null)
        {    // Get channel information from RDB
            try
            (
                PreparedStatement statement = connection.prepareStatement(sql.channel_sel_by_name);
            )
            {
                statement.setString(1, name);
                final ResultSet result = statement.executeQuery();
                if (!result.next())
                    throw new Exception("Unknown channel " + name);
                channel = new RDBWriteChannel(name, result.getInt(1));
                result.close();
                channels.put(name, channel);
            }
        }
        return channel;
    }

    @Override
    public void addSample(final WriteChannel channel, final VType sample) throws Exception
    {
        final RDBWriteChannel rdb_channel = (RDBWriteChannel) channel;
        writeMetaData(rdb_channel, sample);
        batchSample(rdb_channel, sample);
        batched_channel.add(rdb_channel);
        batched_samples.add(sample);
    }

    /** Write meta data if it was never written or has changed
     *  @param channel Channel for which to write the meta data
     *  @param sample Sample that may have meta data to write
     */
    private void writeMetaData(final RDBWriteChannel channel, final VType sample) throws Exception
    {
        // Note that Strings have no meta data. But we don't know at this point
        // if it's really a string channel, or of this is just a special
        // string value like "disconnected".
        // In order to not delete any existing meta data,
        // we just do nothing for strings

        final Display display = Display.displayOf(sample);
        if (display != null)
        {
            if (MetaDataHelper.equals(display, channel.getMetadata()))
                return;

            // Clear enumerated meta data, replace numeric
            EnumMetaDataHelper.delete(connection, sql, channel);
            NumericMetaDataHelper.delete(connection, sql, channel);
            NumericMetaDataHelper.insert(connection, sql, channel, display);
            channel.setMetaData(display);
        }
        else if (sample instanceof VEnum)
        {
            final List<String> labels = ((VEnum)sample).getDisplay().getChoices();
            if (MetaDataHelper.equals(labels, channel.getMetadata()))
                return;

            // Clear numeric meta data, set enumerated in RDB
            NumericMetaDataHelper.delete(connection, sql, channel);
            EnumMetaDataHelper.delete(connection, sql, channel);
            EnumMetaDataHelper.insert(connection, sql, channel, labels);
            channel.setMetaData(labels);
        }
    }

    private static Instant getTimestamp(final VType value)
    {
        final Time time = Time.timeOf(value);
        if (time != null  &&  time.isValid())
            return time.getTimestamp();
        return Instant.now();
    }

    private static AlarmSeverity getSeverity(final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm == null)
            return AlarmSeverity.NONE;
        return alarm.getSeverity();
    }

    private static String getMessage(final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm == null)
            return "";
        return alarm.getName();
    }

    /** Perform 'batched' insert for sample.
     *  <p>Needs eventual flush()
     *  @param channel Channel
     *  @param sample Sample to insert
     *  @throws Exception on error
     */
    private void batchSample(final RDBWriteChannel channel, final VType sample) throws Exception
    {
        final Timestamp stamp = TimestampHelper.toSQLTimestamp(getTimestamp(sample));
        final int severity = severities.findOrCreate(getSeverity(sample));
        final Status status = stati.findOrCreate(getMessage(sample));

        // Severity/status cache may enable auto-commit
        if (connection.getAutoCommit() == true)
            connection.setAutoCommit(false);

        // Start with most likely cases and highest precision: Double, ...
        // Then going down in precision to integers, finally strings...
        if (sample instanceof VDouble)
            batchDoubleSamples(channel, stamp, severity, status, ((VDouble)sample).getValue(), null);
        else if (sample instanceof VFloat)
            batchDoubleSamples(channel, stamp, severity, status, ((VFloat)sample).getValue(), null);
        else if (sample instanceof VNumber)
        {   // Write as double or integer?
            // VDouble & VFloat are already handled, but check once more
            // in case a custom VNumber returns a floating point type
            final Number number = ((VNumber)sample).getValue();
            if (number instanceof Double  ||  number instanceof Float)
                batchDoubleSamples(channel, stamp, severity, status, number.doubleValue(), null);
            else
                batchLongSample(channel, stamp, severity, status, number.longValue());
        }
        else if (sample instanceof VByteArray)
        {   // Tread byte array as long string.
            // Other number arrays handled below
            final String text = LongString.fromArray((VByteArray)sample);
            batchTextSamples(channel, stamp, severity, status, text);
        }
        else if (sample instanceof VNumberArray)
        {
            final ListNumber data = ((VNumberArray)sample).getData();
            if (data.size() > 0)
                batchDoubleSamples(channel, stamp, severity, status, data.getDouble(0), data);
            else
                batchDoubleSamples(channel, stamp, severity, status, Double.NaN, data);
        }
        else if (sample instanceof VEnum)
            batchLongSample(channel, stamp, severity, status, ((VEnum)sample).getIndex());
        else if (sample instanceof VString)
            batchTextSamples(channel, stamp, severity, status, ((VString)sample).getValue());
        else if (sample instanceof VStringArray)
        {
            // Store as comma-separated elements, omitting blank elements
            final VStringArray strings = (VStringArray) sample;
            final String text = strings.getData()
                                       .stream()
                                       .filter(element -> ! element.isBlank())
                                       .collect(Collectors.joining(", "));
            batchTextSamples(channel, stamp, severity, status, text);
        }
        else // Handle possible other types as strings
            batchTextSamples(channel, stamp, severity, status, sample.toString());
    }

    /** Helper for batchSample: Add double sample(s) to batch. */
    private void batchDoubleSamples(final RDBWriteChannel channel,
            final Timestamp stamp, final int severity,
            final Status status, final double dbl, final ListNumber additional) throws Exception
    {
        if (use_array_blob)
            batchBlobbedDoubleSample(channel, stamp, severity, status, dbl, additional);
        else
            oldBatchDoubleSamples(channel, stamp, severity, status, dbl, additional);
    }

    /** Helper for batchSample: Add double sample(s) to batch, using
     *  blob to store array elements.
     */
    private void batchBlobbedDoubleSample(final RDBWriteChannel channel,
            final Timestamp stamp, int severity,
            Status status, final double dbl, final ListNumber additional) throws Exception
    {
        // Set scalar or 1st element of a waveform.
        // Catch not-a-number, which JDBC (at least Oracle) can't handle.
        if (Double.isNaN(dbl))
        {
            insert_double_sample.setDouble(5, 0.0);
            severity = severities.findOrCreate(AlarmSeverity.UNDEFINED);
            status = stati.findOrCreate(NOT_A_NUMBER_STATUS);
        }
        else
            insert_double_sample.setDouble(5, dbl);

        if (additional == null)
        {    // No more array elements, only scalar
            switch (dialect)
            {
            case Oracle:
                insert_double_sample.setString(6, " ");
                insert_double_sample.setNull(7, Types.BLOB);
                break;
            case PostgreSQL:
                insert_double_sample.setString(7, " ");
                insert_double_sample.setBytes(8, null);
                break;
            default:
                // Types.BINARY?
                insert_double_sample.setString(7, " ");
                insert_double_sample.setNull(8, Types.BLOB);
            }
        }
        else
        {   // More array elements
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            final DataOutputStream dout = new DataOutputStream(bout);
            // Indicate 'Double' as data type
            final int N = additional.size();
            dout.writeInt(N);
            // Write binary data for array elements
            for (int i=0; i<N; ++i)
                dout.writeDouble(additional.getDouble(i));
            dout.close();
            final byte[] asBytes = bout.toByteArray();
            if (dialect == Dialect.Oracle)
            {
                insert_double_sample.setString(6, "d");
                insert_double_sample.setBytes(7, asBytes);
            }
            else
            {
                insert_double_sample.setString(7, "d");
                insert_double_sample.setBytes(8, asBytes);
            }
        }
        // Batch
        completeAndBatchInsert(insert_double_sample, channel, stamp, severity, status);
        ++batched_double_inserts;
    }

    /** Add 'insert' for double samples to batch, handling arrays
     *  via the original array_val table
     */
    private void oldBatchDoubleSamples(final RDBWriteChannel channel,
            final Timestamp stamp, final int severity,
            final Status status, final double dbl, final ListNumber additional) throws Exception
    {
        // Catch not-a-number, which JDBC (at least Oracle) can't handle.
        if (Double.isNaN(dbl))
        {
            insert_double_sample.setDouble(5, 0.0);
            completeAndBatchInsert(insert_double_sample,
                    channel, stamp,
                    severities.findOrCreate(AlarmSeverity.UNDEFINED),
                    stati.findOrCreate(NOT_A_NUMBER_STATUS));
        }
        else
        {
            insert_double_sample.setDouble(5, dbl);
            completeAndBatchInsert(insert_double_sample, channel, stamp, severity, status);
        }
        ++batched_double_inserts;
        // More array elements?
        if (additional != null)
        {
            final int N = additional.size();
            for (int i = 1; i < N; i++)
            {
                insert_array_sample.setInt(1, channel.getId());
                insert_array_sample.setTimestamp(2, stamp);
                insert_array_sample.setInt(3, i);
                // Patch NaN.
                // Conundrum: Should we set the status/severity to indicate NaN?
                // Would be easy if we wrote the main sample with overall
                // stat/sevr at the end.
                // But we have to write it first to avoid index (key) errors
                // with the array sample time stamp....
                // Go back and update the main sample after the fact??
                if (Double.isNaN(additional.getDouble(i)))
                    insert_array_sample.setDouble(4, 0.0);
                else
                    insert_array_sample.setDouble(4, additional.getDouble(i));
                // MySQL nanosecs
                if (dialect == Dialect.MySQL  ||  dialect == Dialect.PostgreSQL)
                    insert_array_sample.setInt(5, stamp.getNanos());
                // Batch
                insert_array_sample.addBatch();
                ++batched_double_array_inserts;
            }
        }
    }


    /** Helper for batchSample: Add long sample to batch.  */
    private void batchLongSample(final RDBWriteChannel channel,
            final Timestamp stamp, final int severity,
            final Status status, final long num) throws Exception
    {
        insert_long_sample.setLong(5, num);
        completeAndBatchInsert(insert_long_sample, channel, stamp, severity, status);
        ++batched_long_inserts;
    }

    /** Helper for batchSample: Add text sample to batch. */
    private void batchTextSamples(final RDBWriteChannel channel,
            final Timestamp stamp, final int severity,
            final Status status, String txt) throws Exception
    {
        if (txt.length() > Preferences.max_text_sample_length)
        {
            logger.log(Level.INFO,
                "Value of {0} exceeds {1} chars: {2}",
                new Object[] { channel.getName(), Preferences.max_text_sample_length, txt });
            txt = txt.substring(0, Preferences.max_text_sample_length);
        }
        insert_txt_sample.setString(5, txt);
        completeAndBatchInsert(insert_txt_sample, channel, stamp, severity, status);
        ++batched_txt_inserts;
    }

    /** Helper for batchSample:
     *  Set the parameters common to all insert statements, add to batch.
     */
    private void completeAndBatchInsert(
            final PreparedStatement insert_xx, final RDBWriteChannel channel,
            final Timestamp stamp, final int severity,
            final Status status) throws Exception
    {
        // Set the stuff that's common to each type
        insert_xx.setInt(1, channel.getId());
        insert_xx.setTimestamp(2, stamp);
        insert_xx.setInt(3, severity);
        insert_xx.setInt(4, status.getId());
        // MySQL nanosecs
        if (dialect == Dialect.MySQL  ||  dialect == Dialect.PostgreSQL)
            insert_xx.setInt(6, stamp.getNanos());
        // Batch
        insert_xx.addBatch();
    }

    /** {@inheritDoc}
     *  RDB implementation completes pending batches
     */
    @Override
    public void flush() throws Exception
    {
        try
        {
            if (batched_double_inserts > 0)
            {
                try
                {
                    checkBatchExecution(insert_double_sample);
                }
                finally
                {
                    batched_double_inserts = 0;
                }
            }
            if (batched_long_inserts > 0)
            {
                try
                {
                    checkBatchExecution(insert_long_sample);
                }
                finally
                {
                    batched_long_inserts = 0;
                }
            }
            if (batched_txt_inserts > 0)
            {
                try
                {
                    checkBatchExecution(insert_txt_sample);
                }
                finally
                {
                    batched_txt_inserts = 0;
                }
            }
            if (batched_double_array_inserts > 0)
            {
                try
                {
                    checkBatchExecution(insert_array_sample);
                }
                finally
                {
                    batched_double_array_inserts = 0;
                }
            }
        }
        catch (final Exception ex)
        {
            if (ex.getMessage().contains("unique"))
            {
                logger.log(Level.WARNING, "Unique constraint error in these samples: " + ex.getMessage());
                if (batched_samples.size() != batched_channel.size())
                    logger.log(Level.WARNING, "Inconsistent batch history");
            }
            throw ex;
        }
        finally
        {
            batched_channel.clear();
            batched_samples.clear();
        }
    }

    /** Submit and clear the batch, or roll back on error */
    private void checkBatchExecution(final PreparedStatement insert) throws Exception
    {
        try
        {   // Try to perform the inserts
            // In principle this could return update counts for
            // each batched insert, but Oracle 10g and 11g just throw
            // an exception
            insert.executeBatch();
            connection.commit();
        }
        catch (final Exception ex)
        {
            try
            {
                // On failure, roll back.
                // With Oracle 10g, the BatchUpdateException doesn't
                // indicate which of the batched commands faulted...
                insert.clearBatch();
                // Still: Commit what's committable.
                // Unfortunately no way to know what failed,
                // and no way to re-submit the 'remaining' inserts.
                connection.commit();
            }
            catch (Exception nested)
            {
                logger.log(Level.WARNING, "clearBatch(), commit() error after batch issue", nested);
            }
            throw ex;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close()
    {
        channels.clear();
        if (severities != null)
        {
            severities.dispose();
            severities = null;
        }
        if (stati != null)
        {
            stati.dispose();
            stati = null;
        }
        try
        {
            connection.close();
        }
        catch (SQLException ex)
        {
            logger.log(Level.WARNING, "Cannot close connection", ex);
        }
    }
}
