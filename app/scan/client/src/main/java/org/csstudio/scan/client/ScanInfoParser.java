/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.client;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.csstudio.scan.info.Scan;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** SAX parser for '&lt;scans&gt;'
 *
 *  <p>Faster than DOM parser for list of scan infos.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ScanInfoParser
{
    /* Expected data format:
     * <scans>
     *   <scan>
     *     <id>1436</id>
     *     <name>/home/controls/files/2020B/IPTS-25268_Ziling/Nov20th_Ni(bpy)(ox)_MelaniePleaseSubmitThis.gnumeric</name>
     *     <created>1605896078818</created>
     *     <state>Running</state>
     *     <runtime>612832</runtime>
     *     <total_work_units>313</total_work_units>
     *     <performed_work_units>14</performed_work_units>
     *     <finish>1605909752666</finish>
     *     <address>14</address>
     *     <command>Delay 600.0 sec. Remaining: 00:00:56</command>
     */
    private enum State
    {
        NeedScans,
        NeedScan,
        InScan
    };

    private final List<ScanInfo> infos = new ArrayList<>();

    private class ScanInfoSAXHandler extends DefaultHandler
    {
        private State parse_state = State.NeedScans;

        /** Most recently parsed XML text data */
        private StringBuilder cdata = new StringBuilder();

        private int id;
        private String name;
        private Instant created;
        private ScanState state;
        private Optional<String> error;
        private long runtime_ms;
        private long total_work_units;
        private long performed_work_units;
        private long finishtime_ms;
        private long current_address;
        private String current_commmand;

        @Override
        public void startElement(final String uri, final String localName, final String qName,
                final Attributes attributes) throws SAXException
        {
            cdata.setLength(0);

            switch (parse_state)
            {
            case NeedScans:
                if ("scans".equals(qName))
                    parse_state = State.NeedScan;
                break;
            case NeedScan:
                if ("scan".equals(qName))
                {
                    // Reset details of a scan
                    id = -1;
                    name = "";
                    created = Instant.ofEpochMilli(0l);
                    state = ScanState.Logged;
                    error = Optional.empty();
                    runtime_ms = 0l;
                    total_work_units = 0l;
                    performed_work_units = 0l;
                    finishtime_ms = 0l;
                    current_address = 0l;
                    current_commmand = "";

                    parse_state = State.InScan;
                }
                break;
            default:
            }
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName)
                throws SAXException
        {
            if (parse_state != State.InScan)
                return;

            // Handle all the elements of a scan
            if ("id".equals(qName))
                id = Integer.parseInt(cdata.toString());
            else if ("name".equals(qName))
                name = cdata.toString();
            else if ("created".equals(qName))
                created = Instant.ofEpochMilli(Long.parseLong(cdata.toString()));
            else if ("state".equals(qName))
                state = ScanState.valueOf(cdata.toString());
            else if ("error".equals(qName))
                error = Optional.of(cdata.toString());
            else if ("runtime".equals(qName))
                runtime_ms = Long.parseLong(cdata.toString());
            else if ("total_work_units".equals(qName))
                total_work_units = Long.parseLong(cdata.toString());
            else if ("performed_work_units".equals(qName))
                performed_work_units = Long.parseLong(cdata.toString());
            else if ("finish".equals(qName))
                finishtime_ms = Long.parseLong(cdata.toString());
            else if ("address".equals(qName))
                current_address = Long.parseLong(cdata.toString());
            else if ("command".equals(qName))
                current_commmand = cdata.toString();

            // Wrap up collected info for this scan
            else if ("scan".equals(qName))
            {
                final Scan scan = new Scan(id, name, created);
                final ScanInfo info =  new ScanInfo(scan, state, error, runtime_ms, finishtime_ms,
                        performed_work_units, total_work_units, current_address, current_commmand);
                infos.add(info);

                // Ready for new one
                parse_state = State.NeedScan;
            }
        }

        @Override
        public void characters(final char[] ch, final int start, final int length)
                throws SAXException
        {
            cdata.append(ch, start, length);
        }
    }

    public ScanInfoParser(final InputStream stream) throws Exception
    {
        final ScanInfoSAXHandler handler = new ScanInfoSAXHandler();
        final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(stream, handler);
    }

    public List<ScanInfo> getScanInfos()
    {
        return infos;
    }
}
