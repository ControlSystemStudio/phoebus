/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.disconnected;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

/**
 * Creates dummy PVs for use inside tests.
 *
 * The PVs created by this factory will never connect.
 */
public class DisconnectedPVFactory implements PVFactory {

    @Override
    @SuppressWarnings("nls")
    public String getType() {
        return "disconnected";
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception {
        return new DisconnectedPV(base_name);
    }

}
