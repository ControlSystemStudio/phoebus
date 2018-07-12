/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

/** Alarm severity level
 *
 *  <p>Basic severities plus 'acknowledged' variants.
 *
 *  <p>Defined as an enum with known instances OK, MINOR, .. to allow quick
 *  comparisons via "==".
 *
 *  <p>The enum's <code>ordinal()</code> provides the severity level
 *  for an alarm condition as used by user interface tools to
 *  arrange alarms for display purposes.
 *  Higher ordinal means more severe.
 *  For example, MAJOR is more severe than MINOR.
 *  MAJOR is also more severe than MAJOR_ACK as far as <i>different PVs</i>
 *  are concerned:
 *  If different PVs in a group are in MINOR, MAJOR and MAJOR_ACK states,
 *  the summary for the group would indicate MAJOR to reflect the highest alarm
 *  in the group.
 *  <p>
 *  The 'ordinal' order of severity levels is therefore:
 *  0-OK,
 *  1-MINOR_ACK,
 *  2-MAJOR_ACK,
 *  3-INVALID_ACK,
 *  4-UNDEFINED_ACK,
 *  5-MINOR,
 *  6-MAJOR,
 *  7-INVALID,
 *  8-UNDEFINED.
 *
 *  <p>Inside the alarm server, a slightly different order is used to handle
 *  alarm updates: If a PV is in MINOR alarm and receives a MAJOR value,
 *  it will of course update to MAJOR alarm state. But if it's in MAJOR_ACK
 *  and now receives new values with MINOR or MAJOR severity, the existing
 *  acknowledgment for MAJOR means that it stays in MAJOR_ACK. So for the
 *  server MAJOR_ACK takes precedence over MAJOR or MINOR.
 *
 *  <p>The order for updating the alarm level of a PV is therefore:
 *  0-OK,
 *  1-MINOR,
 *  2-MINOR_ACK,
 *  3-MAJOR,
 *  4-MAJOR_ACK,
 *  5-INVALID,
 *  6-INVALID_ACK,
 *  7-UNDEFINED,
 *  8-UNDEFINED_ACK.
 *
 *  @author Kay Kasemir
 */
public enum SeverityLevel
{
    /** OK/NO_ALARM/normal/good */
    OK(false, 0),

    /** Acknowledged minor issue */
    MINOR_ACK(false, 2),

    /** Acknowledged major issue */
    MAJOR_ACK(false, 4),

    /** Acknowledged invalid condition */
    INVALID_ACK(false, 6),

    /** Acknowledged undefined condition */
    UNDEFINED_ACK(false, 8),

    /** Minor issue */
    MINOR(true, 1),

    /** Major issue */
    MAJOR(true, 3),

    /** Invalid condition, potentially very bad */
    INVALID(true, 5),

    /** Unknown states, potentially very bad */
    UNDEFINED(true, 7);

    /** Active alarm (not OK, not acknowledged?) */
    private final boolean active;

    /** Priority used for alarm state updates in the alarm server */
    private final int alarm_update_priority;

    /** Initialize severity level
     *  @param active <code>true</code> for active alarm severity,
     *                <code>false</code> for acknowledged or OK state
     *  @param alarm_update_priority Priority used inside the server to
     *                               update alarm severity of a PV.
     */
    private SeverityLevel(final boolean active,
                  final int alarm_update_priority)
    {
        this.active = active;
        this.alarm_update_priority = alarm_update_priority;
    }

    /** @return <code>true</code> if severity indicates an active alarm,
     *          <code>false</code> for acknowledged or OK state
     */
    public boolean isActive()
    {
        return active;
    }

    /** @return Priority used inside the server to update alarm severity
     *          of a PV.
     */
    public int getAlarmUpdatePriority()
    {
        return alarm_update_priority;
    }
}
