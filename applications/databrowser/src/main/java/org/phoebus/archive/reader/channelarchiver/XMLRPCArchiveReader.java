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
import java.util.List;
import java.util.logging.Level;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;
import org.w3c.dom.Element;

@SuppressWarnings("nls")
public class XMLRPCArchiveReader implements ArchiveReader
{
    private final URL url;
    private final Integer key;
    private final Integer version;
    private final String description;
    private final List<String> status_strings = new ArrayList<>();

    public XMLRPCArchiveReader(final String url) throws Exception
    {
        // TODO Parse key from URL
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

        // Decode status strings
        el = XmlRpc.getStructMember(struct, "stat");
        // XMLUtil.writeDocument(el, System.out);
        for (Element v : XmlRpc.getArrayValues(el))
        {
            final String state = XmlRpc.getValue(v);
            status_strings.add(state);
        }
        logger.log(Level.INFO, "Status strings: {0}", status_strings);

        // TODO Decode severities
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
    public ValueIterator getRawValues(String name, Instant start, Instant end)
            throws UnknownChannelException, Exception
    {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub

    }
}
