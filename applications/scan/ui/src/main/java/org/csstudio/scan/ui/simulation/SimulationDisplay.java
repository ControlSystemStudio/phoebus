/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.simulation;

import org.csstudio.scan.info.SimulationResult;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

public class SimulationDisplay implements AppInstance
{
    private final SimulationDisplayApplication app;

    public SimulationDisplay(final SimulationDisplayApplication app)
    {
        this.app = app;
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    public void show(SimulationResult simulation)
    {
        // TODO Auto-generated method stub

    }

}
