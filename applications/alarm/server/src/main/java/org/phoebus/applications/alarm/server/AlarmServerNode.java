/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** Alarm tree node as used by server
 *
 *  <p>Is part of ServerModel, can maximize severity.
 *  @author Kay Kasemir
 */
public class AlarmServerNode extends AlarmClientNode
{
    private final ServerModel model;
    // Alarm _server_ doesn't read the old alarm state,
    // since monitoring the state would mean it keeps reading
    // its own state updates.
    // That does mean, however, that it's unaware of the last state
    // sent out for all alarm nodes, so use a flag to assert one initial
    // notification to clients.
    private volatile boolean never_updated = true;

    public AlarmServerNode(final ServerModel model, final AlarmClientNode parent, final String name)
    {
        super(parent, name);
        this.model = model;
    }

    @Override
    public AlarmServerNode getParent()
    {
        return (AlarmServerNode) parent;
    }

    /** Set severity of this item by maximizing over its child severities.
     *  Recursively updates parent items.
     */
    public void maximizeSeverity()
    {
        SeverityLevel new_severity = SeverityLevel.OK;

        for (AlarmTreeItem<?> child : getChildren())
        {
            // Skip disabled PVs
            if ((child instanceof AlarmServerPV)  &&
                ! ((AlarmServerPV) child).isEnabled())
                continue;
            final SeverityLevel child_severity = child.getState().severity;
            if (child_severity.ordinal() > new_severity.ordinal())
                new_severity = child_severity;
        }

        if (never_updated  ||  new_severity != getState().severity)
        {
            never_updated = false;
            final BasicState new_state = new BasicState(new_severity);
            setState(new_state);
            model.sentStateUpdate(getPathName(), new_state);
        }

        // Percolate changes towards root
        if (parent instanceof AlarmServerNode)
            ((AlarmServerNode) parent).maximizeSeverity();
    }

    // TODO Port ServerTreeItem#updateSeverityPV()
}
