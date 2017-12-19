/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.rdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.rdb.RDBConnectionPool;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.VType;

/** {@link ArchiveReader} for RDB
 *  @author Kay Kasemir
 *  @author Lana Abadie - PostgreSQL support in CS-Studio version
 *  @author Laurent Philippe - MySQL support in CS-Studio version
 */
@SuppressWarnings("nls")
public class RDBArchiveReader implements ArchiveReader
{
    public static final Logger logger = Logger.getLogger(RDBArchiveReader.class.getPackageName());

    /** Oracle error code for canceled statements */
    private static final String ORACLE_CANCELLATION = "ORA-01013";

    static final String USER = "user";
    static final String PASSWORD = "password";
    static final String PREFIX = "prefix";
    static final String TIMEOUT_SECS = "timeout_secs";

    private static AtomicBoolean initialized = new AtomicBoolean();
    private static String user, password, prefix;
    private static int timeout;

    /** Connection pool */
    private final RDBConnectionPool pool;

    /** SQL statements */
    private final SQL sql;

    /** Map of status IDs to Status strings */
    private final Map<Integer, String> stati;

    /** Map of severity IDs to Severities */
    private final Map<Integer, AlarmSeverity> severities;

    /** Active statements to cancel in cancel() */
    private final List<Statement> cancellable_statements = new ArrayList<>();

    private void initialize()
    {
        final PreferencesReader prefs = new PreferencesReader(RDBArchiveReader.class, "/archive_reader_rdb_preferences.properties");
        user = prefs.get(USER);
        password = prefs.get(PASSWORD);
        prefix = prefs.get(PREFIX);
        timeout = prefs.getInt(TIMEOUT_SECS);
    }

    public RDBArchiveReader(final String description, final String url) throws Exception
    {
        if (! initialized.getAndSet(true))
            initialize();
        pool = new RDBConnectionPool(url, user, password);
        sql = new SQL(pool.getDialect(), prefix);
        stati = getStatusValues();
        severities = getSeverityValues();
    }

    /** @return Map of all status ID/Text mappings
     *  @throws Exception on error
     */
    private Map<Integer, String> getStatusValues() throws Exception
    {
        final Map<Integer, String> stati = new HashMap<>();
        final Connection connection = pool.getConnection();
        try
        {
            try
            (
                final Statement statement = connection.createStatement();
            )
            {
                if (timeout > 0)
                    statement.setQueryTimeout(timeout);
                statement.setFetchSize(100);
                final ResultSet result = statement.executeQuery(sql.sel_stati);
                while (result.next())
                    stati.put(result.getInt(1), result.getString(2));
                result.close();
        }
        }
        finally
        {
            pool.releaseConnection(connection);
        }
        return stati;
    }

    /** @return Map of all severity ID/AlarmSeverity mappings
     *  @throws Exception on error
     */
    private Map<Integer, AlarmSeverity> getSeverityValues() throws Exception
    {
        final Map<Integer, AlarmSeverity> severities = new HashMap<>();
        final Connection connection = pool.getConnection();
        try
        {
            try
            (
                final Statement statement = connection.createStatement();
            )
            {
                if (timeout > 0)
                    statement.setQueryTimeout(timeout);
                statement.setFetchSize(100);
                final ResultSet result = statement.executeQuery(sql.sel_severities);
                while (result.next())
                {
                    final int id = result.getInt(1);
                    final String text = result.getString(2);
                    severities.put(id, decodeAlarmSeverity(text));
                }
                result.close();
        }
        }
        finally
        {
            pool.releaseConnection(connection);
        }
        return severities;
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

    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getURL()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getNamesByPattern(final String glob_pattern) throws Exception
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
            final PreparedStatement statement = connection.prepareStatement(sql.channel_sel_by_like);
            addForCancellation(statement);
            try
            {
                statement.setString(1, sql_pattern);
                final ResultSet result = statement.executeQuery();
                while (result.next())
                    names.add(result.getString(1));
            }
            catch (Exception ex)
            {
                if (ex.getMessage().startsWith(ORACLE_CANCELLATION) || ex.getMessage().contains("user request"))
                {
                    // Ignore Oracle/PostgreSQL error: user requested cancel of current operation
                }
                else
                    throw ex;
            }
            finally
            {
                removeFromCancellation(statement);
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
    public Iterator<VType> getRawValues(String name, Instant start, Instant end)
            throws UnknownChannelException, Exception
    {
        // TODO Auto-generated method stub
        return null;
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
        }

    }

    @Override
    public void close()
    {
        cancel();
        pool.clear();
    }
}
