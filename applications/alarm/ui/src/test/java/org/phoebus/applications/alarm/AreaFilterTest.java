package org.phoebus.applications.alarm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.ui.area.AreaFilter;

public class AreaFilterTest
{
	@Test
	public void testAreaFilter()
	{
		final int level = 2;

		// Build the test tree.
		final AlarmClientNode root = new AlarmClientNode(null, "root");
		final AlarmClientNode a1l2 = new AlarmClientNode(root, "Area 1 Level 2");
		final AlarmClientNode a2l2 = new AlarmClientNode(root, "Area 2 Level 2");
		final AlarmClientNode a3l2 = new AlarmClientNode(root, "Area 3 Level 2");
		final AlarmClientNode a1l3 = new AlarmClientNode(a3l2, "Area 1 Level 3");

		// Initialize an area filter to the desired level.
		AreaFilter areaFilter = new AreaFilter(level);

		// Test the message filtering.
		assertTrue(areaFilter.filter(a1l2));
		assertTrue(areaFilter.filter(a2l2));
		assertTrue(areaFilter.filter(a3l2));
		assertFalse(areaFilter.filter(a1l3));

		// Try a different level.
		areaFilter = new AreaFilter(level + 1);

		assertFalse(areaFilter.filter(a1l2));
		assertFalse(areaFilter.filter(a2l2));
		assertFalse(areaFilter.filter(a3l2));
		assertTrue(areaFilter.filter(a1l3));
	}
}
