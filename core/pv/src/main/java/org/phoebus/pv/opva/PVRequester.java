/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.opva;

import static org.phoebus.pv.PV.logger;

import java.util.logging.Level;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Requester;

/** Base for PVAccess {@link Requester}
 *  @author Kay Kasemir
 */
class PVRequester implements Requester
{
    @Override
    public String getRequesterName()
    {
        return getClass().getName();
    }

    @Override
    public void message(final String message, final MessageType type)
    {
        switch (type)
        {
        case fatalError:
            logger.log(Level.SEVERE, message);
            break;
        case error:
        case warning:
            logger.log(Level.WARNING, message);
            break;
        default:
            logger.log(Level.INFO, message);
        }
    }
}
