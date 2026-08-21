package org.phoebus.applications.alarm.ui.tree;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AlarmTreeHelperTest {

    @Test
    public void testGetAlarmLeaves1() {
        AlarmClientLeaf leaf0 = new AlarmClientLeaf("test/path/0", "testName0");
        AlarmClientLeaf leaf1 = new AlarmClientLeaf("test/path/1", "testName1");
        AlarmClientLeaf leaf2 = new AlarmClientLeaf("test/path/2", "testName2");

        assertEquals(3, AlarmTreeHelper.getLeafItems(List.of(leaf0, leaf1, leaf2)).size());
    }

    @Test
    public void testGetAlarmLeaves2() {
        AlarmTreeItem<BasicState> parent1 = new AlarmClientNode("/root", "parent1");
        AlarmTreeItem<BasicState> parent2 = new AlarmClientNode("/parent", "parent2");
        AlarmClientLeaf child1 = new AlarmClientLeaf("/paren1", "child1");
        parent2.addToParent(parent1);
        child1.addToParent(parent1);
        AlarmClientLeaf child2 = new AlarmClientLeaf("/parent2", "child2");
        child2.addToParent(parent1);

        assertEquals(2, AlarmTreeHelper.getLeafItems(List.of(parent1, parent2)).size());
    }

    @Test
    public void testGetAlarmLeaves3() {
        AlarmTreeItem<BasicState> parent1 = new AlarmClientNode("/root", "parent1");
        AlarmTreeItem<BasicState> parent2 = new AlarmClientNode("/parent", "parent2");
        AlarmClientLeaf child1 = new AlarmClientLeaf("/parent1", "child1");
        parent2.addToParent(parent1);
        child1.addToParent(parent1);
        AlarmClientLeaf child2 = new AlarmClientLeaf("/parent2", "child2");
        child2.addToParent(parent2);

        assertEquals(2, AlarmTreeHelper.getLeafItems(parent1).size());
    }

    @Test
    public void testGetAlarmLeaves4() {
        AlarmTreeItem<BasicState> parent1 = new AlarmClientNode("/root", "parent1");
        AlarmTreeItem<BasicState> parent2 = new AlarmClientNode("/parent", "parent2");
        AlarmClientLeaf child1 = new AlarmClientLeaf("/paren1", "child1");
        parent2.addToParent(parent1);
        child1.addToParent(parent1);
        AlarmClientLeaf child2 = new AlarmClientLeaf("/parent2", "child2");
        child2.addToParent(parent2);
        AlarmClientLeaf child3 = new AlarmClientLeaf("/parent2", "child3");
        child3.setEnabled(false);
        child3.addToParent(parent2);

        Set<AlarmClientLeaf> leaves = AlarmTreeHelper.getLeafItems(List.of(parent1, child3, new AlarmClientLeaf("/none", "child4")));

        assertEquals(4, leaves.size());
    }

    @Test
    public void testGetAlarmNodeInfo1() {
        AlarmTreeItem<BasicState> parent1 = new AlarmClientNode("/root", "parent1");
        AlarmClientLeaf leaf0 = new AlarmClientLeaf("test/path/0", "testName0");
        leaf0.addToParent(parent1);
        AlarmClientLeaf leaf1 = new AlarmClientLeaf("test/path/1", "testName1");
        leaf1.addToParent(parent1);
        AlarmClientLeaf leaf2 = new AlarmClientLeaf("test/path/2", "testName2");
        leaf2.addToParent(parent1);

        TreeNodeInfo treeNodeInfo = AlarmTreeHelper.getTreeNodeInfo(parent1);

        assertEquals(3, treeNodeInfo.leaves().size());
        assertEquals(0, treeNodeInfo.disabled());
        assertEquals(0, treeNodeInfo.disabledWithEnableDate());
        assertTrue(treeNodeInfo.commonEnableDate().isEmpty());
    }

    @Test
    public void testGetAlarmNodeContent2() {
        AlarmTreeItem<BasicState> parent1 = new AlarmClientNode("/root", "parent1");
        AlarmClientLeaf leaf0 = new AlarmClientLeaf("test/path/0", "testName0");
        leaf0.setEnabled(false);
        leaf0.addToParent(parent1);
        AlarmClientLeaf leaf1 = new AlarmClientLeaf("test/path/1", "testName1");
        leaf1.addToParent(parent1);
        AlarmClientLeaf leaf2 = new AlarmClientLeaf("test/path/2", "testName2");
        leaf2.addToParent(parent1);

        TreeNodeInfo treeNodeInfo = AlarmTreeHelper.getTreeNodeInfo(parent1);

        assertEquals(3, treeNodeInfo.leaves().size());
        assertEquals(1, treeNodeInfo.disabled());
        assertEquals(0, treeNodeInfo.disabledWithEnableDate());
        assertTrue(treeNodeInfo.commonEnableDate().isEmpty());

    }

    @Test
    public void testGetAlarmNodeContent3() {
        AlarmTreeItem<BasicState> parent1 = new AlarmClientNode("/root", "parent1");
        LocalDateTime localDateTime1 = LocalDateTime.now().plusDays(1);
        LocalDateTime localDateTime2 = localDateTime1.plusDays(2);
        AlarmClientLeaf leaf0 = new AlarmClientLeaf("test/path/0", "testName0");
        leaf0.setEnabledDate(localDateTime1);
        leaf0.addToParent(parent1);
        AlarmClientLeaf leaf1 = new AlarmClientLeaf("test/path/1", "testName1");
        leaf1.setEnabledDate(localDateTime2);
        leaf1.addToParent(parent1);
        AlarmClientLeaf leaf2 = new AlarmClientLeaf("test/path/2", "testName2");
        leaf2.addToParent(parent1);

        TreeNodeInfo treeNodeInfo = AlarmTreeHelper.getTreeNodeInfo(parent1);

        assertEquals(3, treeNodeInfo.leaves().size());
        assertEquals(0, treeNodeInfo.disabled());
        assertEquals(2, treeNodeInfo.disabledWithEnableDate());
        assertTrue(treeNodeInfo.commonEnableDate().isEmpty());
    }

    @Test
    public void testGetAlarmNodeContent4() {
        AlarmTreeItem<BasicState> parent1 = new AlarmClientNode("/root", "parent1");
        LocalDateTime localDateTime1 = LocalDateTime.now().plusDays(1);
        AlarmClientLeaf leaf0 = new AlarmClientLeaf("test/path/0", "testName0");
        leaf0.setEnabledDate(localDateTime1);
        leaf0.addToParent(parent1);
        AlarmClientLeaf leaf1 = new AlarmClientLeaf("test/path/1", "testName1");
        leaf1.setEnabledDate(localDateTime1);
        leaf1.addToParent(parent1);
        AlarmClientLeaf leaf2 = new AlarmClientLeaf("test/path/2", "testName2");
        leaf2.setEnabled(false);
        leaf2.addToParent(parent1);

        TreeNodeInfo treeNodeInfo = AlarmTreeHelper.getTreeNodeInfo(parent1);

        assertEquals(3, treeNodeInfo.leaves().size());
        assertEquals(1, treeNodeInfo.disabled());
        assertEquals(2, treeNodeInfo.disabledWithEnableDate());
        assertTrue(treeNodeInfo.commonEnableDate().isPresent());
        assertEquals(localDateTime1, treeNodeInfo.commonEnableDate().get());
    }
}
