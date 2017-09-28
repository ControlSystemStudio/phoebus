/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

/** JUnit test of {@link XMLMementoTree}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLMementoTest
{
    @Test
    public void testData() throws Exception
    {
        final MementoTree mt = XMLMementoTree.create();

        mt.setString("text", "Some text\nAnother line");
        mt.setNumber("number", 42);
        mt.setNumber("float", 3.14);
        mt.setNumber("huge", Integer.MAX_VALUE + 2L);
        mt.setBoolean("flag", true);

        System.out.println(mt);

        assertThat(mt.getString("text").get(), equalTo("Some text\nAnother line"));
        assertThat(mt.getNumber("number").get(), equalTo(42));
        assertThat(mt.getNumber("float").get(), equalTo(3.14));
        assertThat(mt.getNumber("huge").get(), equalTo(Integer.MAX_VALUE + 2L));
        assertThat(mt.getBoolean("flag").get(), equalTo(true));
        assertThat(mt.getString("bogus"), equalTo(Optional.empty()));
    }

    @Test
    public void testHierarchy() throws Exception
    {
        final MementoTree mt = XMLMementoTree.create();

        MementoTree sub = mt.getChild("sub1");
        sub.setNumber("x", 10);

        sub = mt.getChild("sub2");
        sub.setNumber("y", 20);

        sub = sub.getChild("sub2_1");
        sub.setNumber("z", 30);

        System.out.println(mt);

        final List<MementoTree> subs = mt.getChildren();
        assertThat(subs.size(), equalTo(2));

        assertThat(mt.getChild("sub1").getChildren().size(), equalTo(0));
        assertThat(mt.getChild("sub2").getChildren().size(), equalTo(1));
        assertThat(mt.getChild("sub2").getChild("sub2_1").getChildren().size(), equalTo(0));
    }

    @Test
    public void testWriteandRead() throws Exception
    {
        final XMLMementoTree mt = XMLMementoTree.create();

        MementoTree sub = mt.getChild("sub1");
        sub.setNumber("x", 10);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        mt.write(out);
        System.out.println(out.toString());

        final XMLMementoTree readback = XMLMementoTree.read(new ByteArrayInputStream(out.toByteArray()));
        // For details not quite clear, the readback contains added lines with just spaces.
        // Doesn't affect the actual data in the memento,
        // but remove them to get exact match with original XML text.
        final String readback_text = readback.toString().replaceAll(" +"+System.getProperty("line.separator"), "");
        System.out.println(readback_text);

        assertThat(readback_text, equalTo(mt.toString()));
    }
}
