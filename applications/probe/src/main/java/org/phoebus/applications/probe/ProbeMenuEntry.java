/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.probe;

import org.phoebus.framework.spi.MenuEntry;

/**
 * Menu entry that starts probe
 * 
 * @author Kunal Shroff
 */
public class ProbeMenuEntry implements MenuEntry {

    @Override
    public String getName() {
        return Probe.NAME;
    }

    /**
     * 
     * @return 
     */
    @Override
    public Void call() throws Exception {
        new Probe().create();
        return null;
    }

    @Override
    public String getMenuPath() {
        return "Display.Utility";
    }
}
