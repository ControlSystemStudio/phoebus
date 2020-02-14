/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.diag;

import javafx.scene.image.Image;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.MenuEntry;

/**
 * Menu entry that starts Formula Tree.
 */
public class FormulaTreeMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return FormulaTreeApp.DISPLAYNAME;
    }

    @Override
    public Void call() throws Exception {
        ApplicationService.findApplication(FormulaTreeApp.NAME).create();
        return null;
    }

    @Override
    public Image getIcon() {
        return FormulaTreeApp.icon;
    }

    @Override
    public String getMenuPath() {
        return "Debug";
    }
}
