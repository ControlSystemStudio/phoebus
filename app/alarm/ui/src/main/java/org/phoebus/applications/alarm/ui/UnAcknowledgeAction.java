/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Action to acknowledge alarm
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class UnAcknowledgeAction extends MenuItem
{
    public UnAcknowledgeAction(final AlarmClient model, final List<AlarmTreeItem<?>> active)
    {
        super("Un-Acknowledge", ImageCache.getImageView(AlarmUI.class, "/icons/unacknowledge.png"));
        
        JobManager.schedule(getText(), monitor ->
        {
            setOnAction(event ->
                active.forEach(item -> model.acknowledge(item, false)));
        });
    }
}
