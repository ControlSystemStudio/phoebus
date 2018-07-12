/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
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

import java.util.Collections;
import java.util.List;

import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Command that performs command it its body in parallel
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ParallelCommand extends ScanCommandWithBody
{
    private volatile double timeout = 0.0;

    /** Initialize with empty body */
    public ParallelCommand()
    {
        this(Collections.emptyList());
    }

    /** Initialize
     *  @param body Body commands
     */
    public ParallelCommand(final ScanCommand... body)
    {
        super(toList(body));
    }


    /** Initialize
     *  @param body Body commands
     */
    public ParallelCommand(final List<ScanCommand> body)
    {
        super(body);
    }

    /** {@inheritDoc} */
    @Override
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(ScanCommandProperty.TIMEOUT);
        super.configureProperties(properties);
    }

    /** @return Timeout in seconds */
    public double getTimeout()
    {
        return timeout;
    }

    /** @param timeout Time out in seconds */
    public void setTimeout(final Double timeout)
    {
        this.timeout = Math.max(0.0, timeout);
    }

    /** {@inheritDoc} */
    @Override
    public void addXMLElements(final Document dom, final Element command_element)
    {
        if (timeout > 0.0)
        {
            final Element element = dom.createElement("timeout");
            element.appendChild(dom.createTextNode(Double.toString(timeout)));
            command_element.appendChild(element);
        }
        super.addXMLElements(dom, command_element);
    }

    /** {@inheritDoc} */
    @Override
    public void readXML(final Element element) throws Exception
    {
        // Read body first, so we don't update other params if this fails
        super.readXML(element);
        setTimeout(XMLUtil.getChildDouble(element, ScanCommandProperty.TAG_TIMEOUT).orElse(0.0));
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Parallel");
        if (timeout > 0)
            buf.append(", ").append(timeout).append(" sec timeout");
        return buf.toString();
    }
}
