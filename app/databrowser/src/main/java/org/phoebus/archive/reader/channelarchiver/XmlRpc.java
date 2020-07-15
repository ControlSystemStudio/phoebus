/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** XML-RPC client
 *
 *  <p>Just the code needed to communicate with the archive data server.
 *  Avoids a third party library like Apache xmlrpc
 *  which needs patches to handle Double.NaN.
 *
 *  <p>Implemented with basic {@link URLConnection}.
 *  Could be updated to HttpClient once that moves beyond jdk.incubator.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XmlRpc
{
    /** Create XML-RPC request
     *  @param method
     *  @param params
     *  @return XML for the request
     *  @throws Exception on error
     */
    public static String command(final String method, final Object... params) throws Exception
    {
        // In principle, the <string>.. values should be URLEncoder.encode()d,
        // but archive data server won't handle encoded PV name...
        final StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"").append(XMLUtil.ENCODING).append("\"?>\n");
        buf.append("<methodCall>\n");
        buf.append(" <methodName>").append(method).append("</methodName>\n");
        if (params.length > 0)
        {
            buf.append(" <params>\n");
            for (Object param : params)
            {
                buf.append("  <param><value>");
                if (param instanceof Integer)
                    buf.append("<i4>").append(param).append("</i4>");
                // TODO Should long use <i8>? Legacy used <i4>...
                else if (param instanceof Long)
                    buf.append("<i4>").append(param).append("</i4>");
                else if (param instanceof String)
                    buf.append("<string>").append(param).append("</string>");
                else if (param instanceof List)
                {
                    buf.append("<array>");
                    buf.append("<data>");
                    for (Object value : (List<?>)param)
                    {
                        if (value instanceof String)
                            buf.append("<value><string>")
                               .append(value)
                               .append("</string></value>");
                        else
                            throw new Exception("Cannot handle array element of type " + value.getClass().getName());
                    }
                    buf.append("</data>");
                    buf.append("</array>");
                }
                else
                    throw new Exception("Cannot handle parameter of type " + param.getClass().getName());
                buf.append("</value></param>\n");
            }
            buf.append(" </params>\n");
        }
        buf.append("</methodCall>\n");
        return buf.toString();
    }

    /** Send XML-RPC command, retrieve response
     *
     *  @param url Server URL
     *  @param command Command to send
     *  @return "value" from the reply
     *  @throws Exception on error
     */
    public static Element communicate(final URL url, final String command) throws Exception
    {
        final URLConnection connection = url.openConnection();
        if (! (connection instanceof HttpURLConnection))
            throw new Exception("Cannot create HttpURLConnection for " + url);
        final HttpURLConnection http = (HttpURLConnection) connection;

        final byte[] bytes = command.getBytes(StandardCharsets.UTF_8);

        // Send the request
        http.setDoOutput(true);
        http.setRequestMethod("POST");
        http.setInstanceFollowRedirects(true);
        // XmlRPC server expects "text/xml" and will report HTTP 400 otherwise
        http.setRequestProperty("Content-Type", "text/xml");
        http.setRequestProperty("charset", "UTF-8");
        http.setRequestProperty("Accept", "text/html, application/xml, *");
        http.setFixedLengthStreamingMode(bytes.length);

        try (OutputStream out = connection.getOutputStream())
        {
            out.write(bytes);
            out.flush();
        }
        
        // Parse the response, expecting <methodResponse><params><param><value>
        Element el = XMLUtil.openXMLDocument(connection.getInputStream(), "methodResponse");
        // Check for fault
        Element fault = XMLUtil.getChildElement(el, "fault");
        if (fault != null)
            throw new Exception(XMLUtil.elementToString(fault, false));

        el = getChildElement(el, "params");
        el = getChildElement(el, "param");
        el = getChildElement(el, "value");
       return el;
    }

    /** Get expected XML child element
     *  @param parent XML parent
     *  @param name Child element name
     *  @return Child element
     *  @throws Exception if not found
     */
    public static Element getChildElement(final Element parent, final String name) throws Exception
    {
        final Element result = XMLUtil.getChildElement(parent, name);
        if (result == null)
            throw new Exception("Expected XML element <" + name + ">");
        return result;
    }

    /** @param value A "value" node that contains a "struct"
     *  @param name Name of desired structure member
     *  @return "value" node of that member, or <code>null</code> if not found
     *  @throws Exception on error
     */
    public static Element getOptionalStructMember(final Element value, final String name) throws Exception
    {
        final Element struct = XMLUtil.getChildElement(value,  "struct");
        for (Element member : XMLUtil.getChildElements(struct, "member"))
        {
            if (name.equals(XMLUtil.getChildString(member, "name").orElse(null)))
                return getChildElement(member, "value");
        }
        return null;
    }

    /** @param value A "value" node that contains a "struct"
     *  @param name Name of desired structure member
     *  @return "value" node of that member
     *  @throws Exception on error
     */
    public static Element getStructMember(final Element value, final String name) throws Exception
    {
        final Element result = getOptionalStructMember(value, name);
        if (result == null)
            throw new Exception("Cannot locate struct element <" + name + ">");
        return result;
    }

    /** Iterate over all "value" elements of an "array"
     *  @param value A "value" node that contains an "array"
     *  @return Iterator for the "value" nodes within the array
     *  @throws Exception on error
     */
    public static Iterable<Element> getArrayValues(final Element value) throws Exception
    {
        Element el = getChildElement(value, "array");
        el = getChildElement(el, "data");
        return XMLUtil.getChildElements(el, "value");
    }

    /** Get first "value" element of an "array"
     *  @param value A "value" node that contains an "array"
     *  @return First "value" nodes within the array
     *  @throws Exception on error
     */
    public static Element getFirstArrayValue(final Element value) throws Exception
    {
        Element el = getChildElement(value, "array");
        el = getChildElement(el, "data");
        return XMLUtil.getChildElement(el, "value");
    }


    /** Decode an XML-RPC "value"
     *  @param value A "value" node that contains "string", "i4"
     *  @return {@link String}, {@link Integer}
     *  @throws Exception on error
     */
    @SuppressWarnings("unchecked")
    public static <TYPE> TYPE getValue(final Element value) throws Exception
    {
        final Element content = (Element) value.getFirstChild();
        final String type = content.getNodeName();
        final String text = XMLUtil.getString(content);
        if ("string".equals(type))
            return (TYPE) text;
        else if ("double".equals(type))
        {
            if (text.equalsIgnoreCase("nan"))
                return (TYPE) Double.valueOf("NaN");
            return (TYPE) Double.valueOf(text);
        }
        else if ("i4".equals(type))
            return (TYPE) Integer.valueOf(text);
        else if ("boolean".equals(type))
            return (TYPE) Boolean.valueOf(text.equals("1"));
        else
            throw new Exception("Cannot decode type " + type);
    }
}
