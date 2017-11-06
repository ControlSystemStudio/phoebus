/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv;

import org.phoebus.pv.PV;

/** Listener to a {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public interface RuntimePVFactory
{
    /** ID of extension point for contributing PV factories */
    public final static String EXTENSION_POINT = "org.csstudio.display.builder.runtime.pvs";

    /** Get a PV
     *  @param name Name of PV
     *  @return {@link RuntimePV}
     *  @throws Exception on error
     */
    public RuntimePV getPV(String name) throws Exception;

    /** Release a PV (close, dispose resources, ...)
     *  @param pv {@link RuntimePV} to release
     */
    public void releasePV(RuntimePV pv);
}
