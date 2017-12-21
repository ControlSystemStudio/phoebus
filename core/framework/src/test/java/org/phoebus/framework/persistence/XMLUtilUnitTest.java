/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.persistence;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.junit.Test;
import org.w3c.dom.Element;

/** JUnit test of the {@link XMLUtil}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLUtilUnitTest
{
    /** Opening an XML doc
     *  @throws Exception on error
     */
    @Test
    public void testOpenDoc() throws Exception
    {
        XMLUtil.openXMLDocument(getClass().getResourceAsStream("xml_util_example.xml"), "display");

        try
        {
            XMLUtil.openXMLDocument(getClass().getResourceAsStream("xml_util_example.xml"), "no_display");
            fail("Read wrong XML root");
        }
        catch (final Exception ex)
        {
            System.out.println("Detected: " + ex.getMessage());
            assertThat(ex.getMessage(), containsString("<no_display>"));
        }
    }

    /** Iterate over elements
     *  @throws Exception on error
     */
    @Test
    public void testIterators() throws Exception
    {
        final Element root = XMLUtil.openXMLDocument(getClass().getResourceAsStream("xml_util_example.xml"), "display");

        int count = 0;
        for (final Element widget : XMLUtil.getChildElements(root, "widget"))
        {
            System.out.println("Found widget with version " + widget.getAttribute("version"));
            ++count;
        }
        System.out.println("Found " + count + " widgets");
        assertThat(count, equalTo(2));
    }

    /** Locate elements by name
     *  @throws Exception on error
     */
    @Test
    public void testFindingElements() throws Exception
    {
        final Element root = XMLUtil.openXMLDocument(getClass().getResourceAsStream("xml_util_example.xml"), "display");

        final Element widget = XMLUtil.getChildElement(root, "widget");
        assertThat(widget, not(nullValue()));
        final Element prop = XMLUtil.getChildElement(widget, "name");
        assertThat(prop, not(nullValue()));
        final String name = XMLUtil.getString(prop);
        System.out.println("Found widget with name " + name);

        Optional<String> value = XMLUtil.getChildString(widget, "name");
        System.out.println("Name: " + value.orElse(" -- "));
        assertThat(value.isPresent(), equalTo(true));

        value = XMLUtil.getChildString(widget, "date");
        System.out.println("Date: " + value.orElse(" -- "));
        assertThat(value.isPresent(), equalTo(false));
    }

    /** Locate elements by name
     *  @throws Exception on error
     */
    @Test
    public void testLineNumber() throws Exception
    {
        final Element root = XMLUtil.openXMLDocument(getClass().getResourceAsStream("xml_util_example.xml"), "display");

        final Element widget = XMLUtil.getChildElement(root, "widget");
        final Element prop = XMLUtil.getChildElement(widget, "name");
        final Optional<Integer> line = XMLUtil.getLineNumber(prop);
        System.out.println("Found widget name in line " + line.orElse(-1));
        assertThat(line.orElse(-1), equalTo(4));
    }
}
