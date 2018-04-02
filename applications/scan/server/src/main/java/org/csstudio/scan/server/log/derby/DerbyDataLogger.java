/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log.derby;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

//import org.apache.derby.drda.NetworkServerControl;

/** Derby-based sample logger
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DerbyDataLogger extends RDBDataLogger
{
    final private static String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

//    private static NetworkServerControl network_server = null;

    /** Database startup
     *
     *  <p>Must be called once before accessing Derby
     *  @throws Exception on error
     */
    public static void startup(final String location) throws Exception
    {
        // Path where Derby creates databases
        System.setProperty("derby.system.home", location);

        logger.info("Derby Data Log directory: " + System.getProperty("derby.system.home"));

        // Debug options
        // System.setProperty("derby.language.logStatementText", "true");
        // System.setProperty("derby.language.logQueryPlan", "true");
        System.setProperty("derby.drda.logConnections", "true");

        // Add network data server to embedded database instance
        // to allow connections via 'ij' for debugging.

        // Simply setting a property to request the net. server "works"
        // but causes many BundleExceptions when network server threads
        // try to load classes while the plugin is still in the middle of startup
        //   System.setProperty("derby.drda.startNetworkServer", "true");
        // So it's started programmatically below.

        // Load driver for 'embedded' Derby RDB
        // newInstance is not needed the _first_ time
        // this is called, but after the driver had
        // been shut down, it will unregister and
        // then newInstance is required to re-start.
        Class.forName(DERBY_DRIVER).getDeclaredConstructor().newInstance();

        final DerbyDataLogger database = new DerbyDataLogger();
        try
        {
            if (! database.haveTables())
                database.createTables();
        }
        finally
        {
            database.close();
        }

        // Start network data server programmatically
//        final int port = Preferences.getServerPort();
//        if (port > 0)
//        {
//            network_server = new NetworkServerControl(InetAddress.getByName("localhost"), port);
//            network_server.start(new PrintWriter(System.out, true));
//        }
    }

    /** Initialize
     *  @throws Exception on error
     */
    public DerbyDataLogger() throws Exception
    {
        // only calls super();
    }

    /** Connect to Derby RDB
     *  @return Connection
     *  @throws Exception on error
     */
    @Override
    protected Connection connect() throws Exception
    {
        // Connect
        return DriverManager.getConnection("jdbc:derby:scan;create=true");
    }

    /** Check if database tables need to be created
     *  @return <code>true</code> if tables appear to be present
     *  @throws Exception on error
     */
    private boolean haveTables() throws SQLException
    {
        final Statement statement = connection.createStatement();
        try
        {
            statement.executeQuery("SELECT * FROM scans WHERE id=1");
            return true;
        }
        catch (SQLException ex)
        {
            if ("42X05".equals(ex.getSQLState()))
                return false;
            throw ex;
        }
        finally
        {
            statement.close();
        }
    }

    /** Create database tables
     *  @throws Exception on error
     */
    private void createTables() throws Exception
    {
        final Statement statement = connection.createStatement();
        try
        {
            logger.info("Creating new database tables");

            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream("/config/scanlog.dbd")                        )
                    );
            StringBuilder cmd = new StringBuilder();

            String line = reader.readLine();
            while (line != null)
            {
                line = line.trim();
                // Skip comments
                if (! line.startsWith("--"))
                {
                    if (line.endsWith(";"))
                    {    // Found end of command
                        cmd.append(line.substring(0, line.length()-1));
                        if (cmd.length() > 0)
                        {
                            final String sql = cmd.toString();
                            try
                            {
                                statement.execute(sql);
                            }
                            catch (SQLException ex)
                            {
                                // Ignore RROR 42Y55: 'DROP TABLE' cannot be performed on '...' because it does not exist.
                                if (! ex.getSQLState().equals("42Y55"))
                                    logger.log(Level.INFO, "SQL failed: " + sql, ex);
                            }
                        }
                        cmd = new StringBuilder();
                    }
                    else
                        cmd.append(line);
                }
                line = reader.readLine();
            }
            reader.close();
        }
        finally
        {
            statement.close();
        }
    }

    /** Shut down database.
     *
     *  <p>Must be called to release resource when Derby is no longer required.
     *  @throws Exception on error
     */
    public static void shutdown() throws Exception
    {
        if (DERBY_DRIVER.contains("EmbeddedDriver"))
        {
            logger.info("Closing Derby Data Log");
//            // Stop network database server
//            if (network_server != null)
//            {
//                network_server.shutdown();
//                network_server = null;
//            }

            // Perform shutdown of embedded RDB
            try
            {
                // Strange: Shutdown is initiated via connection,
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
                // and success is reported via exception, so should not get here:
                throw new Exception("Derby did not report shutdown");
            }
            catch (SQLException ex)
            {
                if  (! "XJ015".equals(ex.getSQLState()))
                    throw new Exception("Derby database shutdown failed", ex);
            }
        }
        // else: External RDB, nothing to shut down
    }
}
