/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.area;

import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.ui.AlarmURI;
import org.phoebus.applications.alarm.ui.tree.AlarmTreeMenuEntry;

import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;

/** Action to open a tree view from an area view.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class OpenTreeViewAction extends MenuItem {

    /**
     * Constructor
     * @param alarmConfigName The alarm configuration name
     */
    public OpenTreeViewAction(String alarmConfigName) {
        this(alarmConfigName, null);
    }

    /**
     * Constructor
     * @param alarmConfigName The alarm configuration name
     * @param alarmRawQuery raw query for alarm (null if no such information is available)
     */
    public OpenTreeViewAction(String alarmConfigName, String alarmRawQuery) {
        final AlarmTreeMenuEntry entry = new AlarmTreeMenuEntry();
        entry.setResource(AlarmURI.createURI(AlarmSystem.server, alarmConfigName, alarmRawQuery));
        setText(entry.getName());
        setGraphic(new ImageView(entry.getIcon()));
        setOnAction(event ->
        {
            try
            {
                entry.call();
            }
            catch (Exception ex)
            {
                AlarmSystem.logger.log(Level.WARNING, "Cannot open alarm tree", ex);
            }
        });
    }

}
