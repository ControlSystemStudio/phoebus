/*******************************************************************************
 * Copyright (c) 2013-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.httpd;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamWriter;

import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.server.ScanServer;
import org.csstudio.scan.server.ScanServerInstance;

/** Servlet for "/server/*": General {@link ScanServer} info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ServerServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException
    {
        final RequestPath path = new RequestPath(request);
        try
        {
            if (path.size() != 1)
                throw new Exception("Missing '/server/*' request detail");
            final String detail = path.getString(0);
            if ("info".equals(detail))
            {
                final ScanServerInfo info = ScanServerInstance.getScanServer().getInfo();
                final XMLStreamWriter writer = ServletHelper.createXML(response);
                ServletHelper.write(writer, info);
                ServletHelper.submitXML(writer);
            }
            else if ("shutdown".equals(detail))
            {
                logger.log(Level.INFO, "Shutdown from " + request.getRemoteHost() + ":" + request.getRemotePort());
                final XMLStreamWriter writer = ServletHelper.createXML(response);
                writer.writeEmptyElement("bye");
                ServletHelper.submitXML(writer);
                ScanServerInstance.stop();
            }
            else
                throw new Exception("Invalid request /server/" + detail);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "GET /server error", ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
    }
}
