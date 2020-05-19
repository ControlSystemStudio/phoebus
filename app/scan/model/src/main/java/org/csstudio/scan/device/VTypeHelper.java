/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.device;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.logging.Level;


/** Compatibility wrapper for legacy scripts
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeHelper extends org.phoebus.core.vtypes.VTypeHelper
{
    static
    {
        logger.log(Level.WARNING,
                   "Legacy code accessed org.csstudio.scan.device.VTypeHelper, update to org.phoebus.core.vtypes.VTypeHelper",
                   new Exception("Call stack"));
    }
}
