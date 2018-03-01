/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
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

/** Command that adds comment to a scan
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommentCommand extends ScanCommand
{
    private volatile String comment;

    /** Initialize with example comment */
    public CommentCommand()
    {
        this("This is a comment");
    }

    /** Initialize
     *  @param comment Comment
     */
    public CommentCommand(final String comment)
    {
        this.comment = comment;
    }

    /** {@inheritDoc} */
    @Override
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(new ScanCommandProperty("comment", "Comment", String.class));
        // NOT calling super.configureProperties(properties);
    }

    /** @return Comment */
    public String getComment()
    {
        return comment;
    }

    /** @param comment Desired comment */
    public void setComment(final String comment)
    {
        this.comment = comment;
    }

    /** {@inheritDoc} */
    @Override
    public void addXMLElements(final Document dom, final Element command_element)
    {
        Element element = dom.createElement("text");
        element.appendChild(dom.createTextNode(comment));
        command_element.appendChild(element);
        // NOT calling super.addXMLElements(dom, command_element);
    }

    /** {@inheritDoc} */
    @Override
    public void readXML(final Element element) throws Exception
    {
        setComment(XMLUtil.getChildString(element, "text").orElse(""));
        // NOT calling super.readXML(factory, element);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        if (comment == null  ||  comment.isEmpty())
            return "-- Empty Comment --";
        return comment;
    }
}
