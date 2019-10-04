/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.util.BitSet;

import org.epics.pva.data.PVAStructure;

/** Handler for a client's write (PUT) to a PV
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface WriteEventHandler
{
    /** Notification that client wrote to a PV
     *
     *  <p>Called with a copy of the PV value
     *  where fields written by the client have already been updated.
     *  Implementation can then transfer these changes into the
     *  PVs data as received, or for example limit values
     *  to a certain range, or ignore the received data.
     *
     *  <p>Implementation may silently ignore received data,
     *  or throw an exception to notify client that the write
     *  access was refused.
     *
     *  @param pv PV that the client wrote
     *  @param changes Fields of the PV data that were changed
     *  @param written Data that the client wrote
     *  @throws Exception on error
     */
    public void handleWrite(ServerPV pv, BitSet changes, PVAStructure written) throws Exception;
}
