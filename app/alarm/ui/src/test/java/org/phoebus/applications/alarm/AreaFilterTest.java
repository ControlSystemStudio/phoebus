/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.area.AreaFilter;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
		final AlarmClientNode a1l2 = new AlarmClientNode(root.getPathName(), "Area 1 Level 2");
		a1l2.addToParent(root);
		a1l2.setState(new BasicState(SeverityLevel.MAJOR));
		final AlarmClientNode a2l2 = new AlarmClientNode(root.getPathName(), "Area 2 Level 2");
		a2l2.addToParent(root);
		a2l2.setState(new BasicState(SeverityLevel.MINOR));
		final AlarmClientNode a3l2 = new AlarmClientNode(root.getPathName(), "Area 3 Level 2");
		a3l2.addToParent(root);
		a3l2.setState(new BasicState(SeverityLevel.OK));
		final AlarmClientNode a1l3 = new AlarmClientNode(a3l2.getPathName(), "Area 1 Level 3");
		a1l3.addToParent(a3l2);

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
		assertNull(name);

		name = areaFilter.filter(a1l3);
		assertNull(name);

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
