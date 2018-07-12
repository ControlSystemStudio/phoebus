/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.httpd;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

/** Jetty-based Web Server Demo that uses servlets
 *  @author Kay Kasemir
 */
@SuppressWarnings({ "nls", "serial" })
public class WebServletsDemo
{
    private static CountDownLatch done = new CountDownLatch(1);

    public static class HelloServlet extends HttpServlet
    {
        public HelloServlet()
        {
            System.out.println("Creating HelloServlet");
        }

        @Override
        protected void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws ServletException, IOException
        {
            System.out.println("Hello from " + request.getRemoteHost() + ":" + request.getRemotePort() + " " + request);
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            final PrintWriter out = response.getWriter();
            out.println("<h1>Hello World</h1>");
            out.println("<img src='img/scan.png'/>");
            out.println("<a href='/stop'>Stop...</a>");
        }
    }

    public static class StopServlet extends HttpServlet
    {
        public StopServlet()
        {
            System.out.println("Creating StopServlet");
        }

        @Override
        protected void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws ServletException, IOException
        {
            System.out.println("Stop from " + request.getRemoteHost() + ":" + request.getRemotePort() + " " + request);
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            final PrintWriter out = response.getWriter();
            out.println("<h1>Bye!</h1>");
            done.countDown();
        }
    }

    public static void main(String[] args) throws Exception
    {
        final Server server = new Server(8080);

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SECURITY | ServletContextHandler.NO_SESSIONS);

        // Our servlets
        context.addServlet(HelloServlet.class, "/hello");
        context.addServlet(StopServlet.class, "/stop");

        // Serve static files from webroot to "/"
        context.setContextPath("/");
        context.setResourceBase(WebServletsDemo.class.getResource("/webroot").toExternalForm());
        context.addServlet(DefaultServlet.class, "/");

        server.setHandler(context);

        server.start();
        System.out.println("Running web server, check http://localhost:8080, http://localhost:8080/hello");
        do
            System.out.println("Main thread could do other things while web server is running...");
        while (! done.await(5, TimeUnit.SECONDS));
        server.stop();
        server.join();
        System.out.println("Done.");
    }
}
