/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.channel.views;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/**
 * Menu entry that starts Channel Tree
 *
 * @author Kunal Shroff
 */
public class ChannelTreeMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return ChannelTreeApp.DISPLAYNAME;
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.findApplication(ChannelTreeApp.NAME).create();
        return null;
    }

    @Override
    public Image getIcon() {
        return ChannelTreeApp.icon;
    }

    @Override
    public String getMenuPath() {
        return "Channel";
    }
}
