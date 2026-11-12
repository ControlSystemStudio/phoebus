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
import org.phoebus.applications.alarm.model.EnabledState;
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
class EnableAction extends MenuItem {
    /**
     * @param node  Node to position dialog
     * @param model {@link AlarmClient}
     * @param items Items to enable
     */
    public EnableAction(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items) {
        setText(Messages.enableAlarms);
        setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/enabled.png"));
        setOnAction(event -> {
            final TreeNodeInfo treeNodeInfo = AlarmTreeHelper.getTreeNodeInfo(items);
            ComponentActionHelper.updateEnabledState(node, model, new EnabledState(true), treeNodeInfo.leaves());
        });
    }
}
