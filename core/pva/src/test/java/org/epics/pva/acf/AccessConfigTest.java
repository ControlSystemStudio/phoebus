/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.acf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileReader;
import java.net.InetAddress;

import org.junit.jupiter.api.Test;

/** JUNit test of the {@link AccessConfigParser}
 *
 *  @author Kay Kasemir
 */
public class AccessConfigTest
{
    private static final String FILE = "src/test/resources/demo2.acf";

    @Test
    public void testTokenizer() throws Exception
    {
        try (AccessConfigTokenizer tokenizer = new AccessConfigTokenizer(FILE, new FileReader(FILE)))
        {
            while (! tokenizer.done())
                System.out.println(tokenizer.nextToken());
        }
    }

    @Test
    public void testParser() throws Exception
    {
        final AccessConfig acf = new AccessConfigParser().parse(FILE, new FileReader(FILE));
        System.out.println(acf);

        assertTrue(acf.getUserGroupNames().contains("expert"));

        assertTrue(acf.getUserGroup("expert").users().contains("Egon"));
        assertFalse(acf.getUserGroup("expert").users().contains("Fred"));

        assertTrue(acf.getHostGroupNames().contains("local"));

        System.out.println("Resolved addresses in HAG(local):");
        HostAccessGroup hag = acf.getHostGroup("local");
        System.out.println(hag.hosts());

        assertTrue(hag.hosts().contains(InetAddress.getByName("localhost")));
        assertTrue(hag.hosts().contains(InetAddress.getByName("127.0.0.1")));
        assertTrue(hag.hosts().contains(InetAddress.getByName("10.1.58.243")));

        System.out.println("Resolved addresses in HAG(well_known):");
        hag = acf.getHostGroup("well_known");
        System.out.println(hag.hosts());

        assertTrue(hag.hosts().contains(InetAddress.getByName("www.google.com")));

        assertTrue(acf.getAccessGroupNames().contains("DEFAULT"));
    }

    @Test
    public void testBuiltinDefault() throws Exception
    {
        final AccessConfig acf = AccessConfig.getDefault();
        System.out.println(acf);
        final AccessSecurityGroup asg = acf.getAccessGroup("DEFAULT");
        assertNotNull(asg);
        assertTrue(asg.mayWrite("Egon",     InetAddress.getByName("localhost")));
        assertTrue(asg.mayWrite("Fred",     InetAddress.getByName("127.0.0.1")));
        assertTrue(asg.mayWrite("Anybody",  InetAddress.getByName("10.23.93.200")));
    }

    @Test
    public void testDefault() throws Exception
    {
        final AccessConfig acf = new AccessConfigParser().parse(FILE, new FileReader(FILE));

        final AccessSecurityGroup asg = acf.getAccessGroup("DEFAULT");

        // Egon, an 'expert', may write from localhost or 127.0.0.1
        assertTrue(asg.mayWrite("Egon",     InetAddress.getByName("localhost")));
        assertTrue(asg.mayWrite("Egon",     InetAddress.getByName("127.0.0.1")));

        // Fred is not on the list of 'expert' users
        assertFalse(asg.mayWrite("Fred",    InetAddress.getByName("localhost")));

        // Egon can only write from localhost, not other IPs
        assertFalse(asg.mayWrite("Egon",    InetAddress.getByName("127.0.0.99")));

        // There's generally no write access to anybody from anywhere
        assertFalse(asg.mayWrite("Anybody", InetAddress.getByName("1.2.3.4")));
    }

    @Test
    public void testOpen() throws Exception
    {
        final AccessConfig acf = new AccessConfigParser().parse(FILE, new FileReader(FILE));

        final AccessSecurityGroup asg = acf.getAccessGroup("OPEN");
        assertTrue(asg.mayWrite("Egon",    InetAddress.getByName("localhost")));
        assertTrue(asg.mayWrite("Fred",    InetAddress.getByName("localhost")));
        assertTrue(asg.mayWrite("Anybody", InetAddress.getByName("1.2.3.4")));
    }

    @Test
    public void testVacuum() throws Exception
    {
        final AccessConfig acf = new AccessConfigParser().parse(FILE, new FileReader(FILE));

        final AccessSecurityGroup asg = acf.getAccessGroup("VACUUM");
        System.out.println(asg);
        assertTrue(asg.mayWrite("Mary Ann", InetAddress.getByName("1.2.3.4")));
        assertFalse(asg.mayWrite("Anybody", InetAddress.getByName("1.2.3.4")));
    }
}
