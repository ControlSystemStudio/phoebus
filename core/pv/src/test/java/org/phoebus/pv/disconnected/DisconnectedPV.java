/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.disconnected;

import org.phoebus.pv.PV;

/**
 * Dummy PV implementation that is never going to connect.
 */
public class DisconnectedPV extends PV {

    protected DisconnectedPV(String name) {
        super(name);
    }

}
