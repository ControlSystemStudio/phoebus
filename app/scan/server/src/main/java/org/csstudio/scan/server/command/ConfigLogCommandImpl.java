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

import org.csstudio.scan.command.ConfigLogCommand;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.internal.JythonSupport;

/** {@link ScanCommandImpl} that configures the log mode
 *  @author Kay Kasemir
 */
public class ConfigLogCommandImpl extends ScanCommandImpl<ConfigLogCommand>
{
    /** {@inheritDoc} */
    public ConfigLogCommandImpl(final ConfigLogCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context)  throws Exception
    {
        context.setLogMode(command.getAutomatic());
        context.workPerformed(1);
    }
}
