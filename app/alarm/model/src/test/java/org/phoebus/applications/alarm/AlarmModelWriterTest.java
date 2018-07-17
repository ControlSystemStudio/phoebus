/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.model.xml.XmlModelWriter;

/** JUnit test of {@link XmlModelWriter}
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmModelWriterTest
{
	private void assertFrequency(final int expected, final String searchStr, final String xml)
	{
		int found = 0;
        int index_start = 0;
        while (true)
        {
        	final int tmp = xml.indexOf(searchStr, index_start + 1);
        	if (tmp == -1)
        		break;
    		index_start = tmp;
    		found++;
        }

        assertEquals(expected, found);
	}

	@Test
	public void testAlarmModelWriter() throws Exception
	{
        final AlarmClientNode  root = new AlarmClientNode(null, "Test");

		// Create an area with 2 PV's.
		final AlarmClientNode area1 = new AlarmClientNode(root, "Area1");

		final List<TitleDetail> area1Guidance = new ArrayList<>();

		area1Guidance.add(new TitleDetail("Area1 Guidance Title 1", "Area1 Guidance Detail 1"));
		area1Guidance.add(new TitleDetail("Area1 Guidance Title 2", "Area1 Guidance Detail 2"));

		// Set area1 commands.
		area1.setCommands(area1Guidance);

		final List<TitleDetail> area1Displays = new ArrayList<>();

		area1Displays.add(new TitleDetail("Area1 Display Title 1", "Area1 Display Detail 1"));
		area1Displays.add(new TitleDetail("Area1 Display Title 2", "Area1 Display Detail 2"));

		// Set area1 displays.
		area1.setDisplays(area1Displays);


		// Set area1 commands.
		area1.setCommands(List.of(
				new TitleDetail("Area1 Command Title 1", "Area1 Command Detail 1"),
				new TitleDetail("Area1 Command Title 2", "Area1 Command Detail 2")));

		final List<TitleDetailDelay> area1Actions = new ArrayList<>();

		area1Actions.add(new TitleDetailDelay("Area1 Action Title 1", "Area1 Action Detail 1", 4));
		area1Actions.add(new TitleDetailDelay("Area1 Action Title 2", "Area1 Action Detail 2", 5));

		// Set area1 commands.
		area1.setActions(area1Actions);

		final AlarmClientLeaf a1pv1 = new AlarmClientLeaf(area1, "a1pv1");

		a1pv1.setAnnunciating(true);
		a1pv1.setCount(5);
		a1pv1.setDelay(4);
		a1pv1.setDescription("a1pv1 description");
		a1pv1.setEnabled(true);
		a1pv1.setFilter("a1pv1 filter");
		a1pv1.setLatching(true);

		final AlarmClientLeaf a1pv2 = new AlarmClientLeaf(area1, "a1pv2");
		a1pv2.setDescription("a1pv2 description");

		final AlarmClientNode area2 = new AlarmClientNode(root, "Area2");

		final AlarmClientLeaf a2pv1 = new AlarmClientLeaf(area2, "a2pv1");
		a2pv1.setDescription("a2pv1 description");

		final AlarmClientNode area3 = new AlarmClientNode(area2, "Area3");

		final AlarmClientLeaf a3pv1 = new AlarmClientLeaf(area3, "a3pv1");
		a3pv1.setDescription("a3pv1 description");

		final ByteArrayOutputStream buf = new ByteArrayOutputStream();

		final XmlModelWriter xmlWriter = new XmlModelWriter(buf);
		xmlWriter.write(root);
		xmlWriter.close();

        final String xml = buf.toString();

        System.out.println(xml);

        // For asserts that look for non unique substrings, implement a check that counts the frequency of the substring
        // and tests against the susbtring's true total frequency?

        // Check for config
        assertTrue(xml.contains("<config name=\"Test\">"));
        assertTrue(xml.contains("</config>"));

        // Check for Area1 and its contents.
        assertTrue(xml.contains("<component name=\"Area1\">"));
        assertTrue(xml.contains("<title>Area1 Command Title 1</title>"));
        assertTrue(xml.contains("<details>Area1 Command Detail 1</details>"));
        assertTrue(xml.contains("<title>Area1 Command Title 2</title>"));
        assertTrue(xml.contains("<details>Area1 Command Detail 2</details>"));
        assertTrue(xml.contains("<title>Area1 Action Title 1</title>"));
        assertTrue(xml.contains("<details>Area1 Action Detail 1</details>"));
        assertTrue(xml.contains("<delay>4</delay>"));
        assertTrue(xml.contains("<title>Area1 Action Title 2</title>"));
        assertTrue(xml.contains("<details>Area1 Action Detail 2</details>"));
        assertTrue(xml.contains("<delay>5</delay>"));
        // Area1 PV1
        assertTrue(xml.contains("<pv name=\"a1pv1\">"));
        assertTrue(xml.contains("<description>a1pv1 description</description>"));

        assertTrue(xml.contains("<delay>4</delay>"));
        assertTrue(xml.contains("<count>5</count>"));
        assertTrue(xml.contains("<filter>a1pv1 filter</filter>"));

        //Area1 PV2
        assertTrue(xml.contains("<pv name=\"a1pv2\">"));
        assertTrue(xml.contains("<description>a1pv2 description</description>"));

        //Check for Area2 and its contents.
        assertTrue(xml.contains("<component name=\"Area2\">"));
        assertTrue(xml.contains("<pv name=\"a2pv1\">"));

        //Check for Area3 and its contents.
        assertTrue(xml.contains("<component name=\"Area3\">"));
        assertTrue(xml.contains("<pv name=\"a3pv1\">"));

        // Check for total frequencies of non unique strings.
        assertFrequency(4, "<enabled>true</enabled>", xml);
        assertFrequency(4, "<latching>true</latching>", xml);
        assertFrequency(4, "<annunciating>true</annunciating>", xml);
	}
}
