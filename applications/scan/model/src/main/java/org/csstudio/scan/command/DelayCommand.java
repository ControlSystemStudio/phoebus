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

import java.util.List;

import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Command that delays the scan for some time
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DelayCommand extends ScanCommand
{
    private volatile double seconds;

    /** Initialize delay with 1 second */
    public DelayCommand()
    {
        this(1.0);
    }

    /** Initialize
     *  @param seconds Delay in seconds
     */
    public DelayCommand(final double seconds)
    {
        this.seconds = seconds;
    }

    /** {@inheritDoc} */
    @Override
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(new ScanCommandProperty("seconds", "Delay (seconds)", Double.class));
        super.configureProperties(properties);
    }

    /** @return Delay in seconds */
    public double getSeconds()
    {
        return seconds;
    }

    /**@param seconds Delay in seconds */
    public void setSeconds(final Double seconds)
    {
        this.seconds = seconds;
    }

    /** {@inheritDoc} */
    @Override
    public void addXMLElements(final Document dom, final Element command_element)
    {
        Element element = dom.createElement("seconds");
        element.appendChild(dom.createTextNode(Double.toString(seconds)));
        command_element.appendChild(element);
        super.addXMLElements(dom, command_element);
    }

    /** {@inheritDoc} */
    @Override
    public void readXML(final Element element) throws Exception
    {
        setSeconds(XMLUtil.getChildDouble(element, "seconds").orElse(0.0));
        super.readXML(element);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "Delay " + seconds + " sec";
    }
}
