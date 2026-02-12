/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.pvlist;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.epics.pva.pvlist.PVListFileRule.AllowingRule;
import org.epics.pva.pvlist.PVListFileRule.DenyingRule;

/** Handle a `*.pvlist` file with DENY and ALLOW rules */
public class PVListFile
{
    private List<DenyingRule> deny = new ArrayList<>();
    private List<AllowingRule> allow = new ArrayList<>();

    /** @return Built-in default pvlist that allows all access */
    public static PVListFile getDefault() throws Exception
    {
        final Reader reader = new InputStreamReader(PVListFile.class.getResourceAsStream("default.pvlist"));
        return new PVListFile("default.pvlist", reader);
    }

    /** @param filename `*.pvlist` file to parse
     *  @throws Exception on error
     */
    public PVListFile(final String filename) throws Exception
    {
        this(filename, new FileReader(filename));
    }

    /** @param filename `*.pvlist` file to parse
     *  @param file_reader {@link Reader} for that file
     *  @throws Exception on error
     */
    public PVListFile(final String filename, final Reader file_reader) throws Exception
    {
        try (BufferedReader reader = new BufferedReader(file_reader))
        {
            String line;
            int lineno = 0;
            while ((line = reader.readLine()) != null)
            {
                ++lineno;
                line = line.strip();
                if (line.isBlank()  ||  line.startsWith("#"))
                    continue;

                PVListFileRule rule;
                try
                {
                    rule = PVListFileRule.create(line);
                }
                catch (Exception ex)
                {
                    throw new Exception(filename + " line " + lineno, ex);
                }
                if (rule instanceof AllowingRule r)
                    allow.add(r);
                else if (rule instanceof DenyingRule r)
                    deny.add(r);
            }
        }
    }

    /** Does a client have access?
     *  @param pv_name PV name to check
     *  @param host Address of the client
     *  @return Access security group of the client or <code>null</code> if no access
     */
    public String getAccess(final String pv_name, final InetAddress host)
    {
        // Does any rule specifically deny access?
        for (var rule : deny)
            if (rule.isDenied(pv_name, host))
                return null;
        // Does any rule specifically allow access?
        for (var rule : allow)
        {
            final String asg = rule.getAccessSecurityGroup(pv_name);
            if (asg != null)
                return asg;
        }
        // No match: Deny
        return null;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        for (var rule : deny)
            buf.append(rule).append('\n');
        for (var rule : allow)
            buf.append(rule).append('\n');
        return buf.toString();
    }
}
