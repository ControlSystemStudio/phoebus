/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

/** Engine's web server
 *  @author Kay Kasemir
 *  @author Dominic Oram JSON support in previous version
 */
@SuppressWarnings("nls")
public class EngineWebServer
{
    private final Server server;

    public EngineWebServer(final int port)
    {
        // Configure Jetty to use java.util.logging, and don't announce that it's doing that
        System.setProperty("org.eclipse.jetty.util.log.announce", "false");
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.JavaUtilLog");

        server = new Server(port);

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SECURITY | ServletContextHandler.NO_SESSIONS);

        // Our servlets
        context.addServlet(MainServlet.class, "/main/*");
        context.addServlet(DisconnectedServlet.class, "/disconnected/*");
        context.addServlet(GroupsServlet.class, "/groups/*");
        context.addServlet(GroupServlet.class, "/group/*");
        context.addServlet(ChannelServlet.class, "/channel/*");
        context.addServlet(RestartServlet.class, "/restart/*");
        context.addServlet(StopServlet.class, "/stop/*");

        // Serve static files from webroot to "/"
        context.setContextPath("/");
        context.setResourceBase(EngineWebServer.class.getResource("/webroot").toExternalForm());
        context.addServlet(DefaultServlet.class, "/");

        server.setHandler(context);
    }

    public void start() throws Exception
    {
        server.start();
    }

    public void shutdown() throws Exception
    {
        server.stop();
        server.join();
    }
}


