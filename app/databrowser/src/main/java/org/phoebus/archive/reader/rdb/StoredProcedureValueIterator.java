/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.rdb;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.framework.rdb.RDBInfo.Dialect;
import org.phoebus.pv.TimeHelper;

/** Value Iterator that provides 'optimized' data by calling
 *  a stored database procedure.
 *  @author Kay Kasemir
 *  @author Laurent Philippe - MySQL support
 */
@SuppressWarnings("nls")
public class StoredProcedureValueIterator extends AbstractRDBValueIterator
{
    final private String stored_procedure;

    /** Values received from the stored procedure */
    private List<VType> values = null;

    /** Iteration index into <code>values</code>, points to what
     *  <code>next()</code> will return or -1
     */
    private int index = -1;

    /** Initialize
     *  @param reader RDBArchiveReader
     *  @param stored_procedure Name of the stored procedure to call
     *  @param channel_id ID of channel
     *  @param start Start time
     *  @param end End time
     *  @param count Desired value count
     *  @throws Exception on error
     */
    public StoredProcedureValueIterator(final RDBArchiveReader reader,
            final String stored_procedure,
            final int channel_id, final Instant start, final Instant end,
            final int count) throws Exception
    {
        super(reader, channel_id);
        this.stored_procedure = stored_procedure;

        try
        {
            executeProcedure(start, end, count);
        }
        catch (Exception ex)
        {
            // Caller won't get a valid iterator, so close here
            super.close();
            throw ex;
        }
    }

    /** Invoke stored procedure
     *  @param start Start time
     *  @param end End time
     *  @param count Desired value count
     *  @throws Exception on error
     */
    private void executeProcedure(final Instant start, final Instant end,
            final int count) throws Exception
    {
        final String sql;
        final Dialect dialect = reader.getPool().getDialect();

        switch (dialect)
        {
        case MySQL:
            sql = "{call " + stored_procedure + "(?, ?, ?, ?)}";
            break;
        case PostgreSQL:
            sql = "{? = call " + stored_procedure + "(?, ?, ?, ?)}";
            break;
        case Oracle:
            sql = "begin ? := " + stored_procedure + "(?, ?, ?, ?); end;";
            break;
        default:
            throw new Exception("Stored procedure data readout not supported for " + dialect);

        }

        final CallableStatement statement =
                connection.prepareCall(sql);
        reader.addForCancellation(statement);
        try
        {
            final ResultSet result;

            if (dialect == Dialect.MySQL)
            {    //MySQL
                 statement.setInt(1, channel_id);
                 statement.setTimestamp(2, Timestamp.from(start));
                 statement.setTimestamp(3, Timestamp.from(end));
                 statement.setInt(4, count);
                 result = statement.executeQuery();
            }
            else if(dialect == Dialect.PostgreSQL)
            {   //PostgreSQL
                statement.registerOutParameter(1, Types.OTHER);
                statement.setLong(2, channel_id);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                statement.setString(3, sdf.format(new Date(Timestamp.from(start).getTime())));
                statement.setString(4, sdf.format(new Date(Timestamp.from(end).getTime())));
                statement.setLong(5, count);
                statement.setFetchDirection(ResultSet.FETCH_FORWARD);
                statement.setFetchSize(1000);
                statement.execute();
                result = (ResultSet) statement.getObject(1);
            }
            else
            {   //ORACLE
                // Get oracle.jdbc.OracleTypes.CURSOR
                // without requiring direct dependency on ojdbc jar
                final Class<?> cls = Class.forName("oracle.jdbc.OracleTypes");
                final Field field = cls.getField("CURSOR");
                final int CURSOR = field.getInt(null);
                statement.registerOutParameter(1, CURSOR);
                statement.setInt(2, channel_id);
                statement.setTimestamp(3, Timestamp.from(start));
                statement.setTimestamp(4, Timestamp.from(end));
                statement.setInt(5, count);
                statement.setFetchDirection(ResultSet.FETCH_FORWARD);
                statement.setFetchSize(1000);
                statement.execute();
                result = (ResultSet) statement.getObject(1);
            }
            result.setFetchSize(1000);

            // Determine result type: min/max/average table or
            // fallback to SAMPLE table format?
            final ResultSetMetaData meta = result.getMetaData();
            final int N = meta.getColumnCount();
            if (N == 9)
                values = decodeOptimizedTable(result);
            else
                values = decodeSampleTable(result);
            // Initialize iterator for first value
            if (values.size() > 0)
                index = 0;
            // else: No data, leave as -1
        }
        catch (Exception ex)
        {
            if (! RDBArchiveReader.isCancellation(ex))
                throw ex;
            // Else: Not a real error; return empty iterator
            Logger.getLogger(getClass().getName()).log(Level.FINE,
                    "Stored procedure cancelled");
        }
        finally
        {
            reader.removeFromCancellation(statement);
            statement.close();
        }
    }

    /** Decode samples from 'optimized' table with min/max/average
     *  @param result ResultSet
     *  @return IValue array of samples
     *  @throws Exception on error
     */
    private List<VType> decodeOptimizedTable(final ResultSet result) throws Exception
    {
        final List<VType> values = new ArrayList<>();

        // Row with min/max/average data:
        // WB: 1, SMPL_TIME: 2010/01/22 21:07:18.772633666, SEVERITY_ID: null, STATUS_ID: null, MIN_VAL: 8.138729867823713E-8, MAX_VAL: 6.002717327646678E-7, AVG_VAL: 8.240168908036992E-8, STR_VAL: null, CNT: 3611
        // Row with String value:
        // WB: -1, SMPL_TIME: 2010/01/28 11:14:11.086000000, SEVERITY_ID: 2, STATUS_ID: 2, MIN_VAL: null, MAX_VAL: null, AVG_VAL: null, STR_VAL: Archive_Off, CNT: 1
        // i.e. Columns 1 WB, 2 SMPL_TIME, 3 SEVERITY_ID, 4 STATUS_ID, 5 MIN_VAL, 6 MAX_VAL, 7 AVG_VAL, 8 STR_VAL, 9 CNT
        while (result.next())
        {
            // Time stamp
            final Time time = TimeHelper.fromInstant(result.getTimestamp(2).toInstant());

            // Get severity/status
            final Alarm alarm;
            final int sev_id = result.getInt(3);
            if (result.wasNull())
                alarm = Alarm.none();
            else
            {
                final String status = reader.getStatus(result.getInt(4));
                final AlarmSeverity severity = filterSeverity(reader.getSeverity(sev_id), status);
                alarm = Alarm.of(severity, AlarmStatus.CLIENT, status);
            }


            // WB==-1 indicates a String sample
            final VType value;
            if (result.getInt(1) < 0)
                value = VString.of(result.getString(8), alarm, time);
            else
            {   // Only one value within averaging bucket?
                final int cnt = result.getInt(9);
                final double val_or_avg = result.getDouble(7);
                if (cnt == 1)
                    value = VDouble.of(val_or_avg, alarm, time, display);
                else // Decode min/max/average
                {
                    final double min = result.getDouble(5);
                    final double max = result.getDouble(6);
                    final double stddev = 0.0; // not known
                    value = VStatistics.of(val_or_avg, stddev, min, max, cnt, alarm, time, display);
                }
            }
            values.add(value);
        }
        return values;
    }

    /** Decode samples from SAMPLE table
     *  @param result ResultSet
     *  @return IValue array of samples
     *  @throws Exception on error, including cancellation
     */
    private List<VType> decodeSampleTable(final ResultSet result) throws Exception
    {
        final ArrayList<VType> values = new ArrayList<>();
        while (result.next())
        {
            final VType value = decodeSampleTableValue(result, false);
            values.add(value);
        }
        return values;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext()
    {
        return index >= 0;
    }

    @Override
    public VType next()
    {
        final VType result = values.get(index);
        ++index;
        if (index >= values.size())
            index = -1;
        return result;
    }

    @Override
    public void close()
    {
        index = -1;
        values = null;
        super.close();
    }
}
