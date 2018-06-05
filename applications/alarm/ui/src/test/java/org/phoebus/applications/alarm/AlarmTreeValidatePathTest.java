package org.phoebus.applications.alarm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.ui.tree.AlarmTreeHelper;

public class AlarmTreeValidatePathTest
{
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

        // Valid. All should be true.
        assertTrue(AlarmTreeHelper.validatePath("root/Area 1/PV 1", root));
        assertTrue(AlarmTreeHelper.validatePath("root/", root));
        // Split path consumes the '/' as a delimiter. This is valid.
        assertTrue(AlarmTreeHelper.validatePath("//root///////Area 1///", root));
        // PV 5 would be the new PV.
        assertTrue(AlarmTreeHelper.validatePath("//root/Area 2/Area 4/PV 5", root));
        
        // Invalid. All should be false.
        assertFalse(AlarmTreeHelper.validatePath(null, root));
        assertFalse(AlarmTreeHelper.validatePath("", root));
        assertFalse(AlarmTreeHelper.validatePath("do what now?", root));
        assertFalse(AlarmTreeHelper.validatePath("Area 1/root", root));
        // Path contains PV. PV cannot have children.
        assertFalse(AlarmTreeHelper.validatePath("root/Area 1/PV 1/New PV", root));
    }
}
