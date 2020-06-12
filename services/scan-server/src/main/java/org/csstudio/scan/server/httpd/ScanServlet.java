/*******************************************************************************
 * Copyright (c) 2012-2020 Oak Ridge National Laboratory.
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
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamWriter;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.server.ScanServer;
import org.csstudio.scan.server.ScanServerInstance;
import org.csstudio.scan.util.IOUtils;
import org.csstudio.scan.util.StringOrDouble;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Servlet for "/scan/*": submitting a new scan, deleting (aborting) a current one
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanServlet extends HttpServlet
{
    private final static long serialVersionUID = 1L;
    private final ScanServer scan_server = ScanServerInstance.getScanServer();

    /** POST scan/{name}: Submit a new, named, scan
     *  Returns ID of new scan
     */
    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException
    {
        // Require XML: "text/xml", "text/xml; charset=UTF-8", ...
        final String format = request.getContentType();
        logger.log(Level.FINE, () -> "POST scan " + format);
        if (! format.contains("/xml"))
        {
            logger.log(Level.WARNING, "POST /scan got format '" + format + "'");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expecting XML content with scan, got format '" + format + "'");
            return;
        }

        // Determine name of scan
        String scan_name = request.getPathInfo();
        if (scan_name == null)
            scan_name = "Scan from " + request.getRemoteHost();
        else
        {
            if (! scan_name.startsWith("/"))
                throw new Error("Path does not start with '/'");
            scan_name = scan_name.substring(1);
        }

        // Queue unless "?queue=false"
        final String queue_parm = request.getParameter("queue");
        final boolean queue = ! "false".equalsIgnoreCase(queue_parm);

        // Execute pre/post commands unless "?pre_post=false"
        final String pre_post_parm = request.getParameter("pre_post");
        final boolean pre_post = ! "false".equalsIgnoreCase(pre_post_parm);

        // Read scan commands
        final String scan_commands = IOUtils.toString(request.getInputStream());

        // Return <id> or <error>
        response.setContentType("text/xml");
        final PrintWriter out = response.getWriter();

        // Submit scan
        try
        {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Scan '" + scan_name + "':\n" + scan_commands);

            final long scan_id = scan_server.submitScan(scan_name, scan_commands, queue, pre_post);

            // Return scan ID
            out.print("<id>");
            out.print(scan_id);
            out.println("</id>");
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "POST /scan error", ex);
            response.resetBuffer();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("<error>");
            out.println("<message>" + scan_name + " failed to submit" + "</message>");
            out.println("<trace>");
            final StringWriter buf = new StringWriter();
            ex.printStackTrace(new PrintWriter(buf));
            out.println(buf.toString().replace("<", "&lt;"));
            out.println("</trace>");
            out.println("</error>");
            response.flushBuffer();
        }
    }

    /** 'Put' scan into new state
     *  <p>PUT scan/{id}/next: Force transition to next command
     *  <p>PUT scan/{id}/pause: Pause running scan
     *  <p>PUT scan/{id}/resume: Resume paused scan
     *  <p>PUT scan/{id}/abort: Abort running or paused scan
     *  <p>PUT scan/{id}/patch: Update property of a scan command<br>
     *     Requires description of what to update:
     *     <pre>
     *     &lt;patch>
     *        &lt;address>10&lt;/address>
     *        &lt;property>name_of_property&lt;/property>
     *        &lt;value>new_value&lt;/value>
     *     &lt;/patch>
     *     </pre>
     *  Returns basic HTTP OK (200) on success, otherwise error
     */
    @Override
    protected void doPut(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException
    {
        final RequestPath path = new RequestPath(request);
        try
        {
            if (path.size() < 2)
                throw new Exception("Missing scan ID and command");
            final long id = path.getLong(0);
            final String command = path.getString(1);

            if ("move".equals(command))
            {
                if (path.size() != 3)
                    throw new Exception("Expecting steps, got " + request.getPathInfo());
                final int steps = (int) path.getLong(2);
                scan_server.move(id, steps);
            }
            else
            {
                if (path.size() != 2)
                    throw new Exception("Expecting scan ID and command, got " + request.getPathInfo());
                switch (command)
                {
                case "next":
                    scan_server.next(id);
                    break;
                case "pause":
                    scan_server.pause(id);
                    break;
                case "resume":
                    scan_server.resume(id);
                    break;
                case "abort":
                    scan_server.abort(id);
                    break;
                case "patch":
                    final Element xml = XMLUtil.openXMLDocument(request.getInputStream(), "patch");
                    final long address = XMLUtil.getChildLong(xml, "address").orElse(-1L);
                    final String property = XMLUtil.getChildString(xml, "property").orElse("");
                    final Object value = StringOrDouble.parse(XMLUtil.getChildString(xml, "value").orElse("0"));
                    if (property.isEmpty())
                        throw new Exception("Missing <property> for patch");
                    scan_server.updateScanProperty(id, address, property, value);
                    break;
                default:
                    throw new Exception("Unknown command '" + command + "'");
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PUT /scan error", ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
    }

    /** DELETE scan/{id}: Remove a scan
     *  Returns basic HTTP OK (200) on success, otherwise error
     */
    @Override
    protected void doDelete(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException
    {
        // Determine scan ID and requested object
        final RequestPath path = new RequestPath(request);
        try
        {
            if (path.size() != 1)
                throw new Exception("Missing scan ID");
            final long id = path.getLong(0);
            scan_server.remove(id);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "DELETE /scan error", ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        }
    }

    /** Get scan information
     *  <p>GET scan/{id} - get scan info
     *  <p>GET scan/{id}/commands - get scan commands
     *  <p>GET scan/{id}/data - get scan data
     *  <p>GET scan/{id}/last_serial - get scan data's last serial
     *  <p>GET scan/{id}/devices - get devices used by a scan
     */
    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException
    {
        // Determine scan ID and requested object
        final RequestPath path = new RequestPath(request);
        final long id;
        try
        {
            if (path.size() < 1)
                throw new Exception("Missing scan ID");
            id = path.getLong(0);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "GET /scan error", ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }

        // Return requested object
        final String object = path.size() < 2 ? null : path.getString(1);
        try
        {
            if (object == null)
            {   // Get Scan info
                final ScanInfo info = scan_server.getScanInfo(id);
                if (info == null)
                    throw new Exception("Unknown scan ID " + id);
                final XMLStreamWriter writer = ServletHelper.createXML(response);
                ServletHelper.write(writer, info);
                ServletHelper.submitXML(writer);
            }
            else if ("commands".equalsIgnoreCase(object))
            {   // Get commands
                response.setContentType("text/xml");
                final ServletOutputStream out = response.getOutputStream();
                out.print(scan_server.getScanCommands(id));
                out.flush();
            }
            else if ("data".equalsIgnoreCase(object))
            {   // Get data
                final ScanData data = scan_server.getScanData(id);
                final XMLStreamWriter writer = ServletHelper.createXML(response);
                ServletHelper.write(writer, data);
                ServletHelper.submitXML(writer);
            }
            else if ("last_serial".equalsIgnoreCase(object))
            {   // Get last serial of data
                final long last_serial = scan_server.getLastScanDataSerial(id);
                response.setContentType("text/xml");
                final ServletOutputStream out = response.getOutputStream();
                out.print("<serial>" + last_serial + "</serial>");
                out.flush();
            }
            else if ("devices".equalsIgnoreCase(object))
            {   // Get devices
                final List<DeviceInfo> devices = scan_server.getDeviceInfos(id);
                final XMLStreamWriter writer = ServletHelper.createXML(response);
                ServletHelper.write(writer, devices);
                ServletHelper.submitXML(writer);
            }
            else
                throw new Exception("Unknown request object " + object);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "GET /scan error", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
}
