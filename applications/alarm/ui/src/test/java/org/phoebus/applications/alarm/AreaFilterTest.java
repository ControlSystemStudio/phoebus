package org.phoebus.applications.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

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
		final AreaFilter areaFilter = new AreaFilter(level);

		String name = areaFilter.filter(a1l2);

		assertEquals("Area 1 Level 2", name);
		name = areaFilter.filter(a2l2);
		assertEquals("Area 2 Level 2", name);
		name = areaFilter.filter(a3l2);
		assertEquals("Area 3 Level 2", name);
		name = areaFilter.filter(root);
		assertEquals(null, name);
		name = areaFilter.filter(a1l3);
		assertEquals(null, name);

		final List<String> expected = Arrays.asList("Area 1 Level 2", "Area 2 Level 2", "Area 3 Level 2");
		final List<String> actual = areaFilter.getItems();

		assertListsEquivalent(expected, actual);

	}

	// Returns if all strings in list "actual" are also contained in list "expected".
	private void assertListsEquivalent(List<String> expected, List<String> actual)
	{
		assertEquals(expected.size(), actual.size());
		for (final String str : actual)
		{
			assertTrue(actual.contains(str));
		}
	}
}
