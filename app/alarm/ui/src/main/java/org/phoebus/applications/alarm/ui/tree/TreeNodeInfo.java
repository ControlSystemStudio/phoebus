package org.phoebus.applications.alarm.ui.tree;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Data object defining a set of properties for the <code>leaves</code>
 * @param leaves A {@link Set} of {@link AlarmClientLeaf}s. Note that the elements do not necessarily share the same parent node.
 * @param disabled Number of {@link AlarmClientLeaf}s in {@link #leaves} disabled indefinitely, i.e. disabled with no enable date set.
 * @param disabledWithEnableDate Number of {@link AlarmClientLeaf}s disabled with an enable date set.
 * @param commonEnableDate Non-empty {@link Optional} if <i>all</i> the {@link AlarmClientLeaf}s counted by {@link #disabledWithEnableDate}
 *                         have the same enable date.
 */
public record TreeNodeInfo(Set<AlarmClientLeaf> leaves, int disabled, int disabledWithEnableDate, Optional<LocalDateTime> commonEnableDate) {
}
