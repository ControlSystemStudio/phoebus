/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.util.List;
import java.util.Objects;

/** Base class for all nodes in the alarm tree that hold a 'state'
 *  @param STATE Type used for the alarm state
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTreeItemWithState<STATE extends BasicState> extends AlarmTreeItem<STATE>
{
    protected AlarmTreeItemWithState(final AlarmTreeItem<BasicState> parent, final String name,
                                     final List<AlarmTreeItem<?>> children)
    {
        super(parent, name, children);
    }

    protected volatile STATE state;

    /** @param state State
     *  @return <code>true</code> if this changed the state
     */
    public boolean setState(final STATE state)
    {
        if (Objects.equals(this.state, state))
            return false;
        this.state = state;
        return true;
    }

    @Override
    public STATE getState()
    {
        return state;
    }

    @Override
    public String toString()
    {
        return getName() + " = " + state;
    }
}
