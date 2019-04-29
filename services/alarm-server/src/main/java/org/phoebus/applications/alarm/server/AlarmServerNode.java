/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.server.actions.AutomatedActions;
import org.phoebus.applications.alarm.server.actions.AutomatedActionsHelper;

/** Alarm tree node as used by server
 *
 *  <p>Is part of ServerModel, can maximize severity.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
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

    private final AtomicReference<AutomatedActions> automated_actions = new AtomicReference<>();

    private volatile String severity_pv_name = null;

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
            model.sendStateUpdate(getPathName(), new_state);

            // Update automated actions
            AutomatedActionsHelper.update(automated_actions, new_severity);

            // Write optional severity PV
            final String pv = severity_pv_name;
            if (pv != null)
                SeverityPVHandler.update(pv, new_severity);
        }

        // Percolate changes towards root
        if (parent instanceof AlarmServerNode)
            ((AlarmServerNode) parent).maximizeSeverity();
    }

    @Override
    public boolean setActions(final List<TitleDetailDelay> actions)
    {
        if (super.setActions(actions))
        {
            AutomatedActionsHelper.configure(automated_actions, this,
                                             getState().severity,
                                             true, actions);

            String severity_pv_name = null;
            for (TitleDetailDelay action : actions)
                if (action.detail.startsWith(TitleDetailDelay.SEVRPV))
                {
                    final String pv_name = action.detail.substring(TitleDetailDelay.SEVRPV.length());
                    if (severity_pv_name != null)
                        logger.log(Level.WARNING,
                                   "Multiple severity PVs for '" + getPathName() + "', '" +
                                   severity_pv_name + "' as well as '" + pv_name + "'");
                    severity_pv_name = pv_name;
                }

            if (! Objects.equals(this.severity_pv_name, severity_pv_name))
            {
                this.severity_pv_name = severity_pv_name;
                // Initial update, since severity may not change for a while
                if (severity_pv_name != null)
                    SeverityPVHandler.update(severity_pv_name, getState().severity);
            }
            return true;
        }
        return false;
    }
}
