/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

import javafx.scene.Node;

/** Action that disables items in the alarm tree configuration
 *  @author Kay Kasemir
 */
class DisableComponentAction extends EnableComponentAction
{
    /** @param node Node to position dialog
     *  @param model {@link AlarmClient}
     *  @param items Items to disable
     */
    public DisableComponentAction(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items)
    {
        super(node, model, items);
    }

    @Override
    protected boolean doEnable()
    {
        return false;
    }
}
