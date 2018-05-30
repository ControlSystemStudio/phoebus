/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.area;

import java.util.logging.Level;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.application.ApplicationLauncherService;

import javafx.scene.control.MenuItem;
/** Action to open a tree view from an area view.
 *  @author Evan Smith
 */
public class OpenTreeViewAction extends MenuItem
{
	public OpenTreeViewAction()
	{
		super("Open Alarm Tree");
		setOnAction((event)->
		{
			final AppDescriptor app = ApplicationService.findApplication("alarm_tree");
	        if (app == null)
	        {
	        	ApplicationLauncherService.logger.log(Level.SEVERE, "Unknown application 'alarm_tree'");
	        	return;
	        }
	        app.create();
		});
	}
}
