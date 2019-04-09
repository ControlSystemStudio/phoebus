/*******************************************************************************
 * Copyright (c) 2011-2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.client;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.Scan;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.info.SimulationResult;
import org.csstudio.scan.util.IOUtils;
import org.csstudio.scan.util.PathUtil;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Client for remotely accessing the scan server.
 *
 *  <p>Uses the REST-based web interface of the
 *  scan server to submit new scans, monitor
 *  and control existing scans.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanClient
{
    /** Default port used by scan server's REST interface */
    public final static int DEFAULT_PORT = 4810;

    /** Serial returned if there is no data, yet */
    public static final long NO_DATA_SERIAL = -1;

    /** Serial returned if scan ID is unknown */
    public static final long UNKNOWN_SCAN_SERIAL = -2;

    final private String host;
    final private int port;

    /** Timeout in seconds for most operations */
    final private int default_timeout = 10;

    /** Timeout in seconds for operations that tend to take longer */
    final private int long_timeout = 120;

    /** Initialize
     *  @param host Scan server host
     *  @param port Scan server port
     */
    public ScanClient(final String host, final int port)
    {
        this.host = host;
        this.port = port;
    }

    /** @return Scan server host */
    public String getHost()
    {
        return host;
    }

    /** @return Scan server port */
    public int getPort()
    {
        return port;
    }

    /** Connect to "http://server:port/path"
     *  @param path Path to use in scan server REST interface
     *  @param timeout_seconds Timeout to use for operations
     *  @return {@link HttpURLConnection}
     *  @throws Exception on error
     */
    private HttpURLConnection connect(final String path, final String query, final int timeout_seconds) throws Exception
    {
        // URI will properly escape content of path
        final URI uri = new URI("http", null, host, port, path, query, null);
        final URL url = uri.toURL();
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setReadTimeout((int) SECONDS.toMillis(timeout_seconds));
        return connection;
    }

    /** Connect to "http://server:port/path"
     *  @param path Path to use in scan server REST interface
     *  @return {@link HttpURLConnection}
     *  @throws Exception on error
     */
    private HttpURLConnection connect(final String path) throws Exception
    {
        return connect(path, null, default_timeout);
    }

    /** POST data to connection
     *  @param connection {@link HttpURLConnection}
     *  @param text Data to post
     *  @throws Exception on error
     */
    private void post(final HttpURLConnection connection, final String text) throws Exception
    {
        writeData(connection, "POST", text);
    }

    /** Write data to connection
     *  @param connection {@link HttpURLConnection}
     *  @param text Data to post
     *  @throws Exception on error
     */
    private void writeData(final HttpURLConnection connection, final String method, final String text) throws Exception
    {
        connection.setDoOutput(true);
        connection.setRequestMethod(method);
        final OutputStream body = connection.getOutputStream();
        body.write(text.getBytes());
        body.flush();
        body.close();
    }

    /** Check HTTP response
     *  @param connection {@link HttpURLConnection}
     *  @throws Exception Anything but "2xx" results in exception
     */
    private void checkResponse(final HttpURLConnection connection) throws Exception
    {
        final int code = connection.getResponseCode();
        if (code >= 200  &&  code < 300)
            return;

        // Some error. Check for detail
        if (connection.getContentType().contains("xml"))
        {
            final Element root_node = parseXML(connection.getErrorStream());
            if ("error".equals(root_node.getNodeName()))
            {
                final String message = XMLUtil.getChildString(root_node, "message").orElse("Error");
                final String trace = XMLUtil.getChildString(root_node, "trace").orElse("- no trace -");
                // Turning the stack trace into the message of a nested exception,
                // because that looks like
                // "Exception in client: ...
                //  cause by: Stack trace on server
                throw new Exception(message, new Exception(trace));
            }
        }

        throw new Exception("HTTP Response code " + code + " (" + connection.getResponseMessage() + ")");
    }

    /** Parse XML document from connection
     *  @param stream {@link HttpURLConnection}'s input or error stream
     *  @return Root {@link Element} for parsed XML
     *  @throws Exception on error
     */
    private Element parseXML(final InputStream stream)
            throws Exception
    {
        final DocumentBuilder docBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(stream);
        doc.getDocumentElement().normalize();
        final Element root_node = doc.getDocumentElement();
        return root_node;
    }

    /** Parse ScanInfo from XML
     *  @param node Node that should contain {@link ScanInfo}
     *  @return {@link ScanInfo}
     *  @throws Exception on error
     */
    private ScanInfo parseScanInfo(final Element node) throws Exception
    {
        final int id = XMLUtil.getChildInteger(node, "id").orElse(-1);
        final String name = XMLUtil.getChildString(node, "name").orElse("");
        final Instant created = Instant.ofEpochMilli(XMLUtil.getChildLong(node, "created").orElse(0l));
        final ScanState state = ScanState.valueOf(XMLUtil.getChildString(node, "state").orElse(ScanState.Logged.name()));
        final Optional<String> error = XMLUtil.getChildString(node, "error");
        final long runtime_ms = XMLUtil.getChildLong(node, "runtime").orElse(0l);
        final long total_work_units = XMLUtil.getChildLong(node, "total_work_units").orElse(0l);
        final long performed_work_units = XMLUtil.getChildLong(node, "performed_work_units").orElse(0l);
        final long finishtime_ms = XMLUtil.getChildLong(node, "finish").orElse(0l);
        final long current_address = XMLUtil.getChildLong(node, "address").orElse(0l);
        final String current_commmand = XMLUtil.getChildString(node, "command").orElse("");

        final Scan scan = new Scan(id, name, created);
        return new ScanInfo(scan, state, error, runtime_ms, finishtime_ms,
                performed_work_units, total_work_units, current_address, current_commmand);
    }

    /** Obtain overall scan server information
     *  @return {@link ScanServerInfo}
     *  @throws Exception on error
     */
    public ScanServerInfo getServerInfo() throws Exception
    {
        final HttpURLConnection connection = connect("/server/info");
        try
        {
            checkResponse(connection);
            final Element root_node = parseXML(connection.getInputStream());
            if (! "server".equals(root_node.getNodeName()))
                throw new Exception("Expected <server/>");

            final String version = XMLUtil.getChildString(root_node, "version").orElse("?");
            final Instant start_time = Instant.ofEpochMilli(XMLUtil.getChildLong(root_node, "start_time").orElse(0l));
            // Support deprecated server that still uses beamline_config
            final String scan_config = XMLUtil.getChildString(root_node, "scan_config")
                                              .orElse(XMLUtil.getChildString(root_node, "beamline_config").orElse(""));
            final long used_mem = XMLUtil.getChildLong(root_node, "used_mem").orElse(0l);
            final long max_mem = XMLUtil.getChildLong(root_node, "max_mem").orElse(0l);
            final long non_heap = XMLUtil.getChildLong(root_node, "non_heap").orElse(0l);
            final List<String> paths = PathUtil.splitPath(XMLUtil.getChildString(root_node, "script_paths").orElse(""));
            final String macros = XMLUtil.getChildString(root_node, "macros").orElse("");
            return new ScanServerInfo(version, start_time, scan_config, paths, macros, used_mem, max_mem, non_heap);
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Obtain information for all scans
     *  @return {@link List} of {@link ScanInfo}s
     *  @throws Exception on error
     */
    public List<ScanInfo> getScanInfos() throws Exception
    {
        final HttpURLConnection connection = connect("/scans");
        try
        {
            checkResponse(connection);
            final Element root_node = parseXML(connection.getInputStream());
            if (! "scans".equals(root_node.getNodeName()))
                throw new Exception("Expected <scans/>");

            final List<ScanInfo> infos = new ArrayList<>();
            for (Element node : XMLUtil.getChildElements(root_node, "scan"))
            {
                final ScanInfo info = parseScanInfo(node);
                infos.add(info);
            }
            return infos;
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Obtain information for a scan
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @return {@link ScanInfo}
     *  @throws Exception on error
     */
    public ScanInfo getScanInfo(final long id) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + id);
        try
        {
            checkResponse(connection);
            final Element root_node = parseXML(connection.getInputStream());
            if (! "scan".equals(root_node.getNodeName()))
                throw new Exception("Expected <scan/>");
            return parseScanInfo(root_node);
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Obtain commands for a scan
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @return XML text for scan commands
     *  @throws Exception on error
     */
    public String getScanCommands(final long id) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + id + "/commands");
        try
        {
            checkResponse(connection);
            return IOUtils.toString(connection.getInputStream());
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Obtain data logged by a scan
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @return {@link ScanData}
     *  @throws Exception on error
     */
    public ScanData getScanData(final long id) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + id + "/data");
        try
        {
            checkResponse(connection);
            final InputStream stream = connection.getInputStream();
            final ScanDataSAXHandler handler = new ScanDataSAXHandler();
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(stream, handler);
            return handler.getScanData();
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Get serial of last logged sample.
     *
     *  <p>Can be used to determine if there are new samples
     *  that should be fetched via <code>getScanData()</code>
     *
     *  @param id ID that uniquely identifies a scan
     *  @return Serial of last sample in scan data, -1 if nothing has been logged, -2 if scan ID not known
     *  @throws Exception on error
     *  @see #getScanData(long)
     */
    public long getLastScanDataSerial(final long id) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + id + "/last_serial");
        try
        {
            checkResponse(connection);
            final Element root_node = parseXML(connection.getInputStream());
            if (! "serial".equals(root_node.getNodeName()))
                throw new Exception("Expected <serial/>");
            return Long.parseLong(root_node.getFirstChild().getNodeValue());
        }
        catch (Exception ex)
        {
            final String error = ex.getMessage();
            if (error != null  &&  error.contains("Unknown scan ID"))
                return UNKNOWN_SCAN_SERIAL;
            else throw ex;
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Obtain devices used by a scan
     *  @param id ID that uniquely identifies a scan, or -1 to get default devices
     *  @return {@link ScanData}
     *  @throws Exception on error
     */
    public Collection<DeviceInfo> getScanDevices(final long id) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + id + "/devices");
        try
        {
            checkResponse(connection);
            final Element root_node = parseXML(connection.getInputStream());
            if (! "devices".equals(root_node.getNodeName()))
                throw new Exception("Expected <devices/>");
            final Collection<DeviceInfo> devices = new ArrayList<>();
            for (Element node : XMLUtil.getChildElements(root_node, "device"))
            {
                devices.add(
                    new DeviceInfo(
                        XMLUtil.getChildString(node, "name").orElse(null),
                        XMLUtil.getChildString(node, "alias").orElse(""),
                        XMLUtil.getChildString(node, "status").orElse("")));
            }
            return devices;
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Submit a scan for execution
     *  @param name Name of the new scan
     *  @param xml_commands XML commands of the scan to submit
     *  @param queue Submit to queue or for immediate execution?
     *  @return Scan ID
     *  @throws Exception on error
     */
    public long submitScan(final String name, final String xml_commands, final boolean queue) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + name, queue ? "" : "queue=false", long_timeout);
        connection.setReadTimeout(0);
        try
        {
            post(connection, xml_commands);
            checkResponse(connection);
            // Obtain returned scan ID
            final Element root_node = parseXML(connection.getInputStream());
            if (! "id".equals(root_node.getNodeName()))
                throw new Exception("Expected <id/>");
            final long id = Long.parseLong(root_node.getFirstChild().getNodeValue());

            return id;
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Submit a scan for simulation
     *  @param xml_commands XML commands of the scan to submit
     *  @return Scan ID
     *  @throws Exception on error
     */
    public SimulationResult simulateScan(final String xml_commands) throws Exception
    {
        final HttpURLConnection connection = connect("/simulate", null, long_timeout);
        try
        {
            post(connection, xml_commands);
            checkResponse(connection);
            // Decode simulation result
            final Element root_node = parseXML(connection.getInputStream());
            if (! "simulation".equals(root_node.getNodeName()))
                throw new Exception("Expected <simulation/>");
            return new SimulationResult(
                XMLUtil.getChildDouble(root_node, "seconds").orElse(0.0),
                XMLUtil.getChildString(root_node, "log").orElse(""));
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Put scan into different state via command
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @param command Command to send
     *  @throws Exception on error
     */
    private void sendScanCommand(final long id, final String command) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + id + "/" + command);
        try
        {
            connection.setRequestMethod("PUT");
            checkResponse(connection);
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Request transition to next command
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @throws Exception on error
     */
    public void nextCommand(final long id) throws Exception
    {
        sendScanCommand(id, "next");
    }

    /** Put running scan into paused state
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @throws Exception on error
     */
    public void pauseScan(final long id) throws Exception
    {
        sendScanCommand(id, "pause");
    }

    /** Resume a paused scan
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @throws Exception on error
     */
    public void resumeScan(final long id) throws Exception
    {
        sendScanCommand(id, "resume");
    }

    /** Move an idle scan up/down in the list of queued scans
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @param steps Positive to move up
     *  @throws Exception on error
     */
    public void moveScan(final long id, final int steps) throws Exception
    {
        sendScanCommand(id, "move/" + steps);
    }

    /** Abort a running or paused scan
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @throws Exception on error
     */
    public void abortScan(final long id) throws Exception
    {
        sendScanCommand(id, "abort");
    }

    /** Submit patch to running scan
     *  @param id ID that uniquely identifies a scan
     *  @param address Address of command within scan
     *  @param property Property of command to patch
     *  @param value New value for that property
     *  @throws Exception on error
     */
    public void patchScan(final long id, final long address,
            final String property, final Object value) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + id + "/patch");
        try
        {
            final String patch =
                "<patch>"
                + "<address>" + address + "</address>"
                + "<property>" + property + "</property>"
                + "<value>" + value + "</value>"
                + "</patch>";
            writeData(connection, "PUT", patch);
            checkResponse(connection);
        }
        finally
        {
            connection.disconnect();
        }
    }


    /** Remove a completed scan
     *  @param id ID that uniquely identifies a scan (within JVM of the scan engine)
     *  @throws Exception on error
     */
    public void removeScan(final long id) throws Exception
    {
        final HttpURLConnection connection = connect("/scan/" + id);
        try
        {
            connection.setRequestMethod("DELETE");
            checkResponse(connection);
        }
        finally
        {
            connection.disconnect();
        }
    }

    /** Remove all completed scans
     *  @throws Exception on error
     */
    public void removeCompletedScans() throws Exception
    {
        final HttpURLConnection connection = connect("/scans/completed");
        try
        {
            connection.setRequestMethod("DELETE");
            checkResponse(connection);
        }
        finally
        {
            connection.disconnect();
        }
    }
}
