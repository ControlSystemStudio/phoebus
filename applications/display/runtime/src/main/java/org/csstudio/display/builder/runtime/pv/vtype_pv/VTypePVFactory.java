/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv.vtype_pv;

import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVFactory;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

/** {@link RuntimePVFactory} for {@link PV}
 *
 *  @author Kay Kasemir
 */
public class VTypePVFactory implements RuntimePVFactory
{
    @Override
    public RuntimePV getPV(final String name) throws Exception
    {
        return new VTypePV(PVPool.getPV(name));
    }

    @Override
    public void releasePV(final RuntimePV pv)
    {
        final VTypePV vpv = (VTypePV)pv;
        vpv.close();
        PVPool.releasePV(vpv.getPV());
    }
}
