/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.simulation;

import org.phoebus.framework.spi.AppDescriptor;

/** Application for displaying scan simulation
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimulationDisplayApplication implements AppDescriptor
{
    public static final String NAME = "scan_simulation";
    public static final String DISPLAY_NAME = "Scan Simulation";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public SimulationDisplay create()
    {
        return new SimulationDisplay(this);
    }
}