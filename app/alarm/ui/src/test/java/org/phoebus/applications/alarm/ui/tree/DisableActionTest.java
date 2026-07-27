/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.ui.tree;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.EnabledState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DisableActionTest {

    @Test
    public void testFindAffectedPvs_5() {
        AlarmTreeItem<BasicState> parent1 = new AlarmClientNode("/root", "parent1");
        AlarmTreeItem<BasicState> parent2 = new AlarmClientNode("/parent", "parent2");
        AlarmClientLeaf child1 = new AlarmClientLeaf("/paren1", "child1");
        parent2.addToParent(parent1);
        child1.setEnabled(false);
        child1.addToParent(parent1);
        AlarmClientLeaf child2 = new AlarmClientLeaf("/parent2", "child2");
        child2.addToParent(parent2);
        AlarmClientLeaf child3 = new AlarmClientLeaf("/parent2", "child3");
        child3.setEnabled(new EnabledState(LocalDateTime.now()));
        child3.addToParent(parent2);

        Set<AlarmClientLeaf> total = new HashSet<>();
        Set<AlarmClientLeaf> withEnableDate = new HashSet<>();

        DisableAction.findAffectedPVs(parent1, total, withEnableDate);

        assertEquals(3, total.size());
        assertEquals(1, withEnableDate.size());
    }
}
