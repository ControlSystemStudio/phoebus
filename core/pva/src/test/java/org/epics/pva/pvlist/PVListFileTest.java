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

import org.junit.jupiter.api.Test;

/** Unit test for {@link PVListFile} */
class PVListFileTest
{
    @Test
    void testFileLoading() throws Exception
    {
        final PVListFile pvlist = new PVListFile("src/test/resources/demo.pvlist");
        System.out.println(pvlist);
    }

    @Test
    void testDefaultFile() throws Exception
    {
        final PVListFile pvlist = PVListFile.getDefault();
        System.out.println(pvlist);
        assertEquals("DEFAULT", pvlist.getAccess("Any", null));
        assertEquals("DEFAULT", pvlist.getAccess("PV", null));
        assertEquals("DEFAULT", pvlist.getAccess("Any", InetAddress.getByName("11.12.13.14")));
        assertEquals("DEFAULT", pvlist.getAccess("PV", InetAddress.getByName("11.12.13.14")));
    }

    @Test
    void testDeny() throws Exception
    {
        final PVListFile pvlist = new PVListFile("src/test/resources/demo.pvlist");
        // Specifically denied
        assertNull(pvlist.getAccess("Ignore:This",  null));
        // OK, but caught by general deny rule for that IP address
        assertNull(pvlist.getAccess("Basically:OK", InetAddress.getByName("11.12.13.14")));
    }

    @Test
    void testAllow() throws Exception
    {
        final PVListFile pvlist = new PVListFile("src/test/resources/demo.pvlist");
        // PV allowed by final catch-all in ASG(DEFAULT)
        assertEquals("DEFAULT", pvlist.getAccess("Basically:OK", null));
        // PV listed for ASG(RF)
        assertEquals("RF", pvlist.getAccess("SomeRFSetting", null));
    }
}
