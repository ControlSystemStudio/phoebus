/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.rdb;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.epics.util.array.ArrayDouble;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.framework.rdb.RDBInfo.Dialect;
import org.phoebus.pv.TimeHelper;

/** Base for ValueIterators that read from the RDB
 *  @author Kay Kasemir
 *  @author Lana Abadie (PostgreSQL)
 */
@SuppressWarnings("nls")
abstract class AbstractRDBValueIterator implements ValueIterator
{
    final protected RDBArchiveReader reader;
    final protected Connection connection;
    final protected int channel_id;

    protected Display display = null;
    protected EnumDisplay labels = null;

    /** SELECT ... for the array samples. */
    private PreparedStatement sel_array_samples = null;

    /** Before version 3.1.0, we would look for array
     *  values in the array_val table until we are sure
     *  that there are no array samples.
     *  Than we stopped looking for array samples
     *  to speed things up.
     *
     *  Since version 3.1.0, there is the option of using a
     *  BLOB and the sample table contains an is_array indicator,
     *  so we know for sure right away.
     *
     *  To remain compatible with old data, we still assume
     *  there are array values until we know otherwise.
     */
    protected boolean is_an_array = true;

    /** @param reader {@link RDBArchiveReader}
     *  @param channel_id ID of channel
     *  @throws Exception on error
     */
    AbstractRDBValueIterator(final RDBArchiveReader reader,
            final int channel_id) throws Exception
    {
        this.reader = reader;
        this.connection = reader.getPool().getConnection();

        // Disable auto-commit to determine sample with PostgreSQL when fetch direction is FETCH_FORWARD
        if (reader.getPool().getDialect() == Dialect.PostgreSQL)
            connection.setAutoCommit(false);

        this.channel_id = channel_id;
        try
        {
            this.display = determineDisplay();
            final List<String> options = determineLabels();
            this.labels = options == null ? null : EnumDisplay.of(options);
        }
        catch (final Exception ex)
        {
            // Set iterator to empty
            close();
            if (! RDBArchiveReader.isCancellation(ex))
                throw ex;
            // Else: Not a real error, return empty iterator
        }
        if (labels == null  &&  display == null)
            display = Display.of(Range.of(0, 10), Range.undefined(), Range.undefined(), Range.undefined(), "", NumberFormats.precisionFormat(0));
    }

    /** @return Numeric meta data information for the channel or <code>null</code>
     *  @throws Exception on error
     */
    private Display determineDisplay() throws Exception
    {
        Display display = null;
        try
        (
            final PreparedStatement statement =
                connection.prepareStatement(reader.getSQL().numeric_meta_sel_by_channel);
        )
        {
            // Try numeric meta data
            statement.setInt(1, channel_id);
            final ResultSet result = statement.executeQuery();
            if (result.next())
            {
                final NumberFormat format = NumberFormats.precisionFormat(result.getInt(7));   // prec
                display = Display.of(Range.of(result.getDouble(1), result.getDouble(2)),
                                     Range.of(result.getDouble(5), result.getDouble(6)),
                                     Range.of(result.getDouble(3), result.getDouble(4)),
                                     Range.of(result.getDouble(1), result.getDouble(2)),
                                     result.getString(8),
                                     format);
            }
            result.close();
        }

        return display;
    }

    /** @return Numeric meta data information for the channel or <code>null</code>
     *  @throws Exception on error
     */
    private List<String> determineLabels() throws Exception
    {
        // Try enumerated meta data
        List<String> labels = null;
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                        reader.getSQL().enum_sel_num_val_by_channel);
        )
        {
            statement.setInt(1, channel_id);
            final ResultSet result = statement.executeQuery();
            if (result.next())
            {
                labels = new ArrayList<>();
                do
                {
                    final int id = result.getInt(1);
                    final String val = result.getString(2);
                    // Expect vals for ids 0, 1, 2, ...
                    if (id != labels.size())
                        throw new Exception("Enum IDs for channel with ID "
                                + channel_id + " not in sequential order");
                    labels.add(val);
                }
                while (result.next());
                // Anything found?
                if (labels.isEmpty())
                    labels = null;
            }
            result.close();
        }
        return labels;
    }

    /** Extract value from SQL result
     *  @param result ResultSet that must contain contain time, severity, ..., value
     *  @param handle_array Try to read array elements, or only a scalar value?
     *  @return IValue Decoded IValue
     *  @throws Exception on error, including cancellation
     */
    protected VType decodeSampleTableValue(final ResultSet result, final boolean handle_array) throws Exception
    {
        // Get time stamp
        final java.sql.Timestamp stamp = result.getTimestamp(1);
        // Oracle has nanoseconds in TIMESTAMP, other RDBs in separate column
        if (reader.getPool().getDialect() != Dialect.Oracle)
            stamp.setNanos(result.getInt(7));
        final Time time = TimeHelper.fromInstant(stamp.toInstant());

        // Get severity/status
        final String status = reader.getStatus(result.getInt(3));
        final AlarmSeverity severity = filterSeverity(reader.getSeverity(result.getInt(2)), status);
        final Alarm alarm = Alarm.of(severity, AlarmStatus.CLIENT, status);

        // Determine the value type
        // Try double
        final double dbl0 = result.getDouble(5);
        if (! result.wasNull())
        {
            // Is it an error to have enumeration strings for double samples?
            // In here, we handle it by returning enumeration samples,
            // because the meta data would be wrong for double values.
            if (labels != null)
                return VEnum.of((int) dbl0, labels, alarm, time);
            // Double data.
            if (handle_array)
            {   // Get array elements - if any.
                final double data[] = RDBPreferences.use_array_blob
                    ? readBlobArrayElements(dbl0, result)
                    : readArrayElements(time, dbl0, severity);
                if (data.length == 1)
                    return VDouble.of(data[0], alarm, time, display);
                else
                    return VDoubleArray.of(ArrayDouble.of(data), alarm, time, display);
            }
            else
                return VDouble.of(dbl0, alarm, time, display);
        }

        // Try integerRDBUtil
        final int num = result.getInt(4);
        if (! result.wasNull())
        {   // Enumerated integer?
            if (labels != null)
                return VEnum.of(num, labels, alarm, time);
            return VDouble.of(num, alarm, time, display);
        }

        // Default to string
        final String txt = result.getString(6);
        return VString.of(txt, alarm, time);
    }

    /** @param severity Original severity
     *  @param status Status text
     *  @return If the status indicates that there is no actual value,
     *          provide the special 'no value' severity
     */
    protected AlarmSeverity filterSeverity(final AlarmSeverity severity, final String status)
    {
        // Hard-coded knowledge:
        // When the status indicates
        // that the archive is off or channel was disconnected,
        // we use the special severity that marks a sample
        // without a value.
        if (status.equalsIgnoreCase("Archive_Off") ||
            status.equalsIgnoreCase("Disconnected") ||
            status.equalsIgnoreCase("Write_Error"))
            return AlarmSeverity.UNDEFINED;
        return severity;
    }

    /** Given the time and first element of the  sample, see if there
     *  are more array elements.
     *  @param stamp Time stamp of the sample
     *  @param dbl0 Value of the first (maybe only) array element
     *  @param severity Severity of the sample
     *  @return Array with given element and maybe more.
     *  @throws Exception on error, including 'cancel'
     */
    private double[] readArrayElements(final Time time,
            final double dbl0,
            final AlarmSeverity severity) throws Exception
    {
        // For performance reasons, only look for array data until we hit a scalar sample.
        if (is_an_array==false)
            return new double [] { dbl0 };

        // See if there are more array elements
        if (sel_array_samples == null)
        {   // Lazy initialization
            sel_array_samples = connection.prepareStatement(
                    reader.getSQL().sample_sel_array_vals);
        }
        sel_array_samples.setInt(1, channel_id);
        sel_array_samples.setTimestamp(2, java.sql.Timestamp.from(time.getTimestamp()));
        // MySQL keeps nanoseconds in designated column, not TIMESTAMP
        if (reader.getPool().getDialect() != Dialect.Oracle)
            sel_array_samples.setInt(3, time.getTimestamp().getNano());

        // Assemble array of unknown size in ArrayList ....
        final ArrayList<Double> vals = new ArrayList<>();
        reader.addForCancellation(sel_array_samples);
        try
        {
            final ResultSet res = sel_array_samples.executeQuery();
            vals.add(Double.valueOf(dbl0));
            while (res.next())
                vals.add(res.getDouble(1));
            res.close();
        }
        finally
        {
            reader.removeFromCancellation(sel_array_samples);
        }
        // Convert to plain double array
        final int N = vals.size();
        final double ret[] = new double[N];
        for (int i = 0; i < N; i++)
            ret[i] = vals.get(i).doubleValue();
        // Check if it's in fact just a scalar, and a valid one
        if (N == 1  &&  severity != AlarmSeverity.UNDEFINED)
        {   // Found a perfect non-array sample:
            // Assume that the data is scalar, skip the array check from now on
            is_an_array = false;
        }
        return ret;
    }

    /** See if there are array elements.
     *  @param dbl0 Value of the first (maybe only) array element
     *  @param result ResultSet for the sample table with blob
     *  @return Array with given element and maybe more.
     *  @throws Exception on error, including 'cancel'
     */
    private double[] readBlobArrayElements(final double dbl0, final ResultSet result) throws Exception
    {
        final String datatype;
        if (reader.getPool().getDialect() == Dialect.Oracle)
            datatype = result.getString(7);
        else
            datatype = result.getString(8);

        // ' ' or NULL indicate: Scalar, not an array
        if (datatype == null || " ".equals(datatype) || result.wasNull())
            return new double [] { dbl0 };

        // Decode BLOB
        final byte[] bytes = result.getBytes(reader.getPool().getDialect() == Dialect.Oracle ? 8 : 9);
        final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        final DataInputStream data = new DataInputStream(stream);
        if ("d".equals(datatype))
        {    // Read Double typed array elements
            final int nelm = data.readInt();
            final double[] array = new double[nelm];
            for (int i = 0; i < nelm; i++)
                array[i] = data.readDouble();
            data.close();
            return array;
        }
        // TODO Decode 'l' Long and 'i' Integer?
        else
        {
            throw new Exception("Sample BLOBs of type '" + datatype + "' are not decoded");
        }
    }


    @Override
    public void close()
    {
        if (sel_array_samples != null)
        {
            try
            {
                sel_array_samples.close();
            }
            catch (Exception ex)
            {
                // Ignore
            }
            sel_array_samples = null;
        }

        if (reader.getPool().getDialect() == Dialect.PostgreSQL)
        {
            // Restore default auto-commit on result set close
             try
             {
                 connection.rollback();
                 connection.setAutoCommit(true);
             }
             catch (Exception ex)
             {
                 // Ignore
             }
        }

        reader.getPool().releaseConnection(connection);
    }
}
