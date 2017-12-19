/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.rdb;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

/** Demo of the {@link RDBConnectionPool}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBConnectionTest
{
    private static final String PASS = "sns";
    private static final String USER = "sns_reports";
    private static final String MYSQL_URL = "jdbc:mysql://ics-web.sns.ornl.gov/archive";
    private static final String ORACLE_URL = "jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=OFF)(FAILOVER=ON)" +
            "(ADDRESS=(PROTOCOL=TCP)(HOST=snsappa.sns.ornl.gov)(PORT=1610))" +
            "(ADDRESS=(PROTOCOL=TCP)(HOST=snsappb.sns.ornl.gov)(PORT=1610))" +
            "(CONNECT_DATA=(SERVICE_NAME=prod_controls)))";

    @Before
    public void setup()
    {
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.FINE);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(Level.FINE);
    }

    private void check(final String url, final String query) throws Exception
    {
        final RDBConnectionPool pool = new RDBConnectionPool(url, USER, PASS);
        System.out.println(pool.getDialect());

        final Connection connection = pool.getConnection();
        final PreparedStatement stmt = connection.prepareStatement(query);
        final ResultSet result = stmt.executeQuery();
        int i = 0;
        while (result.next())
            System.out.format("%2d %s\n", ++i, result.getString(1));
        result.close();
        stmt.close();
        pool.releaseConnection(connection);
        pool.clear();
    }
    @Test
    public void testMySQL() throws Exception
    {
        check(MYSQL_URL, "SELECT name FROM channel LIMIT 20");
    }

    @Test
    public void testOracle() throws Exception
    {
        check(ORACLE_URL, "SELECT name FROM chan_arch.channel FETCH FIRST 20 ROWS ONLY");
    }

    @Test
    public void testPooling() throws Exception
    {
        final RDBConnectionPool pool = new RDBConnectionPool(MYSQL_URL, USER, PASS);

        // Get two connections
        Connection connection1 = pool.getConnection();
        Connection connection2 = pool.getConnection();
        assertThat(connection2, not(sameInstance(connection1)));

        // Release the first connection
        pool.releaseConnection(connection1);
        // It should now be re-used
        Connection connection = pool.getConnection();
        assertThat(connection, sameInstance(connection1));
        pool.releaseConnection(connection);

        // Release the second connection
        pool.releaseConnection(connection2);
        pool.clear();

        // Get a connections
        connection1 = pool.getConnection();
        // Close it, either by accident or to simulate a problem
        connection1.close();
        // Release
        pool.releaseConnection(connection1);
        // Will NOT get it back because it's been closed
        connection = pool.getConnection();
        assertThat(connection, not(sameInstance(connection1)));
        pool.releaseConnection(connection);
    }

    @Test
    public void testTimeout() throws Exception
    {
        System.out.println("Timeout Test");
        final RDBConnectionPool pool = new RDBConnectionPool(MYSQL_URL, USER, PASS);
        pool.setTimeout(2);

        Connection connection1 = pool.getConnection();
        pool.releaseConnection(connection1);

        // 'Right now', get the same connection again
        Connection connection = pool.getConnection();
        assertThat(connection, sameInstance(connection1));
        pool.releaseConnection(connection);
        System.out.println("Re-used existing connection");

        // After the expiration time, get a new connection
        TimeUnit.SECONDS.sleep(pool.getTimeoutSeconds() * 2);
        connection = pool.getConnection();
        assertThat(connection, not(sameInstance(connection1)));
        pool.releaseConnection(connection);
        System.out.println("Existing connection closed after timeout");

        // Now keep using that connection within the timeout,
        // to check that it remains open
        connection1 = connection;
        for (int check=0; check < pool.getTimeoutSeconds() * 3; ++check)
        {
            connection = pool.getConnection();
            assertThat(connection, sameInstance(connection1));
            pool.releaseConnection(connection);
            TimeUnit.SECONDS.sleep(1);
            System.out.println("Existing connection kept open");
        }
        pool.clear();
    }
}
