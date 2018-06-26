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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.model.xml.XmlModelReader;

/** JUnit test of {@link XmlModelReader}
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmModelReaderTest
{
	// XML Alarm Test Model
    public static final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
	+ "<config name=\"Test\">\n"
	+ "  <component name=\"Area1\">\n"
	+ "    <automated_action>\n"
	+ "      <title>Area1 Action Title 1</title>\n"
	+ "      <details>Area1 Action Detail 1</details>\n"
	+ "      <delay>5</delay>\n"
	+ "    </automated_action>\n"
	+ "    <automated_action>\n"
    + "      <title>Area1 Action Title 2</title>\n"
    + "      <details>Area1 Action Detail 2</details>\n"
    + "      <delay>6</delay>\n"
    + "    </automated_action>\n"
	+ "    <command>\n"
	+ "      <title>Area1 Command Title 1</title>\n"
	+ "      <details>Area1 Command Detail 1</details>\n"
	+ "    </command>\n"
	+ "    <command>\n"
	+ "      <title>Area1 Command Title 2</title>\n"
	+ "      <details>Area1 Command Detail 2</details>\n"
	+ "    </command>\n"
	+ "    <pv name=\"a1pv1\">\n"
	+ "       <description>a1pv1 description</description>\n"
	+ "       <enabled>true</enabled>\n"
	+ "       <latching>true</latching>\n"
	+ "       <annunciating>true</annunciating>\n"
	+ "       <delay>4</delay>\n"
	+ "       <count>5</count>\n"
	+ "       <filter>a1pv1 filter</filter>\n"
	+ "       <command>\n"
	+ "         <title>a1pv1 Command Title 1</title>\n"
	+ "         <details>a1pv1 Command Detail 1</details>\n"
	+ "       </command>\n"
	+ "    </pv>"
	+ "    <pv name=\"a1pv2\">\n"
	+ "      <description>a1pv2 description</description>\n"
	+ "      <enabled>true</enabled>\n"
	+ "      <latching>true</latching>\n"
	+ "      <annunciating>true</annunciating>\n"
	+ "    </pv>\n"
	+ "  </component>\n"
	+ "  <component name=\"Area2\">\n"
	+ "    <component name=\"Area3\">\n"
	+ "      <pv name=\"a3pv1\">\n"
	+ "        <description>a3pv1 description</description>\n"
	+ "        <enabled>true</enabled>\n"
	+ "        <latching>true</latching>\n"
	+ "        <annunciating>true</annunciating>\n"
	+ "      </pv>\n"
	+ "    </component>\n"
	+ "    <pv name=\"a2pv1\">\n"
	+ "      <description>a2pv1 description</description>\n"
	+ "      <enabled>true</enabled>\n"
	+ "      <latching>true</latching>\n"
	+ "      <annunciating>true</annunciating>\n"
	+ "    </pv>\n"
	+ "  </component>\n"
	+ "</config>\n";


	@Test
	public void testAlarmModelReader() throws Exception
	{
		final XmlModelReader reader = new XmlModelReader();
		final InputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));

		reader.load(input);

		final AlarmClientNode root = reader.getRoot();

		assertEquals(2, root.getChildren().size());

		final AlarmTreeItem<?> area1 = root.getChild("Area1");
		final AlarmTreeItem<?> area2 = root.getChild("Area2");

		assertEquals("Area1", area1.getName());

		final List<TitleDetail> a1_commands = area1.getCommands();

		assertEquals(2, a1_commands.size());

		final TitleDetail a1_command1 = a1_commands.get(0);

		assertEquals("Area1 Command Title 1", a1_command1.title);
		assertEquals("Area1 Command Detail 1", a1_command1.detail);

		final TitleDetail a1_command2 = a1_commands.get(1);

		assertEquals("Area1 Command Title 2", a1_command2.title);
		assertEquals("Area1 Command Detail 2", a1_command2.detail);

		final List<TitleDetailDelay> a1_actions = area1.getActions();

		assertEquals(2, a1_actions.size());

		final TitleDetailDelay a1_action1 = a1_actions.get(0);
		assertEquals("Area1 Action Title 1", a1_action1.title);
		assertEquals("Area1 Action Detail 1", a1_action1.detail);
		assertEquals(5, a1_action1.delay);

		final TitleDetailDelay a1_action2 = a1_actions.get(1);
        assertEquals("Area1 Action Title 2", a1_action2.title);
        assertEquals("Area1 Action Detail 2", a1_action2.detail);
        assertEquals(6, a1_action2.delay);

		final AlarmTreeLeaf a1pv1 = (AlarmTreeLeaf) area1.getChild("a1pv1");

		assertEquals("a1pv1", area1.getChild("a1pv1").getName());
		assertEquals("a1pv1 description", a1pv1.getDescription());
		assertTrue(a1pv1.isEnabled());
		assertTrue(a1pv1.isLatching());
		assertTrue(a1pv1.isAnnunciating());
		assertEquals(5, a1pv1.getCount());
		assertEquals(4, a1pv1.getDelay());
		assertEquals("a1pv1 filter", a1pv1.getFilter());

		final List<TitleDetail> a1pv1_commands = ((AlarmTreeItem<?>)a1pv1).getCommands();

		assertEquals(1, a1pv1_commands.size());

		assertEquals("a1pv1 Command Title 1", a1pv1_commands.get(0).title);
		assertEquals("a1pv1 Command Detail 1", a1pv1_commands.get(0).detail);

		final AlarmTreeLeaf a1pv2 = (AlarmTreeLeaf) area1.getChild("a1pv2");

		assertEquals("a1pv2", area1.getChild("a1pv2").getName());
		assertEquals("a1pv2 description", a1pv2.getDescription());
		assertTrue(a1pv2.isEnabled());
		assertTrue(a1pv2.isLatching());
		assertTrue(a1pv2.isAnnunciating());

		assertEquals("Area2", area2.getName());

		final AlarmTreeItem<?> area3 = area2.getChild("Area3");

		assertEquals("Area3", area3.getName());

		final AlarmTreeLeaf a3pv1 = (AlarmTreeLeaf) area3.getChild("a3pv1");

		assertEquals("a3pv1", ((AlarmTreeItem<?>) a3pv1).getName());
		assertEquals("a3pv1 description", a3pv1.getDescription());
		assertTrue(a3pv1.isEnabled());
		assertTrue(a3pv1.isLatching());
		assertTrue(a3pv1.isAnnunciating());

		final AlarmTreeLeaf a2pv1 = (AlarmTreeLeaf) area2.getChild("a2pv1");

		assertEquals("a2pv1", ((AlarmTreeItem<?>) a2pv1).getName());
		assertEquals("a2pv1 description", a2pv1.getDescription());
		assertTrue(a2pv1.isEnabled());
		assertTrue(a2pv1.isLatching());
		assertTrue(a2pv1.isAnnunciating());
	}
}
