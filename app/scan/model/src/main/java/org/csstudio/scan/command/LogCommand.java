/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
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
package org.csstudio.scan.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.csstudio.scan.device.DeviceInfo;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Command that reads data from devices and logs it
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LogCommand extends ScanCommand
{
    private volatile String[] device_names;

    /** Initialize empty log command */
    public LogCommand()
    {
        this("device");
    }

    /** Initialize
     *  @param device_names List of device names
     */
    public LogCommand(final String... device_names)
    {
        this.device_names = device_names;
    }

    /** Initialize
     *  @param device_name Single device name
     */
    public LogCommand(final String device_name)
    {
        this(new String[] { device_name });
    }

    /** {@inheritDoc} */
    @Override
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(
            new ScanCommandProperty("device_names", "Device Names", DeviceInfo[].class));
        super.configureProperties(properties);
    }

    /** @return Names of devices to read and log */
    public String[] getDeviceNames()
    {
        return Arrays.copyOf(device_names, device_names.length);
    }

    /** @param device_names Names of devices to read and log */
    public void setDeviceNames(final String... device_names)
    {
        this.device_names = device_names;
    }

    /** {@inheritDoc} */
    @Override
    public void addXMLElements(final Document dom, final Element command_element)
    {
        final Element devices_element = dom.createElement("devices");
        for (String device : device_names)
        {
            final Element element = dom.createElement("device");
            element.appendChild(dom.createTextNode(device));
            devices_element.appendChild(element);
        }
        command_element.appendChild(devices_element);
        super.addXMLElements(dom, command_element);
    }

    /** {@inheritDoc} */
    @Override
    public void readXML(final Element element) throws Exception
    {
        final List<String> devices = new ArrayList<String>();
        Element node = XMLUtil.getChildElement(element, "devices");
        if (node == null)
            throw new Exception("Missing 'devices'");
        for (Element dev_node : XMLUtil.getChildElements(node,  ScanCommandProperty.TAG_DEVICE))
        {
            final Node text_node = dev_node.getFirstChild();
            if (text_node == null)
                throw new Exception("Missing device name");
            devices.add(text_node.getNodeValue());
        }
        setDeviceNames(devices.toArray(new String[devices.size()]));
        super.readXML(element);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Log ");
        for (int i=0; i<device_names.length; ++i)
        {
            final String device_name = device_names[i];
            if (i > 0)
                buf.append(", ");
            buf.append("'" + device_name + "'");
        }
        return buf.toString();
    }
}
