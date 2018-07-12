/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.email.actions;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;

/**
 * Menu entry that starts email dialog
 *
 * @author Kunal Shroff
 */
public class CreateEmailMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return EmailApp.DISPLAY_NAME;
    }

    /**
     *
     * @return
     */
    @Override
    public Void call() throws Exception {
        ApplicationService.createInstance(EmailApp.NAME);
        return null;
    }

    @Override
    public String getMenuPath() {
        return "Utility";
    }
}
