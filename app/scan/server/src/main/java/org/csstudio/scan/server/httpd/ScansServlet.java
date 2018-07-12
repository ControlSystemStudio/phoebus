/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.httpd;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamWriter;

import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.server.ScanServer;
import org.csstudio.scan.server.ScanServerInstance;

/** Servlet for "/scans": listing scans, remove completed
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScansServlet extends HttpServlet
{
    private final static long serialVersionUID = 1L;
    private final ScanServer scan_server = ScanServerInstance.getScanServer();

    /** Get scan information
     *  <p>GET scans - get all scan infos
     */
    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException
    {
        final List<ScanInfo> scans;
        try
        {
            scans = scan_server.getScanInfos();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "GET /scans error", ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
        try
        {
            final XMLStreamWriter writer = ServletHelper.createXML(response);
            writer.writeStartElement("scans");
            for (ScanInfo info : scans)
                ServletHelper.write(writer, info);
            writer.writeEndElement();
            ServletHelper.submitXML(writer);

        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "GET /scans reply error", ex);
            // Can't send error to client because sending to client is the problem
        }
    }

    /** DELETE scans/completed: Remove completed scans
     *  Returns basic HTTP OK (200) on success, otherwise error
     */
    @Override
    protected void doDelete(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException
    {
        final String path = request.getPathInfo();
        try
        {
            if (! "/completed".equals(path))
                throw new Exception("Illegal path '/scans" + path + "'");
            scan_server.removeCompletedScans();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "DELETE /scans/completed error", ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
    }
}
