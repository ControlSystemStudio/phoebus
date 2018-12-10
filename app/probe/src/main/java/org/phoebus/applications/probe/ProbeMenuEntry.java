/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.probe;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/**
 * Menu entry that starts probe
 *
 * @author Kunal Shroff
 */
public class ProbeMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return Probe.DISPLAYNAME;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(Probe.class, "/icons/probe.png");
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.createInstance(Probe.NAME);
        return null;
    }

    @Override
    public String getMenuPath() {
        return Messages.ProbeMenuPath;
    }
}
