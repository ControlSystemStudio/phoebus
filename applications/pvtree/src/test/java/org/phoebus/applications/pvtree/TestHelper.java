/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree;

import java.util.Collection;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.RefCountMap.ReferencedEntry;

/** Helper for tests
 *
 *  <p>Hardcoded values for interactive demos.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TestHelper
{
    public static void setupLogging()
    {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tH:%1$tM:%1$tS %2$s %4$s: %5$s%6$s%n");

        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.FINE);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(Level.FINE);

        logger = Logger.getLogger("org.csstudio.vtype.pv");
        logger.setLevel(Level.WARNING);
        logger = Logger.getLogger("com.cosylab.epics.caj");
        logger.setLevel(Level.WARNING);
    }

    public static void checkShutdown()
    {
        final Collection<ReferencedEntry<PV>> pvs = PVPool.getPVReferences();
        if (pvs.isEmpty())
            System.out.println("Done.");
        else
            for (ReferencedEntry<PV> pv : pvs)
                System.out.println("Failed to dispose " + pv);
        System.exit(0);
    }
}
