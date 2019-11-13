/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/** Menu entry for opening {@link ErrLogApp}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ErrLogMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return ErrLogApp.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(ErrLog.class, "/icons/errlog.png");
    }

    @Override
    public String getMenuPath()
    {
        return "Debug";
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(ErrLogApp.NAME);
        return null;
    }
}
