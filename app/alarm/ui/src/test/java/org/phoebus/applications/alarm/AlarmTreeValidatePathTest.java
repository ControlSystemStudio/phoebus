/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.ui.tree.AlarmTreeHelper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        AlarmClientNode area1 = new AlarmClientNode(root.getPathName(), "Area 1");
        area1.addToParent(root);

        AlarmClientLeaf pv1 = new AlarmClientLeaf(area1.getPathName(), "PV 1");
        pv1.addToParent(area1);

        AlarmClientNode area2 = new AlarmClientNode(root.getPathName(), "Area 2");
        area2.addToParent(root);

        AlarmClientLeaf pv2 = new AlarmClientLeaf(area2.getPathName(), "PV 2");
        pv2.addToParent(area2);

        AlarmClientNode area3 = new AlarmClientNode(area1.getPathName(), "Area 3");
        area3.addToParent(area1);

        AlarmClientLeaf pv3 = new AlarmClientLeaf(area3.getPathName(), "PV 3");
        pv3.addToParent(area3);

        AlarmClientNode area4 = new AlarmClientNode(area2.getPathName(), "Area 4");
        area4.addToParent(area2);

        AlarmClientLeaf pv4 = new AlarmClientLeaf(area4.getPathName(), "PV 4");
        pv4.addToParent(area4);

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
