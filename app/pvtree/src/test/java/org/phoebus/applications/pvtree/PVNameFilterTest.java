/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.phoebus.applications.pvtree.model.PVNameFilter;

/** {@link PVNameFilter} test
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVNameFilterTest
{
    @Test
    public void testPVNames()
    {
        // 'Normal' PV
        assertTrue(PVNameFilter.isPvName("SomePV"));
        assertTrue(PVNameFilter.isPvName("SomePV:WithNumber2"));

        // Starts with number, as used by AreaDetector
        assertTrue(PVNameFilter.isPvName("13SIM:cam1:whatever42"));

        // 'Hardware' links
        assertFalse(PVNameFilter.isPvName("@plc2"));
        assertFalse(PVNameFilter.isPvName("#2 S3"));
    }

    @Test
    public void testConstants()
    {
        // Numbers
        assertFalse(PVNameFilter.isPvName("2"));
        assertFalse(PVNameFilter.isPvName("3.14"));
        assertFalse(PVNameFilter.isPvName("1.9E-31"));
    }
}
