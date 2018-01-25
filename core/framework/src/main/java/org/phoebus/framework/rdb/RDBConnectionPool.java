/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.rdb;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.rdb.RDBInfo.Dialect;

/** Pool for an RDB connection
 *
 *  <p>Maintains connections to a URL/name/password.
 *  Connections returned to the pool are kept open
 *  until they either get re-used within some time,
 *  or released after a time out.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBConnectionPool
{
    /** Logger for the package */
    public static final Logger logger = Logger.getLogger(RDBConnectionPool.class.getPackageName());

    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(target ->
    {
        final Thread thread = new Thread(target, "RDBConnectionPool");
        thread.setDaemon(true);
        return thread;
    });

    private final RDBInfo info;
    private final List<Connection> pool = new ArrayList<>();

    private final AtomicReference<Future<?>> cleanup = new AtomicReference<>();

    private volatile int timeout = 10;

    /** Create connection pool
     *
     *  <p>URL format depends on the database dialect.
     *
     *  <p>For MySQL resp. Oracle, the formats are:
     *  <pre>
     *     jdbc:mysql://[host]:[port]/[database]?user=[user]&password=[password]
     *     jdbc:oracle:thin:[user]/[password]@//[host]:[port]/[database]
     *  </pre>
     *
     *  For Oracle, the port is usually 1521.
     *
     *  @param url Database URL
     *  @param user User name or <code>null</code> if part of URL
     *  @param password Password or <code>null</code> if part of URL
     *  @throws Exception on error
     */
    public RDBConnectionPool(final String url, final String user, final String password) throws Exception
    {
        this.info = new RDBInfo(url, user, password);
    }

    /** @return Database dialect */
    public Dialect getDialect()
    {
        return info.getDialect();
    }

    /** @param seconds Duration for keeping idle connections in pool */
    public void setTimeout(final int seconds)
    {
        timeout = seconds;
    }

    /** @return Time for which idle connections are cached */
    public int getTimeoutSeconds()
    {
        return timeout;
    }

    /** Get a connection
     *
     *  <p>The connection should not be closed
     *  but returned to the pool.
     *
     *  @return {@link Connection}
     *  @throws Exception on error
     *  @see #releaseConnection(Connection)
     */
    public Connection getConnection() throws Exception
    {
        Connection connection = pop();
        while (connection != null)
        {   // Is existing connection still good?
            if (connection.isValid(5))
                return connection;
            // Close it
            close(connection);
            // Check next existing connection
            connection = pop();
        }

        // No suitable existing connection, create new one
        return info.connect();
    }

    /** @param connection Connection that is released to the pool for re-use (or time out) */
    public void releaseConnection(final Connection connection)
    {
        push(connection);
        final Future<?> previous = cleanup.getAndSet(timer.schedule(this::clear, timeout, TimeUnit.SECONDS));
        if (previous != null)
            previous.cancel(false);
    }

    private synchronized void push(final Connection connection)
    {
        pool.add(connection);
    }

    private synchronized Connection pop()
    {
        int last = pool.size() - 1;
        if (last >= 0)
            return pool.remove(last);
        return null;
    }

    /** Clear the pool, close all connections */
    public synchronized void clear()
    {
        for (Connection connection : pool)
            close(connection);
        pool.clear();
    }

    private void close(final Connection connection)
    {
        try
        {
            logger.log(Level.FINE, "Closing " + connection.getMetaData().getDatabaseProductName());
            connection.close();
        }
        catch (Exception ex)
        {
            // Ignore, closing anyway?
            logger.log(Level.FINE, "Error closing RDB connection", ex);
        }
    }
}
