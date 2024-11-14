/*******************************************************************************
 * Copyright (c) 2012-2024 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.httpd;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.time.Instant;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFormatter;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.util.PathUtil;
import org.phoebus.framework.persistence.IndentingXMLStreamWriter;
import org.phoebus.framework.persistence.XMLUtil;

/** Servlet Helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ServletHelper
{
    /** Dump request headers
     *  @param request
     */
    public static void dumpHeaders(final HttpServletRequest request)
    {
        final Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements())
        {
            String header = headers.nextElement();
            System.out.println(header + " = " + request.getHeader(header));
        }
    }

    /** Create XML for HTTP client
     *  @param response {@link HttpServletResponse} to which XML is submitted
     *  @throws Exception on error
     */
    public static XMLStreamWriter createXML(final HttpServletResponse response) throws Exception
    {
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        final XMLStreamWriter base = XMLOutputFactory.newInstance().createXMLStreamWriter(response.getOutputStream(), XMLUtil.ENCODING);
        final IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter(base);
        writer.writeStartDocument(XMLUtil.ENCODING, "1.0");
        return writer;
    }

    /** Create XML element for string
     *  @param writer {@link XMLStreamWriter}
     *  @param name Name of XML element
     *  @param text Text content
     *  @throws Exception on error
     */
    public static void write(final XMLStreamWriter writer, final String name, final String text) throws Exception
    {
        writer.writeStartElement(name);
        writer.writeCharacters(sanitizeXML(text));
        writer.writeEndElement();
    }

    /** Sanitize XML text
     * 
     *  <p>XMLStreamWriter.writeCharacters handles '<', '>', ampersand and potentially double-quotes.
     *  It will not, however, escape 'control' characters like character 7.
     *  One could escape them as "ampersand hashmark 7 semicolon",
     *  but their use is still not full supported in XML, see
     *  https://en.wikipedia.org/wiki/Valid_characters_in_XML
     *  https://stackoverflow.com/questions/404107/why-are-control-characters-illegal-in-xml-1-0
     *  https://learn.microsoft.com/en-us/previous-versions/troubleshoot/msxml/error-when-xml-contains-low-order-ascii
     *  
     *  <p>For example, when viewing the XML in Safari,
     *  that client will un-escape back into character 7
     *  and then complain "invalid XML char value 7".
     *  
     *  <p>This sanitizer will replace such control characters with underscores and log a warning.
     *  
     *  @param text Text like "demo\0x7"
     *  @return Text like "demo_"
     */
    private static String sanitizeXML(final String text)
    {
        final StringBuilder out = new StringBuilder();
        for (int i=0; i<text.length(); ++i)
        {
            char c = text.charAt(i);
            // https://en.wikipedia.org/wiki/Valid_characters_in_XML:
            // U+0009, U+000A, U+000D: these are the only C0 controls accepted in XML 1.0;
            if (c < ' ' && c != '\t' && c != '\r' && c != '\n')
            {
                logger.log(Level.WARNING,
                           String.format("Sanitizing XML for '%s' at index %d (char 0x%X)",
                                         text, i, (int) c));
                out.append('_');
            }
            else
                out.append(c);
        }
        return out.toString();
    }

    /** Create XML element for number
     *  @param writer {@link XMLStreamWriter}
     *  @param name Name of XML element
     *  @param number Number content
     */
    public static void write(final XMLStreamWriter writer, final String name, final long number) throws Exception
    {
        write(writer, name, Long.toString(number));
    }

    /** Create XML element for date, encoded as milliseconds since epoch
     *  @param writer Parent document
     *  @param name Name of XML element
     *  @param date Date content
     */
    public static void write(final XMLStreamWriter writer,final String name, final Instant date) throws Exception
    {
        write(writer, name, date.toEpochMilli());
    }

    /** Create XML content for scan server info
     *  @param writer {@link XMLStreamWriter}
     *  @param info {@link ScanServerInfo}
     *  @throws Exception on error
     */
    public static void write(final XMLStreamWriter writer, final ScanServerInfo info) throws Exception
    {
        writer.writeStartElement("server");

        write(writer, "version", info.getVersion());
        write(writer, "start_time", info.getStartTime());
        write(writer, "scan_config", info.getScanConfig());
        write(writer, "script_paths", PathUtil.joinPaths(info.getScriptPaths()));
        write(writer, "macros", info.getMacros());
        write(writer, "used_mem", info.getUsedMem());
        write(writer, "max_mem", info.getMaxMem());
        write(writer, "non_heap", info.getNonHeapUsedMem());

        writer.writeEndElement();
    }

    public static void write(final XMLStreamWriter writer, final ScanInfo info) throws Exception
    {
        writer.writeStartElement("scan");
        write(writer, "id", info.getId());
        write(writer, "name", info.getName());
        write(writer, "created", info.getCreated());
        write(writer, "state", info.getState().name());
        write(writer, "runtime", info.getRuntimeMillisecs());

        if (info.getTotalWorkUnits() > 0)
        {
            write(writer, "total_work_units", info.getTotalWorkUnits());
            write(writer, "performed_work_units", info.getPerformedWorkUnits());
        }

        final Instant finish = info.getFinishTime();
        if (finish != null)
            write(writer, "finish", finish);

        write(writer, "address", info.getCurrentAddress());
        write(writer, "command", info.getCurrentCommand());

        if (info.getError().isPresent())
            write(writer, "error", info.getError().get());

        writer.writeEndElement();
    }

    public static void write(final XMLStreamWriter writer, final List<DeviceInfo> devices) throws Exception
    {
        writer.writeStartElement("devices");

        for (DeviceInfo info : devices)
        {
            writer.writeStartElement("device");
            write(writer, "name", info.getName());
            write(writer, "alias", info.getAlias());
            if (! info.getStatus().isEmpty())
                write(writer, "status", info.getStatus());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    public static void write(final XMLStreamWriter writer, final ScanData data) throws Exception
    {
        writer.writeStartElement("data");

        for (String device_name : data.getDevices())
        {
            writer.writeStartElement("device");
            {
                write(writer, "name", device_name);
                writer.writeStartElement("samples");
                for (ScanSample data_sample : data.getSamples(device_name))
                {
                    writer.writeStartElement("sample");
                    writer.writeAttribute("id", Long.toString(data_sample.getSerial()));
                    write(writer, "time", data_sample.getTimestamp());
                    write(writer, "value", ScanSampleFormatter.asString(data_sample));
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    public static void submitXML(XMLStreamWriter writer) throws Exception
    {
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }
}
