/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.client;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** SAX handler for XML sent by "/scan/{id}/data"
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanDataSAXHandler extends DefaultHandler
{
    /* Expected data format:
     * <data>
     *   <device>
     *     <name>readback</name>
     *     <samples>
     *       <sample serial="0">
     *         <time>2013-05-30 13:38:57.970</time>
     *         <value>0.0</value>
     *       </sample>     *
     *     </samples>
     *   </device>
     * </data>
     */
    enum State
    {
        NeedDevice,
        NeedName,
        NeedSample,
        NeedTimeAndValue
    };

    private State state = State.NeedDevice;

    /** Most recently parsed XML text data */
    private String cdata;

    /** Currently parsed device */
    private String device = null;

    /** Currently assembled sample's serial */
    private long serial = -1;

    /** Currently assembled sample's time stamp */
    private Instant time = null;

    /** Currently assembled sample's value */
    private Object value = null;

    /** Samples for the currently parsed device */
    private List<ScanSample> samples;

    private Map<String, List<ScanSample>> data = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    public void startElement(final String uri, final String localName, final String qName,
            final Attributes attributes) throws SAXException
    {
        cdata = "";
        switch (state)
        {
        case NeedDevice:
            if ("device".equalsIgnoreCase(qName))
                state = State.NeedName;
            break;
        case NeedSample:
            if ("sample".equalsIgnoreCase(qName))
            {
                state = State.NeedTimeAndValue;
                serial = Long.parseLong(attributes.getValue("id"));
                time = null;
                value = null;
            }
            break;
        default:
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endElement(final String uri, final String localName, final String qName)
            throws SAXException
    {
        switch (state)
        {
        case NeedName:
            if ("name".equalsIgnoreCase(qName))
            {
                device = cdata;
                samples = new ArrayList<>();
                data.put(device, samples);
                state = State.NeedSample;
            }
            break;
        case NeedTimeAndValue:
            if ("time".equalsIgnoreCase(qName))
            {
                try
                {
                    time = Instant.ofEpochMilli(Long.parseLong(cdata));
                }
                catch (NumberFormatException ex)
                {
                    throw new SAXException("Cannot parse time stamp for sample #" + serial + ": " + cdata);
                }
            }
            else if ("value".equalsIgnoreCase(qName))
            {
                try
                {
                    value = Double.parseDouble(cdata);
                }
                catch (NumberFormatException ex)
                {
                    value = cdata.toString();
                }
            }
            else if ("sample".equalsIgnoreCase(qName))
            {
                if (time == null  ||  value == null)
                    throw new SAXException("Missing time or value for sample ");
                final ScanSample sample;
                if (value instanceof Number)
                    sample = ScanSampleFactory.createSample(time, serial, (Number)value);
                else
                    sample = ScanSampleFactory.createSample(time, serial, (String)value);
                samples.add(sample);

                state = State.NeedSample;
            }
            break;
        case NeedSample:
            if ("samples".equalsIgnoreCase(qName))
                state = State.NeedDevice;
            break;
        default:
        }
    }

    /** {@inheritDoc} */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException
    {
        cdata = new String(ch, start, length);
    }

    /** @return {@link ScanData} parsed from XML */
    public ScanData getScanData()
    {
        return new ScanData(data);
    }
}
