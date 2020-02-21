/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.rdb;

import static org.phoebus.framework.rdb.RDBConnectionPool.logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.logging.Level;


/** Information about an RDB connection
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBInfo
{
    /** Start of MySQL URL, including " jdbc:mysql:replication://.." */
    private static final String JDBC_MYSQL = "jdbc:mysql:";

    /** Start of PostgreSQL URL */
    private static final String JDBC_POSTGRESQL = "jdbc:postgresql:";

    /** Start of Oracle URL */
    private static final String JDBC_ORACLE = "jdbc:oracle:";

    /** Database dialect.
     *  For starters, the connection mechanisms vary, and since
     *  SQL isn't fully normed, there might be more differences
     *  that we need to handle, so we keep track of the dialect.
     */
    public enum Dialect
    {
        /** Database that understands MySQL commands */
        MySQL,
        /** Database that understands Oracle commands */
        Oracle,
        /** Database that understands PostgreSQL commands */
        PostgreSQL
    };

    private final String url, user, password;
    private final Dialect dialect;

    public RDBInfo(final String url, final String user, final String password) throws Exception
    {
        this.url = url;
        this.user = user;
        this.password = password;

        if (url.startsWith(JDBC_MYSQL))
            dialect = Dialect.MySQL;
        else if (url.startsWith(JDBC_POSTGRESQL))
            dialect = Dialect.PostgreSQL;
        else if (url.startsWith(JDBC_ORACLE))
            dialect = Dialect.Oracle;
        else
            throw new Exception("Unknown database dialect " + url);
    }

    /** @return {@link Dialect}  */
    public Dialect getDialect()
    {
        return dialect;
    }

    /** @return user */
    public String getUser()
    {
        return user;
    }

    /** Create a new {@link Connection} */
    public Connection connect() throws Exception
    {
        Connection connection = null;

        final Properties prop = new Properties();
        if (user != null)
            prop.put("user", user);
        if (password != null)
            prop.put("password", password);

        if (dialect == Dialect.Oracle)
        {
            // Get class loader to find the driver
            // Class.forName("oracle.jdbc.driver.OracleDriver").getDeclaredConstructor().newInstance();
            // Connect such that Java float and double map to Oracle
            // BINARY_FLOAT resp. BINARY_DOUBLE
            prop.put("SetFloatAndDoubleUseBinary", "true");
        }
        else if (dialect == Dialect.MySQL)
        {
            if (url.startsWith("jdbc:mysql:replication"))
            {
                // Use ReplicationDriver based on code by Laurent Philippe
                // in org.csstudio.platform.utility.rdb.internal.MySQL_RDB
                Driver driver = (Driver)Class.forName("com.mysql.jdbc.ReplicationDriver").getDeclaredConstructor().newInstance();

                // We want this for failover on the slaves
                prop.put("autoReconnect", "true");

                // We want to load balance between the slaves
                prop.put("roundRobinLoadBalance", "true");

                connection = driver.connect(url, prop);
            }
        }
        else if (dialect == Dialect.PostgreSQL)
        {
            // Required to locate driver?
            Class.forName("org.postgresql.Driver").getDeclaredConstructor().newInstance();
        }

        // Unless connection was created by dialect-specific code,
        // use generic driver manager
        if (connection == null)
        {
            try
            {
                connection = DriverManager.getConnection(url, prop);
            }
            catch (Exception ex)
            {
                throw new Exception("Cannot connect to " + url + " as " + user, ex);
            }
        }

        // Basic database info
        if (logger.isLoggable(Level.FINE))
        {
            final DatabaseMetaData meta = connection.getMetaData();
            logger.fine(dialect + " connection: " +
                        meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        }
        return connection;
    }
}
