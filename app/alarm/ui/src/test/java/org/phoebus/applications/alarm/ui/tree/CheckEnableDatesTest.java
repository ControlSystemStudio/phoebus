package org.phoebus.applications.alarm.ui.tree;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckEnableDatesTest {

    AlarmClientLeaf leaf0 = new AlarmClientLeaf("test/path/0", "testName0");
    AlarmClientLeaf leaf1 = new AlarmClientLeaf("test/path/1", "testName1");
    AlarmClientLeaf leaf2 = new AlarmClientLeaf("test/path/2", "testName2");

    List<AlarmTreeItem<?>> items = new ArrayList<>();
    Set<AlarmClientLeaf> totalLeafItems = new HashSet<>();
    Set<AlarmClientLeaf> leavesWithEnableDate = new HashSet<>();

    LocalDateTime testTime = LocalDateTime.now().plusDays(3);

    @Test
    public void noEnableDates(){
        items.add(leaf0);
        items.add(leaf1);
        items.add(leaf2);

        assertTrue(DisableAction.checkEnableDates(items, totalLeafItems, leavesWithEnableDate));
    }

    @Test
    public void identicalEnableDates(){
        leaf0.setEnabledDate(testTime);
        leaf1.setEnabledDate(testTime);
        leaf2.setEnabledDate(testTime);

        items.add(leaf0);
        items.add(leaf1);
        items.add(leaf2);

        assertTrue(DisableAction.checkEnableDates(items, totalLeafItems, leavesWithEnableDate));
    }

    @Test
    public void differentEnableDates(){
        leaf0.setEnabledDate(testTime);
        leaf1.setEnabledDate(testTime.plusDays(1));
        leaf2.setEnabledDate(testTime);

        items.add(leaf0);
        items.add(leaf1);
        items.add(leaf2);

        assertFalse(DisableAction.checkEnableDates(items, totalLeafItems, leavesWithEnableDate));
    }

    @Test
    public void sameSize(){
        leaf0.setEnabledDate(testTime);
        leaf1.setEnabledDate(testTime);

        items.add(leaf0);
        items.add(leaf1);
        items.add(leaf2);

        assertFalse(DisableAction.checkEnableDates(items, totalLeafItems, leavesWithEnableDate));
    }
}
