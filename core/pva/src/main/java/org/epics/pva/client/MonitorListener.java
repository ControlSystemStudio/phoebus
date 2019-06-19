/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.util.BitSet;

import org.epics.pva.data.PVAStructure;

/** Listener to {@link MonitorRequest}
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface MonitorListener
{
    /** Invoked whenever a monitor is received
     *
     *  <p>Data in the structure is only guaranteed to be valid
     *  while inside this method.
     *  For example, the array data of a `PVA*Array`
     *  may be reused after this method has been called.
     *
     *  @param channel Channel that received an update
     *  @param changes Elements of the structure that changed
     *  @param overruns Elements of the structure with skipped updates
     *  @param data Complete data, merges existing value and changes
     */
    public void handleMonitor(final PVAChannel channel,
                              final BitSet changes,
                              final BitSet overruns,
                              final PVAStructure data);
}
