/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server.actions;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetailDelay;

/** Helper for handling {@link AutomatedActions} in server nodes and PVs
 *  @author Kay Kasemir
 */
public class AutomatedActionsHelper
{
    /** Configure {@link AutomatedActions}
     *
     *  <p>Sets or updates a reference to automated actions.
     *
     *  @param automated_actions {@link AutomatedActions} to configure
     *  @param item Item on which the actions should operate
     *  @param initial_severity Initial alarm severity (which is ignored)
     *  @param enabled Is the alarm enabled?
     *  @param actions Actions to execute
     */
    public static void configure(final AtomicReference<AutomatedActions> automated_actions,
                                 final AlarmTreeItem<?> item,
                                 final SeverityLevel initial_severity,
                                 final boolean enabled,
                                 final List<TitleDetailDelay> actions)
    {
        // Update Automated Actions since their configuration changed
        final AutomatedActions new_actions =
            (actions.isEmpty() ||  !enabled)
            ? null
            : new AutomatedActions(item, initial_severity, AutomatedActionExecutor.INSTANCE);

        // Cancel previous ones.
        final AutomatedActions previous = automated_actions.getAndSet(new_actions);
        if (previous != null)
            previous.cancel();
    }

    /** Update automated actions
     *
     *  @param automated_actions Ref to actions. May be empty.
     *  @param severity Alarm severity that may trigger or cancel actions
     */
    public static void update(final AtomicReference<AutomatedActions> automated_actions,
                              final SeverityLevel severity)
    {
        final AutomatedActions actions = automated_actions.get();
        if (actions != null)
            actions.handleSeverityUpdate(severity);
    }

    /** @param automated_actions Actions to cancel */
    public static void cancel(final AtomicReference<AutomatedActions> automated_actions)
    {
        final AutomatedActions actions = automated_actions.get();
        if (actions != null)
            actions.cancel();
    }
}
