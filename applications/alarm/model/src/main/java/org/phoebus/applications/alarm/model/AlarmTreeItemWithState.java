/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.util.List;

/** Base class for all nodes in the alarm tree that hold a 'state'
 *  @param STATE Type used for the alarm state
 *  @author Kay Kasemir
 */
public class AlarmTreeItemWithState<STATE extends BasicState> extends AlarmTreeItem<STATE>
{
    protected AlarmTreeItemWithState(final AlarmTreeItem<BasicState> parent, final String name,
                                     final List<AlarmTreeItem<?>> children)
    {
        super(parent, name, children);
        // TODO Auto-generated constructor stub
    }

    protected volatile STATE state;

    public void setState(final STATE state)
    {
        this.state = state;
    }

    @Override
    public STATE getState()
    {
        return state;
    }
}
