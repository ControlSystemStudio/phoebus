/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.runtime.Preferences;
import org.csstudio.display.builder.runtime.TextPatch;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

/** Factory for PVs used by the widget runtime
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVFactory
{
    /** @param name PV Name that might contain legacy information
     *  @return Patched PV name
     */
    private static String patch(final String name)
    {
        String patched = name;
        for (TextPatch patch : Preferences.pv_name_patches)
            patched = patch.patch(patched);
        if (! patched.equals(name))
            logger.log(Level.WARNING, "Patched PV name '" + name + "' into '" + patched + "'");
        return patched;
    }

    /** Get a PV
     *  @param name Name of PV
     *  @return {@link RuntimePV}
     *  @throws Exception on error
     */
    public static RuntimePV getPV(final String name) throws Exception
    {
        final PV pv = PVPool.getPV(patch(name));
        return new RuntimePV(pv);
    }

    /** Release a PV (close, dispose resources, ...)
     *  @param pv {@link RuntimePV} to release
     */
    public static void releasePV(final RuntimePV pv)
    {
        pv.close();
    }
}
