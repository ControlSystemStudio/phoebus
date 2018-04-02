/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server.command;

import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.condition.DeviceCondition;
import org.csstudio.scan.server.condition.WaitForDevicesCondition;
import org.csstudio.scan.server.internal.JythonSupport;

/** Implementation of the {@link WaitForDevicesCommand}
 *  @author Kay Kasemir
 */
public class WaitForDevicesCommandImpl extends ScanCommandImpl<WaitForDevicesCommand>
{
    private volatile DeviceCondition condition = null;

    /** {@inheritDoc} */
    public WaitForDevicesCommandImpl(final WaitForDevicesCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
        condition = new WaitForDevicesCondition(command.getDevices());
        condition.await();
        condition = null;
        context.workPerformed(1);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final DeviceCondition active = condition;
        if (active != null)
            return active.toString();
        return super.toString();
    }
}
