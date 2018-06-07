/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.ui.tree.AlarmTreeHelper;

/** Test of {@link AlarmTreeHelper}
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmTreeValidatePathTest
{
    // Needed for the AlarmClientNodes and AlarmClientLeaves other than root.
    @SuppressWarnings("unused")
    @Test
    public void testValidatePath()
    {
        // Build the test tree.
        AlarmClientNode root = new AlarmClientNode(null, "root");
        AlarmClientNode area1 = new AlarmClientNode(root, "Area 1");
        AlarmClientLeaf pv1 = new AlarmClientLeaf(area1, "PV 1");
        AlarmClientNode area2 = new AlarmClientNode(root, "Area 2");
        AlarmClientLeaf pv2 = new AlarmClientLeaf(area2, "PV 2");
        AlarmClientNode area3 = new AlarmClientNode(area1, "Area 3");
        AlarmClientLeaf pv3 = new AlarmClientLeaf(area3, "PV 3");
        AlarmClientNode area4 = new AlarmClientNode(area2, "Area 4");
        AlarmClientLeaf pv4 = new AlarmClientLeaf(area4, "PV 4");

        // root -> Area 1 -> Area 3 -> PV 3
        //                -> PV 1
        //      -> Area 2 -> Area 4 -> PV 4
        //                -> PV 2

        // Valid examples of inserting new items at all path locations. All should be true.
        assertTrue(AlarmTreeHelper.validateNewPath("root/new", root));
        assertTrue(AlarmTreeHelper.validateNewPath("root/Area 1/new", root));
        assertTrue(AlarmTreeHelper.validateNewPath("root/Area 2/new", root));
        assertTrue(AlarmTreeHelper.validateNewPath("root/Area 1/Area 3/new", root));
        assertTrue(AlarmTreeHelper.validateNewPath("root/Area 2/Area 4/new", root));
        // splitPath(), used internally, consumes the '/' as a delimiter. This is valid.
        assertTrue(AlarmTreeHelper.validateNewPath("//root///////Area 1///", root));

        // Invalid. All should be false.
        assertFalse(AlarmTreeHelper.validateNewPath(null, root));
        assertFalse(AlarmTreeHelper.validateNewPath("", root));
        // The Alarm Tree only displays areas and PVs above the root level, and only allows for a single root level.
        // Any attempt to add a new root should be invalid.
        assertFalse(AlarmTreeHelper.validateNewPath("new", root));
        assertFalse(AlarmTreeHelper.validateNewPath("new/new PV", root));
        // The split path method does not consume leading or trailing whitespace of the path elements.
        assertFalse(AlarmTreeHelper.validateNewPath(" /root / Area 1 / PV 1 ", root));
        // Path contains PV. PV cannot have children.
        assertFalse(AlarmTreeHelper.validateNewPath("root/Area 1/PV 1/New PV", root));
    }
}
