/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import org.epics.pva.data.PVAStructure;

/** RPC Service
 *  @author Kay Kasemir
 */
public interface RPCService
{
    /** Implementation of the RPC service
     *
     *  @param parameters Service call parameters, i.e. arguments of the call
     *  @return Result
     *  @throws Exception on error
     */
    public PVAStructure call(PVAStructure parameters) throws Exception;
}
