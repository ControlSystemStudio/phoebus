/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log.derby;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFactory;
import org.csstudio.scan.info.Scan;

/** Base for an RDB-based sample logger
 *
 *  <p>Can write and read data for any scan.
 *
 *  <p>Derived class needs to handle connection to specific RDB.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class RDBDataLogger implements AutoCloseable
{
    /** Max 'name' length */
    private volatile static int max_name_length = 0;

    final protected Connection connection;

    /** Device ID cache */
    final private Map<String, Integer> devices = new HashMap<String, Integer>();

    /** Re-used statement */
    private PreparedStatement insert_sample_statement = null;

    /** Initialize
      *  @throws Exception on error
     */
    public RDBDataLogger() throws Exception
    {
        connection = connect();

        init(connection);
    }

    private void init(final Connection connection) throws Exception
    {
        synchronized (RDBDataLogger.class)
        {
            if (max_name_length <= 0)
            {
                max_name_length = 100; // default
                try
                (
                    final ResultSet rs = connection.getMetaData().getColumns(null, null, "SCANS", "NAME");
                )
                {
                    if (rs.next())
                        max_name_length = rs.getInt("COLUMN_SIZE");
                }
                catch (Exception ex)
                {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot obtain max scan name length", ex);
                }
            }
        }
    }

    /** Connect to RDB
     *  @return Connection
     *  @throws Exception on error
     */
    abstract protected Connection connect() throws Exception;

    /** Create a new scan in the database
     *  @param scan_name Name of the scan
     *  @return Scan ID, unique within the database
     *  @throws Exception on error
     */
    public Scan createScan(String scan_name) throws Exception
    {
        final Instant now = Instant.now();
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO scans(name, created) VALUES (?,?)",
                    Statement.RETURN_GENERATED_KEYS);
        )
        {
            if (scan_name.length() > max_name_length)
            {
                Logger.getLogger(getClass().getName())
                .log(Level.WARNING, "Truncating scan name to {0}: {1}", new Object[] { max_name_length, scan_name});
                scan_name = scan_name.substring(0, max_name_length);
            }
            statement.setString(1, scan_name);
            statement.setTimestamp(2, Timestamp.from(now));
            statement.executeUpdate();
            final ResultSet result = statement.getGeneratedKeys();
            try
            {
                if (! result.next())
                    throw new Exception("Missing new scan ID");
                final long id = result.getLong(1);
                return new Scan(id, scan_name, now);
            }
            finally
            {
                result.close();
            }
        }
    }

    /** Locate scan
     *  @param id Scan ID
     *  @return Scan for ID or <code>null</code>
     *  @throws Exception on error
     */
    public Scan getScan(final long id) throws Exception
    {
        final Scan scan;
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, created FROM scans WHERE id=?");
        )
        {
            statement.setLong(1, id);
            final ResultSet result = statement.executeQuery();
            if (result.next())
                scan = new Scan(result.getLong(1),
                                result.getString(2),
                                result.getTimestamp(3).toInstant());
            else
                scan = null;
            result.close();
        }
        return scan;
    }

    /** Obtain all available scans
     *  @return Scans that have been logged
     *  @throws Exception on error
     */
    public List<Scan> getScans() throws Exception
    {
        final List<Scan> scans = new ArrayList<Scan>();
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, created FROM scans ORDER BY id");
        )
        {
            final ResultSet result = statement.executeQuery();
            while (result.next())
                scans.add(new Scan(result.getLong(1),
                                   result.getString(2),
                                   result.getTimestamp(3).toInstant()));
            result.close();
        }
        return scans;
    }

    /** Find (or create) a device in the database
     *  @param device_name Name of the scan
     *  @return Device ID, unique within the database
     *  @throws Exception on error
     */
    public int getDevice(final String device_name) throws Exception
    {
        // Check cache
        final Integer cached_id = devices.get(device_name);
        if (cached_id != null)
            return cached_id;
        // Locate existing device
        int id = findDevice(device_name);
        if (id > 0)
        {
            devices.put(device_name, id);
            return id;
        }
        // Insert new device
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO devices(name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS);
        )
        {
            statement.setString(1, device_name);
            statement.executeUpdate();
            final ResultSet result = statement.getGeneratedKeys();
            try
            {
                if (! result.next())
                    throw new Exception("Missing new device ID");
                id = result.getInt(1);
                devices.put(device_name, id);
                return id;
            }
            finally
            {
                result.close();
            }
        }
    }

    /** Find a device in the database
     *  @param device_name Name of the scan
     *  @return Device ID found in database; -1 if not found
     *  @throws SQLException on error
     */
    private int findDevice(final String device_name) throws SQLException
    {
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM devices WHERE name=?");
        )
        {
            statement.setString(1, device_name);
            final ResultSet result = statement.executeQuery();
            try
            {
                if (! result.next())
                    return -1;
                return result.getInt(1);
            }
            finally
            {
                result.close();
            }
        }
    }

    /** Log a sample
     *  @param scan_id ID of associated scan
     *  @param device Device name
     *  @param sample Sample to log
     *  @throws Exception on error
     */
    public void log(final long scan_id, final String device, final ScanSample sample) throws Exception
    {
        final int device_id = getDevice(device);

        if (insert_sample_statement == null)
            insert_sample_statement = connection.prepareStatement(
                    "INSERT INTO samples(scan_id, device_id, serial, timestamp, value)" +
                    " VALUES (?,?,?,?,?)");
        insert_sample_statement.setLong(1, scan_id);
        insert_sample_statement.setInt(2, device_id);
        insert_sample_statement.setLong(3, sample.getSerial());
        insert_sample_statement.setTimestamp(4, Timestamp.from(sample.getTimestamp()));
        insert_sample_statement.setObject(5, new SampleValue(sample.getValues()));
        final int rows = insert_sample_statement.executeUpdate();
        if (rows != 1)
            throw new Exception("Sample insert affected " + rows + " rows");
    }

    /** Get serial of last logged sample.
     *
     *  <p>Can be used to determine if there are new samples
     *  that should be fetched via <code>getScanData()</code>
     *  @param scan_id ID of the scan
     *  @return Serial of last sample in scan data or -1 if nothing has been logged
     *  @throws Exception on error
     *  @see #getScanData()
     */
    public long getLastScanDataSerial(final long scan_id) throws Exception
    {
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(serial) FROM samples WHERE scan_id=?");
        )
        {
            statement.setLong(1, scan_id);
            try
            (
                final ResultSet result = statement.executeQuery();
            )
            {
                if (result.next())
                {
                    final long serial = result.getLong(1);
                    if (! result.wasNull())
                        return serial;
                }
            }
        }
        return -1;
    }


    /** Obtain data for a scan
     *  @param scan_id ID of the scan
     *  @return {@link ScanData}
     *  @throws Exception on error
     */
    public ScanData getScanData(final long scan_id) throws Exception
    {
        final Map<String, List<ScanSample>> device_logs = new HashMap<String, List<ScanSample>>();

        // Could fetch all samples for scan ID, but
        // organizing the retrieval by device in case
        // that's helpful later on
        final String[] devices = getScanDevices(scan_id);
        for (String device : devices)
        {
            final List<ScanSample> samples = getScanSamples(scan_id, device);
            device_logs.put(device, samples);
        }

        return new ScanData(device_logs);
    }

    /** Get samples
     *  @param scan_id ID of the scan
     *  @param device_name Name of the device
     *  @return Samples for that scan
     *  @throws Exception on error
     */
    private List<ScanSample> getScanSamples(final long scan_id, final String device_name) throws Exception
    {
        final List<ScanSample> samples = new ArrayList<ScanSample>();
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT serial, timestamp, value FROM samples WHERE scan_id=? AND device_id=? ORDER BY serial");
        )
        {
            statement.setLong(1, scan_id);
            statement.setInt(2, getDevice(device_name));
            final ResultSet result = statement.executeQuery();
            while (result.next())
            {
                final long serial = result.getLong(1);
                final Instant timestamp = result.getTimestamp(2).toInstant();
                final SampleValue value = (SampleValue) result.getObject(3);
                samples.add(ScanSampleFactory.createSample(timestamp, serial, value.getValues()));
            }
            result.close();
        }
        return samples;
    }

    /** Obtain devices that have data for a scan
     *  @param scan_id ID of the scan
     *  @return Device names
     *  @throws SQLException on error
     */
    private String[] getScanDevices(final long scan_id) throws SQLException
    {
        final List<String> devices = new ArrayList<String>();
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    "SELECT DISTINCT d.name FROM samples s JOIN devices d ON s.device_id = d.id  WHERE scan_id=?");
        )
        {
            statement.setLong(1, scan_id);
            final ResultSet result = statement.executeQuery();
            while (result.next())
                devices.add(result.getString(1));
            result.close();
        }
        return devices.toArray(new String[devices.size()]);
    }

    /** Delete logged data for a scan
     *  @param scan_id ID of the scan
     *  @throws SQLException on error
     */
    public void deleteDataLog(final long scan_id) throws SQLException
    {
        connection.setAutoCommit(false);
        try
        {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM samples WHERE scan_id=?");
            try
            {
                statement.setLong(1, scan_id);
                statement.executeUpdate();
            }
            finally
            {
                statement.close();
            }

            statement = connection.prepareStatement(
                    "DELETE FROM scans WHERE id=?");
            try
            {
                statement.setLong(1, scan_id);
                statement.executeUpdate();
            }
            finally
            {
                statement.close();
            }

            connection.commit();
        }
        catch (SQLException ex)
        {
            connection.rollback();
            throw ex;
        }
        finally
        {
            connection.setAutoCommit(true);
        }
    }

    /** Close database.
     *  Must be called to release resources.
     */
    @Override
    public void close()
    {
        try
        {
            if (insert_sample_statement != null)
            {
                insert_sample_statement.close();
                insert_sample_statement = null;
            }
            connection.close();
        }
        catch (Exception ex)
        {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error closing log RDB", ex);
        }
    }
}
