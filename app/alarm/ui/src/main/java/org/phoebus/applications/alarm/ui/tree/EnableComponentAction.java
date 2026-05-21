/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.ui.javafx.ImageCache;

import java.util.List;

/**
 * Action that enables items in the alarm tree configuration
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
class EnableComponentAction extends MenuItem {
    /**
     * @param node  Node to position dialog
     * @param model {@link AlarmClient}
     * @param items Items to enable
     */
    public EnableComponentAction(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items) {
        if (doEnable()) {
            setText(Messages.enableAlarms);
            setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/enabled.png"));
        } else {
            setText(Messages.disableAlarms);
            setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/disabled.png"));
        }

        setOnAction(event -> ComponentActionHelper.updateEnablement(node, model, items, doEnable()));
    }

    // Implementation can actually disable or enable. Which one is it going to be?
    protected boolean doEnable() {
        return true;
    }
}
