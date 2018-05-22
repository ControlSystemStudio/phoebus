package org.phoebus.applications.alarm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.phoebus.applications.alarm.model.xml.XmlModelReader;

public class AlarmModelReaderTest
{
	// XML Alarm Test Model
	public static final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
	+ "<config name=\"Test\">\n"
	+ "  <component name=\"Area1\">\n"
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
	}
}
