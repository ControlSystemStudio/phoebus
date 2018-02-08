/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.vtype.AlarmSeverity;
import org.w3c.dom.Element;

/** Archive reader for "xnds:.."
 *
 *  <p>ExampleURLs:
 *  "xnds://my.host.site/archive/ArchiveDataServer.cgi?key=1"
 *  for the data server.
 *  "xnds://my.host.site:8080/RPC2"
 *  for the simpler standalone data server.
 *
 *  <p>Compared to previous ChannelArchiverReader,
 *  the 'key' is now part of the URL.
 *  Access to different sub-archives on one server requires
 *  using one URL per sub-archive, selecting the sub-archive
 *  via the "?key=.." query.
 *  In the previous implementation, one URL was used and the
 *  key then passed to each sample read request.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLRPCArchiveReader implements ArchiveReader
{
    private final URL url;
    private final Integer key;
    private final Integer version;
    private final String description;
    private final List<String> status_strings = new ArrayList<>();
    private final Map<Integer, SeverityInfo> severities = new HashMap<>();
    private int method_raw = 0, method_optimized = 0;

    public XMLRPCArchiveReader(String url) throws Exception
    {
        // Parse key from URL
        final int key_index = url.indexOf("?key=");
        if (key_index >= 0)
        {
            key = Integer.parseInt(url.substring(key_index+5));
            url = url.substring(0, key_index);
        }
        else
            key = 1;
        this.url = new URL("http" + url.substring(4));

        final Element response = XmlRpc.communicate(this.url, XmlRpc.command("archiver.info"));
        // XMLUtil.writeDocument(response, System.out);

        // Get version information
        Element el = XmlRpc.getChildElement(response, "params");
        el = XmlRpc.getChildElement(el, "param");
        final Element struct = XmlRpc.getChildElement(el, "value");
        version = XmlRpc.getValue(XmlRpc.getStructMember(struct, "ver"));
        description = XmlRpc.getValue(XmlRpc.getStructMember(struct, "desc"));
        if (version != 1)
            logger.log(Level.WARNING,  "Expected version 1, got " + description);

        // Decode request methods
        el = XmlRpc.getStructMember(struct, "how");
        // XMLUtil.writeDocument(el, System.out);
        int method_index = 0;
        for (Element v : XmlRpc.getArrayValues(el))
        {
            final String method = XmlRpc.getValue(v);
            if (method.equals("raw"))
                method_raw = method_index;
            if (method.equals("plot-binning"))
                method_optimized = method_index;
            ++method_index;
        }
        logger.log(Level.FINE, "Request methods: raw=" + method_raw + ", optimized=" + method_optimized);

        // Decode status strings
        el = XmlRpc.getStructMember(struct, "stat");
        // XMLUtil.writeDocument(el, System.out);
        for (Element v : XmlRpc.getArrayValues(el))
        {
            final String state = XmlRpc.getValue(v);
            status_strings.add(state);
        }
        logger.log(Level.FINE, "Status strings: {0}", status_strings);

        // TODO Decode severities
        el = XmlRpc.getStructMember(struct, "sevr");
        // XMLUtil.writeDocument(el, System.out);
        for (Element v : XmlRpc.getArrayValues(el))
        {
            final Integer num = XmlRpc.getValue(XmlRpc.getStructMember(v, "num"));
            final String text = XmlRpc.getValue(XmlRpc.getStructMember(v, "sevr"));
            final Boolean has_value = XmlRpc.getValue(XmlRpc.getStructMember(v, "has_value"));
            final Boolean txt_stat = XmlRpc.getValue(XmlRpc.getStructMember(v, "txt_stat"));

            // Patch "NO ALARM" into "OK"
            final AlarmSeverity severity;
            if ("NO_ALARM".equals(text)  ||
                "OK".equals(text))
                severity = AlarmSeverity.NONE;
            else if ("MINOR".equals(text))
                severity = AlarmSeverity.MINOR;
            else if ("MAJOR".equals(text))
                severity = AlarmSeverity.MAJOR;
            else if ("MAJOR".equals(text))
                severity = AlarmSeverity.INVALID;
            else
                severity = AlarmSeverity.UNDEFINED;

            severities.put(num, new SeverityInfo(severity, text, has_value, txt_stat));
        }
        logger.log(Level.FINE, "Severities: {0}", severities);
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public List<String> getNamesByPattern(final String glob_pattern) throws Exception
    {
        final String pattern;
        if (glob_pattern.isEmpty())
            pattern = "";
        else
            pattern = RegExHelper.fullRegexFromGlob(glob_pattern);
        final Element response = XmlRpc.communicate(this.url, XmlRpc.command("archiver.names", key, pattern));
        // XMLUtil.writeDocument(response, System.out);

        final List<String> result = new ArrayList<>();
        // Decode PV names from
        // <params>
        //   <param>
        //     <value>
        //       <array>
        //         <data>
        //           <value>
        //             <struct>
        //               <member>
        //                 <name>name</name>
        //                 <value><string>BoolPV</string></value>
        //               </member>
        //             </struct>
        //           </value>
        //           <value>...
        Element el = XmlRpc.getChildElement(response, "params");
        el = XmlRpc.getChildElement(el, "param");
        el = XmlRpc.getChildElement(el, "value");
        for (Element value : XmlRpc.getArrayValues(el))
        {
            final String name = XmlRpc.getValue(XmlRpc.getStructMember(value, "name"));
            result.add(name);
        }

        return result;
    }

    @Override
    public ValueIterator getRawValues(final String name, final Instant start, final Instant end)
            throws UnknownChannelException, Exception
    {
        final int count = 10;
        final int method = method_raw;

        final String command = XmlRpc.command("archiver.values",
                                              key,
                                              List.of(name),
                                              start.getEpochSecond(),
                                              start.getNano(),
                                              end.getEpochSecond(),
                                              end.getNano(),
                                              count,
                                              method);
        final Element response = XmlRpc.communicate(this.url, command);
        XMLUtil.writeDocument(response, System.out);

        // TODO Decode samples
        return null;
    }

    @Override
    public void cancel()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void close()
    {
        // NOP
    }
}
