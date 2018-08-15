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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/** Jetty-based Web Server Demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WebServerDemo extends AbstractHandler
{
    private static CountDownLatch done = new CountDownLatch(1);

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException
    {
        System.out.println("Web client " + baseRequest.getRemoteHost() + ":" + baseRequest.getRemotePort() + " " + baseRequest);
        final String path = baseRequest.getRequestURI();
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        final PrintWriter out = response.getWriter();
        if ("/stop".equalsIgnoreCase(path))
        {
            out.println("<h1>Bye!</h1>");
            done.countDown();
        }
        else
        {
            out.println("<h1>Hello World</h1>");
            out.println("<a href='/stop'>Stop...</a>");
        }
    }

    public static void main(String[] args) throws Exception
    {
        final Server server = new Server(8080);
        server.setHandler(new WebServerDemo());
        server.start();

        System.out.println("Running web server, check http://localhost:8080");
        do
            System.out.println("Main thread could do other things while web server is running...");
        while (! done.await(5, TimeUnit.SECONDS));
        server.stop();
        server.join();
        System.out.println("Done.");
    }
}
