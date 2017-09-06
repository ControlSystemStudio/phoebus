/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.utility;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.ui.docking.DockStage;

import javafx.stage.Stage;

/**
 * Toolbar entry that starts Logging Configuration Application
 * 
 * @author Kunal Shroff
 */
public class LoggingConfigurationMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return LoggingConfiguration.NAME;
    }

    /**
     * 
     * @return 
     */
    @Override
    public Void call() throws Exception {
        LoggingConfiguration.open();
        return null;
    }

    @Override
    public String getMenuPath() {
        return null;
    }
}
