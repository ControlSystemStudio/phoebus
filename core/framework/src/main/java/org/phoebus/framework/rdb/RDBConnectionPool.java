/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.rdb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    /** Total connection count handled by all pools */
    private static final ConcurrentHashMap<Connection, Exception> total_connections =
            logger.isLoggable(Level.FINE) ? new ConcurrentHashMap<>() : null;

    /** Pool instance counter to get unique instance number to each pool */
    private static final AtomicInteger instances = new AtomicInteger();

    private static final ScheduledExecutorService clear_timer = Executors.newSingleThreadScheduledExecutor(target ->
    {
        final Thread thread = new Thread(target, "RDBConnectionPool Cleanup");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean closed = false;
    private final int instance;
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
        instance = instances.incrementAndGet();
        this.info = new RDBInfo(url, user, password);
        logger.log(Level.INFO, "New " + this);
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
        if (closed)
            throw new Exception(this + " is closed");
        Connection connection = pop();
        while (connection != null)
        {   // Is existing connection still good?
            if (connection.isValid(5))
            {
                logger.log(Level.FINER, () -> "Reusing connection of " + this);
                if (total_connections != null)
                    total_connections.put(connection, new Exception("Reuse connection " + this));
                return connection;
            }
            // Close it
            close(connection);
            // Check next existing connection
            connection = pop();
        }

        // No suitable existing connection, create new one
        connection = info.connect();
        if (total_connections != null)
        {
            total_connections.put(connection, new Exception("Open connection " + this));
            final int t = total_connections.size();
            logger.log(Level.FINE, "New total connection count: " + t);
        }
        return connection;
    }

    /** @param connection Connection that is released to the pool for re-use (or time out) */
    public void releaseConnection(final Connection connection)
    {
        if (closed)
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                // Ignore, closing anyway
            }
            logger.log(Level.INFO, this + " is closed", new Exception("Call stack"));
        }
        push(connection);
        logger.log(Level.FINER, "Released connection into " + this);
        final Future<?> previous = cleanup.getAndSet(clear_timer.schedule(this::clear, timeout, TimeUnit.SECONDS));
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

    /** Close the pool, close all connections.
     *  Pool can not be used after calling this.
     */
    public void clear()
    {
        closed = true;
        synchronized (this)
        {
            final Iterator<Connection> iterator = pool.iterator();
            while (iterator.hasNext())
            {
                final Connection connection = iterator.next();
                iterator.remove();
                close(connection);
            }
            pool.clear();
        }
        logger.log(Level.FINE, () -> "Cleared " + this);

        // In case a timer was running, cancel
        final Future<?> previous = cleanup.getAndSet(null);
        if (previous != null)
            previous.cancel(false);
    }

    private void close(final Connection connection)
    {
        try
        {
            logger.log(Level.FINE, () -> "Closing connection of " + this);
            connection.close();
        }
        catch (Exception ex)
        {
            // Ignore, closing anyway?
            logger.log(Level.FINE, "Error closing RDB connection", ex);
        }

        if (total_connections != null)
        {
            total_connections.remove(connection);
            for (Exception ex : total_connections.values())
                ex.printStackTrace();
            final int t = total_connections.size();
            logger.log(Level.FINE, "Remaining total connection count: " + t);
        }
    }

    @Override
    public String toString()
    {
        return "RDBConnectionPool-" + info.getUser() + "-" + info.getDialect() + "-" + instance + ", size " + pool.size();
    }
}
