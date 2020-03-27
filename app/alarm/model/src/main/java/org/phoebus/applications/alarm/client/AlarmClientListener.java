/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import org.phoebus.applications.alarm.model.AlarmTreeItem;

/** Listener to the {@link AlarmClient}
 *  @author Kay Kasemir
 */
public interface AlarmClientListener
{
    /** @param alive Is server alive, or has it timed out? */
    void serverStateChanged(boolean alive);

    /** @param maintenance_mode Is the server in 'maintenance' mode? Else 'normal' */
    void serverModeChanged(boolean maintenance_mode);

    /** @param disable_notify Should email notifications be disabled? */
    void serverDisableNotifyChanged(boolean disable_notify);

    /** @param item Item that has been added */
    void itemAdded(AlarmTreeItem<?> item);

    /** @param item Item that has been removed */
    void itemRemoved(AlarmTreeItem<?> item);

    /** Called when the settings or the alarm state
     *  of an item have changed.
     *  This does not include the addition or removal
     *  of child items, which will be reported via
     *  the other methods of this interface
     *
     *  @param item Item that has been updated
     */
    void itemUpdated(AlarmTreeItem<?> item);
}
