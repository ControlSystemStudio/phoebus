/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.pvlist;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Unit test for {@link PVListFile} */
class PVListFileTest
{
    private static PVListFile pvlist;

    @BeforeAll
    static void setUpBeforeClass() throws Exception
    {
        pvlist = new PVListFile("src/test/resources/demo.pvlist");
    }

    @Test
    void testFileLoading()
    {
        System.out.println(pvlist);
    }

    @Test
    void testDeny() throws Exception
    {
        // Specifically denied
        assertNull(pvlist.getAccess("Ignore:This",  null));
        // OK, but caught by general deny rule for that IP address
        assertNull(pvlist.getAccess("Basically:OK", InetAddress.getByName("11.12.13.14")));
    }

    @Test
    void testAllow() throws Exception
    {
        // PV allowed by final catch-all in ASG(DEFAULT)
        assertEquals("DEFAULT", pvlist.getAccess("Basically:OK", null));
        // PV listed for ASG(RF)
        assertEquals("RF", pvlist.getAccess("SomeRFSetting", null));
    }
}
