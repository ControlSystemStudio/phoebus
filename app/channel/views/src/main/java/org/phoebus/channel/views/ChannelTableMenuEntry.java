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
 * Menu entry that starts Channel Table
 *
 * @author Kunal Shroff
 */
public class ChannelTableMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return ChannelTableApp.DISPLAYNAME;
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.findApplication(ChannelTableApp.NAME).create();
        return null;
    }

    @Override
    public Image getIcon() {
        return ChannelTableApp.icon;
    }

    @Override
    public String getMenuPath() {
        return "Channel";
    }
}
