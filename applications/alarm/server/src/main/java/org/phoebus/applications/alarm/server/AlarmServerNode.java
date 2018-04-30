/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmClientNode;
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
            // TODO if child is PV, and disabled, will sevr be OK?
            final SeverityLevel child_severity = child.getState().severity;
            if (child_severity.ordinal() > new_severity.ordinal())
                new_severity = child_severity;
        }

        if (new_severity != getState().severity)
        {
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
