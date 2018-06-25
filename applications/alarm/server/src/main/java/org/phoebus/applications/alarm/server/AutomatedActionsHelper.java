/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetail;

/** Helper for handling {@link AutomatedActions} in server nodes and PVs
 *  @author Kay Kasemir
 */
class AutomatedActionsHelper
{
    static void update(final AtomicReference<AutomatedActions> automated_actions,
                       final SeverityLevel severity)
    {
        final AutomatedActions actions = automated_actions.get();
        if (actions != null)
            actions.handleSeverityUpdate(severity);
    }

    static void configure(final AtomicReference<AutomatedActions> automated_actions,
                          final AlarmTreeItem<?> item,
                          final boolean is_active,
                          final boolean enabled,
                          final List<TitleDetail> actions)
    {
        // Update Automated Actions since their configuration changed
        final AutomatedActions new_actions =
            (actions.isEmpty() ||  !enabled)
            ? null
            : new AutomatedActions(item, is_active,
                                   AutomatedActionExecutor.INSTANCE);

        // Cancel previous ones.
        final AutomatedActions previous = automated_actions.getAndSet(new_actions);
        if (previous != null)
            previous.cancel();
    }

    static void cancel(final AtomicReference<AutomatedActions> automated_actions)
    {
        final AutomatedActions actions = automated_actions.get();
        if (actions != null)
            actions.cancel();
    }
}
