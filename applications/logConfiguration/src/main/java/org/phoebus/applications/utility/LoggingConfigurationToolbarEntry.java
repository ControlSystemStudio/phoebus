/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.utility;

import org.phoebus.framework.spi.ToolbarEntry;

/**
 * Toolbar entry that starts LoggingConfiguration
 *
 * @author Kunal Shroff
 */
// @ProviderFor(ToolbarEntry.class)
public class LoggingConfigurationToolbarEntry implements ToolbarEntry {

    @Override
    public String getName() {
        return LoggingConfigurationApplication.NAME;
    }

    @Override
    public void call() throws Exception {
        // TODO Get app descriptor from somewhere instead of creating new one
        new LoggingConfigurationApplication().create();
    }
}
