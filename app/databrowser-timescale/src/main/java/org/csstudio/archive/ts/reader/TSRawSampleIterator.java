/*******************************************************************************
 * Copyright (c) 2021-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.ts.reader;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

import org.csstudio.archive.ts.Preferences;
import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.reader.rdb.RDBArchiveReader;
import org.phoebus.framework.rdb.RDBInfo.Dialect;
import org.phoebus.pv.TimeHelper;

/** Raw sample iterator for TimescaleDB
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TSRawSampleIterator implements ValueIterator
{
    private final TSArchiveReader reader;
    private final Connection connection;
    private final int channel_id;

    /** SELECT ... for the start .. end samples. */
    private PreparedStatement sel_samples = null;

    /** Result of <code>sel_samples</code> */
    private ResultSet result_set = null;

    /** 'Current' value that <code>next()</code> will return,
     *  or <code>null</code>
     */
    private VType value = null;
    private DisplayInfo display;

    /** @param reader Reader
     *  @param channel_id Channel ID
     *  @param start Start time
     *  @param end End time
     *  @throws Exception on error
     */
    public TSRawSampleIterator(final TSArchiveReader reader, final int channel_id,
                               final Instant start, final Instant end) throws Exception
    {
        this.reader = reader;
        this.channel_id = channel_id;
        this.connection = reader.getPool().getConnection();

        try
        {
            display = DisplayInfo.forChannel(channel_id, reader);
            determineInitialSample(start, end);
        }
        catch (Exception ex)
        {
            // Caller won't get valid iterator, close here
            close();
            throw ex;
        }
    }

    /** Get the samples: <code>result_set</code> will have the samples,
     *  <code>value</code> will contain the first sample
     *  @param start Start time
     *  @param end End time
     *  @throws Exception on error, including cancellation
     */
    private void determineInitialSample(final Instant start, final Instant end) throws Exception
    {
        // Fetch the samples
        sel_samples = connection.prepareStatement(
                reader.getSQL().sample_sel_by_id_start_end, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        sel_samples.setFetchDirection(ResultSet.FETCH_FORWARD);

        sel_samples.setFetchSize(Preferences.fetch_size);

        sel_samples.setInt(1, channel_id);
        sel_samples.setTimestamp(2, Timestamp.from(start));
        sel_samples.setTimestamp(3, Timestamp.from(end));

        reader.addForCancellation(sel_samples);

        result_set = sel_samples.executeQuery();
        // Get first sample
        if (result_set.next())
            value = decodeSampleTableValue(result_set);
        // else leave value null to indicate end of samples
    }

    @Override
    public boolean hasNext()
    {
        return value != null;
    }

    @Override
    public VType next()
    {
        // This should not happen...
        if (result_set == null)
            throw new IllegalStateException("RawSampleIterator.next(" + channel_id + ") called after end");

        // Remember value to return...
        final VType result = value;
        // ... and prepare next value
        try
        {
            if (result_set.next())
                value = decodeSampleTableValue(result_set);
            else
                close();
        }
        catch (Exception ex)
        {
            close();
            if (! RDBArchiveReader.isCancellation(ex))
            {
                logger.log(Level.WARNING, "Error reading samples for channel ID " + channel_id, ex);
            }
            // Else: Not a real error; return empty iterator
        }
        return result;
    }

    /** Extract value from SQL result
     *  @param result ResultSet that must contain contain time, severity, ..., value
     *  @return IValue Decoded IValue
     *  @throws Exception on error, including cancellation
     */
    protected VType decodeSampleTableValue(final ResultSet result) throws Exception
    {
        // 1          2            3          4        5          6        7         8         9
        // smpl_time, severity_id, status_id, num_val, float_val, str_val, nanosecs, datatype, array_val

        // Get time stamp
        final Timestamp stamp = result.getTimestamp(1);
        // Oracle has nanoseconds in TIMESTAMP, other RDBs in separate column
        if (reader.getPool().getDialect() != Dialect.Oracle)
            stamp.setNanos(result.getInt(7));
        final Time time = TimeHelper.fromInstant(stamp.toInstant());

        // Get severity/status
        final Alarm alarm = reader.decodeAlarm(result.getInt(2), result.getInt(3));

        // Determine the value type
        // Try double
        final double dbl0 = result.getDouble(5);
        if (! result.wasNull())
        {   // Scalar or array?
            final String datatype = result.getString(8);
            if (datatype == null  ||  datatype.isBlank())
                return VDouble.of(dbl0, alarm, time, display.getDisplay());

            // Decode array elements from BLOB
            final byte[] bytes = result.getBytes(9);
            final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
            final DataInputStream data = new DataInputStream(stream);
            if ("d".equals(datatype))
            {
                // Read Double typed array elements
                final int nelm = data.readInt();
                final double[] array = new double[nelm];
                for (int i = 0; i < nelm; i++)
                    array[i] = data.readDouble();
                data.close();
                return VDoubleArray.of(ArrayDouble.of(array), alarm, time, display.getDisplay());
            }
            throw new Exception("Sample BLOBs with array data of type '" + datatype + "' cannot be decoded");
        }

        // Try integer
        final int num = result.getInt(4);
        if (! result.wasNull())
        {   // Enumerated integer?
            if (display.getLabels() != null)
                return VEnum.of(num, display.getLabels(), alarm, time);
            return VDouble.of(num, alarm, time, display.getDisplay());
        }

        // Default to string
        final String txt = result.getString(6);
        return VString.of(txt, alarm, time);
    }

    @Override
    public void close()
    {
        value = null;
        if (result_set != null)
        {
            try
            {
                result_set.close();
            }
            catch (Exception ex)
            {
                // Ignore
            }
            result_set = null;
        }
        if (sel_samples != null)
        {
            reader.removeFromCancellation(sel_samples);
            try
            {
                sel_samples.close();
            }
            catch (Exception ex)
            {
                // Ignore
            }
            sel_samples = null;
        }

        reader.getPool().releaseConnection(connection);
    }
}
