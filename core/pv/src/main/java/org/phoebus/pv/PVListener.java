/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import org.phoebus.vtype.VType;

/** Listener to a {@link PV}
 *  @author Kay Kasemir
 */
public interface PVListener
{
    public default void permissionsChanged(boolean readonly) {};

    public void valueChanged(VType value);

    public default void disconnected() {};
}
