/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.net.InetAddress;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;

/** Singleton class that provides facilities to identify the user name and host name
 *  of who invoked a program.
 *
 *  <p>User is obtained from system property.
 *  Host name is obtained from network library. Since that might be slow,
 *  it's fetched in background thread.
 *
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class IdentificationHelper
{
    private final static String USER_PROPERTY = "user.name";

    private volatile static String USER = "???";
    private volatile static String HOST = "???";

    /** Start background thread that fetches information
     *
     *  <p>Called early on during {@link AlarmSystem} initialization
     */
    public static void initialize()
    {
        final Thread thread = new Thread(IdentificationHelper::fetch, "IdentificationHelper");
        thread.setDaemon(true);
        thread.start();
    }

    private static void fetch()
    {
        try
        {
            USER = System.getProperty(USER_PROPERTY);
            HOST = InetAddress.getLocalHost().getHostName();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot obtain USER and HOST", ex);
        }
    }

    /** @return <code>String</code> - user name if found, "???" otherwise. */
    public static String getUser() { return USER; }

    /** @return <code>String</code> - host name if found, "???" otherwise. */
    public static String getHost() { return HOST; }
}
