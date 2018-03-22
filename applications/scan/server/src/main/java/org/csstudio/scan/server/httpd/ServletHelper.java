/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.httpd;

import java.time.Instant;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

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

//    /** Create XML content for scan info
//     *  @param doc XML {@link Document}
//     *  @param info {@link ScanInfo}
//     *  @return XML {@link Element} for the scan info
//     */
//    public static Element createXMLElement(final Document doc, final ScanInfo info)
//    {
//        final Element scan = doc.createElement("scan");
//
//        scan.appendChild(createXMLElement(doc, "id", info.getId()));
//        scan.appendChild(createXMLElement(doc, "name", info.getName()));
//        scan.appendChild(createXMLElement(doc, "created", info.getCreated()));
//        scan.appendChild(createXMLElement(doc, "state", info.getState().name()));
//        scan.appendChild(createXMLElement(doc, "runtime", info.getRuntimeMillisecs()));
//
//        if (info.getTotalWorkUnits() > 0)
//        {
//            scan.appendChild(createXMLElement(doc, "total_work_units", info.getTotalWorkUnits()));
//            scan.appendChild(createXMLElement(doc, "performed_work_units", info.getPerformedWorkUnits()));
//        }
//
//        final Instant finish = info.getFinishTime();
//        if (finish != null)
//            scan.appendChild(createXMLElement(doc, "finish", finish));
//
//        scan.appendChild(createXMLElement(doc, "address", info.getCurrentAddress()));
//        scan.appendChild(createXMLElement(doc, "command", info.getCurrentCommand()));
//
//        if (info.getError().isPresent())
//            scan.appendChild(createXMLElement(doc, "error", info.getError().get()));
//
//        return scan;
//    }

//    /** Create XML content for scan data
//     *  @param doc XML {@link Document}
//     *  @param data {@link ScanData}
//     *  @return XML {@link Element} for the scan data
//     */
//    public static Node createXMLElement(final Document doc, final ScanData data)
//    {
//        final Element result = doc.createElement("data");
//
//        for (String device_name : data.getDevices())
//        {
//            final Element device = doc.createElement("device");
//            device.appendChild(createXMLElement(doc, "name", device_name));
//
//            final Element samples = doc.createElement("samples");
//            for (ScanSample data_sample : data.getSamples(device_name))
//            {
//                final Element sample = doc.createElement("sample");
//                sample.setAttribute("id", Long.toString(data_sample.getSerial()));
//                sample.appendChild(createXMLElement(doc, "time", data_sample.getTimestamp()));
//                sample.appendChild(createXMLElement(doc, "value", ScanSampleFormatter.asString(data_sample)));
//                samples.appendChild(sample);
//            }
//
//            device.appendChild(samples);
//            result.appendChild(device);
//        }
//        return result;
//    }

//    /** Create XML content for device infos
//     *  @param doc XML {@link Document}
//     *  @param devices {@link DeviceInfo}s
//     *  @return XML {@link Element} for the device infos
//     */
//    public static Node createXMLElement(final Document doc, final DeviceInfo... devices)
//    {
//        final Element result = doc.createElement("devices");
//
//        for (DeviceInfo info : devices)
//        {
//            final Element device = doc.createElement("device");
//            device.appendChild(createXMLElement(doc, "name", info.getName()));
//            device.appendChild(createXMLElement(doc, "alias", info.getAlias()));
//            if (! info.getStatus().isEmpty())
//                device.appendChild(createXMLElement(doc, "status", info.getStatus()));
//            result.appendChild(device);
//        }
//
//        return result;
//    }

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
        writer.writeCharacters(text);
        writer.writeEndElement();
    }

    /** Create XML element for number
     *  @param writer {@link XMLStreamWriter}
     *  @param name Name of XML element
     *  @param number Number content
     *  @return XML element
     */
    public static void write(final XMLStreamWriter writer, final String name, final long number) throws Exception
    {
        write(writer, name, Long.toString(number));
    }

    /** Create XML element for date, encoded as milliseconds since epoch
     *  @param doc Parent document
     *  @param name Name of XML element
     *  @param date Date content
     *  @return XML element
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
// TODO
//        if (info.getTotalWorkUnits() > 0)
//        {
//            scan.appendChild(createXMLElement(doc, "total_work_units", info.getTotalWorkUnits()));
//            scan.appendChild(createXMLElement(doc, "performed_work_units", info.getPerformedWorkUnits()));
//        }
//
//        final Instant finish = info.getFinishTime();
//        if (finish != null)
//            scan.appendChild(createXMLElement(doc, "finish", finish));
//
//        scan.appendChild(createXMLElement(doc, "address", info.getCurrentAddress()));
//        scan.appendChild(createXMLElement(doc, "command", info.getCurrentCommand()));
//
//        if (info.getError().isPresent())
//            scan.appendChild(createXMLElement(doc, "error", info.getError().get()));
//


        writer.writeEndElement();
    }


    public static void submitXML(XMLStreamWriter writer) throws Exception
    {
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }
}
