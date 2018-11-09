/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv;

import org.epics.vtype.VType;
import org.phoebus.pv.PV;

/** Listener to a {@link PV}
 *  @author Kay Kasemir
 */
public interface RuntimePVListener
{
    /** Notification from PV that indicates change in access permissions
     *  @param pv PV that changed permissions
     *  @param readonly Is PV now read-only, i.e. no write access?
     */
    default public void permissionsChanged(RuntimePV pv, boolean readonly)
    {
        // Ignore
    }

    /** Notification from PV that indicates change in value
     *  @param pv PV that sent a new value
     *  @param value Current value of the PV
     */
    public void valueChanged(RuntimePV pv, VType value);

    /** Notification from PV that indicates a disconnect
     *  @param pv PV that is no longer accessible
     */
    default public void disconnected(RuntimePV pv)
    {
        // Ignore
    }
}
