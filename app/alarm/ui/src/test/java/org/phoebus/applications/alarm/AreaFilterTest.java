/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.area.AreaFilter;
/** Tests the AreaFilter class and its methods.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AreaFilterTest
{
    @Test
	public void testAreaFilter()
	{
		final int level = 2;

		// Build the test tree.
		final AlarmClientNode root = new AlarmClientNode(null, "root");
		final AlarmClientNode a1l2 = new AlarmClientNode(root, "Area 1 Level 2");
		a1l2.setState(new BasicState(SeverityLevel.MAJOR));
		final AlarmClientNode a2l2 = new AlarmClientNode(root, "Area 2 Level 2");
		a2l2.setState(new BasicState(SeverityLevel.MINOR));
		final AlarmClientNode a3l2 = new AlarmClientNode(root, "Area 3 Level 2");
		a3l2.setState(new BasicState(SeverityLevel.OK));
		final AlarmClientNode a1l3 = new AlarmClientNode(a3l2, "Area 1 Level 3");

		// Initialize an area filter to the desired level.
		final AreaFilter areaFilter = new AreaFilter(level);

		String name = areaFilter.filter(a1l2);
		assertThat(name, equalTo("Area 1 Level 2"));
		assertEquals(SeverityLevel.MAJOR, areaFilter.getSeverity(name));

		name = areaFilter.filter(a2l2);
		assertEquals("Area 2 Level 2", name);
		assertEquals(SeverityLevel.MINOR, areaFilter.getSeverity(name));

		name = areaFilter.filter(a3l2);
		assertEquals("Area 3 Level 2", name);
		assertEquals(SeverityLevel.OK, areaFilter.getSeverity(name));

		name = areaFilter.filter(root);
		assertEquals(null, name);

		name = areaFilter.filter(a1l3);
		assertEquals(null, name);

		List<String> actual = areaFilter.getItems();

		assertThat(actual.size(), equalTo(3));
		assertThat(actual, hasItems("Area 1 Level 2", "Area 2 Level 2", "Area 3 Level 2"));

		// If Area 1 Level 2 is deleted from model, filter should reflect that.
		areaFilter.removeItem("Area 1 Level 2");
		assertThat(areaFilter.getSeverity("Area 1 Level 2"), equalTo(SeverityLevel.UNDEFINED));
		actual = areaFilter.getItems();
		assertThat(actual.size(), equalTo(2));
		assertThat(actual, hasItems("Area 2 Level 2", "Area 3 Level 2"));

		// Add it back.
		assertThat(areaFilter.filter(a1l2), equalTo("Area 1 Level 2"));
		assertEquals(SeverityLevel.MAJOR, areaFilter.getSeverity("Area 1 Level 2"));
		actual = areaFilter.getItems();
		assertThat(actual.size(), equalTo(3));
		assertThat(actual, hasItems("Area 1 Level 2", "Area 2 Level 2", "Area 3 Level 2"));
	}
}
