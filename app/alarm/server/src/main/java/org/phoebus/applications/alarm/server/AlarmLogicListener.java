/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** Listener to {@link AlarmLogic} updates
 *  @author Kay Kasemir
 */
public interface AlarmLogicListener
{
    /** Invoked when enablement changes.
     *  @param is_enabled Is alarm logic now enabled?
     */
    public default void alarmEnablementChanged(boolean is_enabled) {}

    /** Invoked on change in alarm state, current or latched,
     *  to allow for notification of clients.
     *  @param current Current state of the input PV
     *  @param alarm Alarm state, which might be latched or delayed
     */
    public void alarmStateChanged(AlarmState current, AlarmState alarm);

    /** Invoked when annunciation is required.
     *  @param level Level to annunciate
     */
    public void annunciateAlarm(SeverityLevel level);

    /** Invoked when a 'global' alarm change happened:
     *  An alarm stayed un-acknowledged past the 'global' delay,
     *  or such a global alarm state cleared.
     *  @param alarm Global Alarm state
     */
    public default void globalStateChanged(AlarmState alarm) {}
}
