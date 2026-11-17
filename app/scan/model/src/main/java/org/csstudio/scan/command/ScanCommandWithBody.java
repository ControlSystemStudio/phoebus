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

import java.util.ArrayList;
import java.util.List;

import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Command with 'body' of commands
 *
 *  <p>Base class for commands like the {@link LoopCommand}
 *  which contain a body of commands
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class ScanCommandWithBody extends ScanCommand
{
    private List<ScanCommand> body;

    /** Initialize
     *  @param body Optional body commands
     */
    public ScanCommandWithBody(final List<ScanCommand> body)
    {
        // Body must be a mutable array list
        if (body instanceof ArrayList)
            this.body = body;
        else
            this.body = new ArrayList<>(body);
    }

    /** @param commands Array of commands
     *  @return Mutable(!) list of commands
     */
    protected static List<ScanCommand> toList(final ScanCommand[] commands)
    {
        final List<ScanCommand> list = new ArrayList<ScanCommand>(commands.length);
        for (ScanCommand command : commands)
            list.add(command);
        return list;
    }

    /** Set address of this command as well as commands in body
     *  {@inheritDoc}
     */
    @Override
    public long setAddress(final long address)
    {
        long next = super.setAddress(address);
        for (ScanCommand command : body)
            next = command.setAddress(next);
        return next;
    }

    /** @return Descriptions for body of commands */
    public List<ScanCommand> getBody()
    {
        return body;
    }

    /** @param body Body commands */
    public void setBody(final List<ScanCommand> body)
    {
        this.body = body;
    }

    /** {@inheritDoc} */
    @Override
    public void addXMLElements(final Document dom, final Element command_element)
    {
        final Element element = dom.createElement("body");
        for (ScanCommand cmd : body)
            cmd.writeXML(dom, element);
        command_element.appendChild(element);

        super.addXMLElements(dom, command_element);
    }

    /** {@inheritDoc} */
    @Override
    public void readXML(final Element element) throws Exception
    {
        final Element body_node = XMLUtil.getChildElement(element, "body");
        if (body_node != null)
        {
            final List<ScanCommand> body = ScanCommandFactory.readCommands(body_node.getFirstChild());
            setBody(body);
        }
        super.readXML(element);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return getCommandName();
    }
}
