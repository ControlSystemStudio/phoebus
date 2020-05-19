/*******************************************************************************
 * Copyright (c) 2013-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.httpd;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.csstudio.scan.info.SimulationResult;
import org.csstudio.scan.server.ScanServer;
import org.csstudio.scan.server.ScanServerInstance;
import org.csstudio.scan.util.IOUtils;

/** Servlet for "/simulate": submitting a scan for simulation
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimulateServlet extends HttpServlet
{
    private final static long serialVersionUID = 1L;
    private final ScanServer scan_server = ScanServerInstance.getScanServer();

    /** POST simulate: Submit a scan for simulation
     *  Returns of the simulation
     */
    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException
    {
        // Require XML: "text/xml", "text/xml; charset=UTF-8", ...
        final String format = request.getContentType();
        logger.log(Level.FINE, () -> "POST simulate " + format);
        if (! format.contains("/xml"))
        {
            logger.log(Level.WARNING, "POST /simulate got format '" + format + "'");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expecting XML content with scan, got format '" + format + "'");
            return;
        }

        response.setContentType("text/xml");
        final PrintWriter out = response.getWriter();

        // Read scan commands
        final String scan_commands = IOUtils.toString(request.getInputStream());

        // Simulate scan
        try
        {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Commands for simulation:\n" + scan_commands);

            final SimulationResult simulation = scan_server.simulateScan(scan_commands);
            // Return scan ID
            out.println("<simulation>");
            out.print("  <log>");
            out.print("<![CDATA[");
            out.print(simulation.getSimulationLog());
            out.println("]]>");
            out.println("  </log>");
            out.println("  <seconds>" + simulation.getSimulationSeconds() + "</seconds>");
            out.println("</simulation>");
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "POST /simulate error", ex);
            response.resetBuffer();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("<error>");
            out.println("<message>Failed to simulate</message>");
            out.println("<trace>");
            final StringWriter buf = new StringWriter();
            ex.printStackTrace(new PrintWriter(buf));
            out.println(buf.toString().replace("<", "&lt;"));
            out.println("</trace>");
            out.println("</error>");
            response.flushBuffer();
        }
    }
}
