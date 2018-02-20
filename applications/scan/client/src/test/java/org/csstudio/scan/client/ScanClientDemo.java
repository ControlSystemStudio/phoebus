/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.junit.Before;
import org.junit.Test;

/** {@link ScanClient} demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanClientDemo
{
    private ScanClient client;

    @Before
    public void setup()
    {
        client = new ScanClient("localhost", ScanClient.DEFAULT_PORT);
    }

    @Test
    public void testServerInfo() throws Exception
    {
        final ScanServerInfo server_info = client.getServerInfo();
        System.out.println(server_info);
        assertThat(server_info.getVersion(), containsString("ScanServer"));
    }

    @Test
    public void testScanInfos() throws Exception
    {
        final List<ScanInfo> scans = client.getScanInfos();
        for (ScanInfo scan : scans)
            System.out.println(scan);
    }
}
